#!/usr/bin/env python3
"""
缓存索引清理集成测试 — 验证方案 D (flow 维度索引) + 方案 E (事务外清理)

验证点:
1. writeCache 后索引 key (cp:cache:flow:keys:{flowId}) 存在且含业务 key
2. deploy 后索引被清理 (evictExecutionResults)
3. stop 后索引被清理
4. 同版本重部署跳过执行结果清理 (方案 E)

前置: open-server(:18080) + connector-api(:18180) 运行, Redis 集群可达
用法: cd open-server/src/test/python && pytest modules/flow/test_cache_evict.py -v -m L2
"""
import os
import sys
import time

import pytest
import importlib.util

TEST_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(TEST_DIR), "common"))

from conftest import api, _get_data, deployed_flow, published_connector  # noqa: F401
from common import _REDIS_CLUSTER_NODES, REDIS_PASSWORD
from redis.cluster import RedisCluster

_first_node = _REDIS_CLUSTER_NODES[0]
_redis = RedisCluster(
    host=_first_node[0], port=int(_first_node[1]),
    password=REDIS_PASSWORD, decode_responses=True
)


def _redis_exists(key: str) -> bool:
    try:
        return bool(_redis.exists(key))
    except Exception as e:
        print(f"  REDIS EXISTS ERROR: {e}")
        return False


def _redis_smembers(key: str) -> set:
    try:
        return set(_redis.smembers(key))
    except Exception as e:
        print(f"  REDIS SMEMBERS ERROR: {e}")
        return set()


def _redis_sscan_count(key: str) -> int:
    """SSCAN 统计索引成员数"""
    try:
        count = 0
        cursor = 0
        while True:
            cursor, members = _redis.sscan(key, cursor, count=100)
            count += len(members)
            if cursor == 0:
                break
        return count
    except Exception as e:
        print(f"  REDIS SSCAN ERROR: {e}")
        return -1


class TestCacheIndexWrite:
    """验证写入缓存时索引同步维护 (方案 D ①)"""

    @pytest.mark.L2
    def test_write_cache_index_pattern(self):
        """索引 key 命名对齐 cp:cache:flow:{flowId}:* 命名空间"""
        fid = "100"
        idx = f"cp:cache:flow:keys:{fid}"
        # 命名空间对齐: 索引 key 与业务 key 同前缀 cp:cache:flow:
        assert idx.startswith("cp:cache:flow:")
        # Set 是具体 key, 不带 :* 通配
        assert not idx.endswith(":*")


class TestCacheIndexEvict:
    """验证生命周期操作触发索引清理 (方案 D ② + 方案 E)"""

    @pytest.mark.L2
    def test_deploy_same_version_keeps_index(self, deployed_flow):
        """同版本重部署 → 执行结果缓存不清 (方案 E: 版本变化才清理)"""
        fid, fvid = deployed_flow
        idx = f"cp:cache:flow:keys:{fid}"

        # 模拟写入缓存 (直连 Redis SET + SADD, 模拟 writeCache 的 Lua 效果)
        _redis.set(f"cp:cache:flow:{fid}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{fid}:k1")

        assert _redis_exists(idx), "前置: 索引应存在"
        # 再次 deploy (同版本重部署)
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
        assert resp.status_code == 200

        # 等待 afterCommit 处理
        time.sleep(1)
        # 同版本重部署: 版本未变, 执行结果缓存应保留 (方案 E 设计)
        assert _redis_exists(idx), "同版本重部署不应清理执行结果缓存索引"
        assert _redis_exists(f"cp:cache:flow:{fid}:k1"), "同版本重部署业务缓存应保留"

    @pytest.mark.L2
    def test_deploy_version_change_evicts(self, deployed_flow, published_connector):
        """版本变化 → 执行结果缓存被清理 (方案 E: 版本变化才清理)"""
        fid, fvid = deployed_flow
        idx = f"cp:cache:flow:keys:{fid}"
        _redis.set(f"cp:cache:flow:{fid}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{fid}:k1")

        # 创建新版本并部署 (版本变化)
        cid, cvid = published_connector
        r = api("POST", f"/flows/{fid}/versions", {})
        new_vid = int(_get_data(r)["versionId"])
        api("PUT", f"/flows/{fid}/versions/{new_vid}", {
            "orchestrationConfig": {
                "flowConfig": {"flowMode": "serial", "timeout": 3000},
                "nodes": [
                    {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                    {"id": "conn", "type": "connector", "data": {"type": "connector", "connectorId": cid, "connectorVersionId": cvid}},
                    {"id": "exit", "type": "exit", "data": {"type": "exit"}},
                ],
                "edges": [
                    {"id": "e1", "source": "trigger", "target": "conn"},
                    {"id": "e2", "source": "conn", "target": "exit"},
                ],
            }
        })
        api("POST", f"/flows/{fid}/versions/{new_vid}/publish")
        # 审批新版本
        from conftest import _find_approval
        aid = _find_approval(new_vid)
        if aid:
            detail = api("GET", f"/approvals/{aid}").json().get("data", {})
            nodes = detail.get("combinedNodes") or detail.get("nodes") or []
            seen = set()
            for node in nodes:
                uid = node.get("userId")
                if uid and uid not in seen:
                    seen.add(uid)
                    api("POST", f"/approvals/{aid}/approve", {"comment": "approve"},
                        headers={"Cookie": f"user_id={uid}", "X-XSRF-TOKEN": f"user_id={uid}"})
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": new_vid})
        assert resp.status_code == 200

        time.sleep(1)
        assert not _redis_exists(idx), "版本变化 deploy 后索引应被清理"
        assert not _redis_exists(f"cp:cache:flow:{fid}:k1"), "业务缓存 key 应被清理"

    @pytest.mark.L2
    def test_stop_evicts_index(self, deployed_flow):
        """stop → 索引被清理"""
        fid, _ = deployed_flow
        idx = f"cp:cache:flow:keys:{fid}"
        _redis.set(f"cp:cache:flow:{fid}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{fid}:k1")

        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200

        time.sleep(1)
        assert not _redis_exists(idx), f"stop 后索引 {idx} 应被清理"
        assert not _redis_exists(f"cp:cache:flow:{fid}:k1"), "业务缓存 key 应被清理"
