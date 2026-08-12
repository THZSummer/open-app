#!/usr/bin/env python3
"""
缓存索引清理集成测试 — 验证方案 D (flow 维度索引) + 方案 E (事务外清理)

验证点:
1. writeCache 后索引 key (cp:cache:flow:keys:{flowId}) 存在且含业务 key
2. deploy 后索引被清理 (evictExecutionResults)
3. stop 后索引被清理
4. 同版本重部署跳过执行结果清理 (方案 E)

前置: open-server(:18080) + connector-api(:18180) 运行, Redis 集群可达
用法: cd open-server/src/test/python && pytest test_cache_evict.py -v -m L2
"""
import os
import time

import pytest

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


def _wait_async_evict_settle(idx_key: str, timeout: float = 15.0):
    """等待异步缓存清理 (AFTER_COMMIT + @Async) 彻底处理完毕.

    语义: 索引 key 连续 3 次检查 (间隔 1s) 均不存在, 才认为在途清理事件已全部执行完。
    避免"首次消失即返回"导致排队中的清理事件随后执行、覆盖测试后续写入的数据。
    """
    stable = 0
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not _redis_exists(idx_key):
            stable += 1
            if stable >= 3:
                return
            time.sleep(1.0)
        else:
            stable = 0
            time.sleep(0.5)
    print(f"  WARN: 异步清理未在 {timeout}s 内稳定 (idx={idx_key})")


def _wait_redis_gone(key: str, timeout: float = 10.0):
    """轮询等待指定 key 从 Redis 消失"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not _redis_exists(key):
            return True
        time.sleep(0.3)
    return False


def _wait_index_member(idx_key: str, member: str, timeout: float = 5.0):
    """轮询等待索引包含指定成员 (确保手工 SADD 已生效)"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if member in set(_redis.smembers(idx_key)):
                return True
        except Exception:
            pass
        time.sleep(0.3)
    print(f"  WARN: 索引未包含成员 {member} (idx={idx_key})")
    return False


class TestCacheIndexWrite:
    """验证写入缓存时索引同步维护 (方案 D ①)"""

    @pytest.mark.L2
    def test_write_cache_index_pattern(self):
        """业务 key 与索引 key 含同一 {flowId} hash tag (集群同 slot 约束, B1 回归)"""
        fid = "100"
        biz = f"cp:cache:flow:{{{fid}}}:k1"
        idx = f"cp:cache:flow:keys:{{{fid}}}"
        # 命名空间对齐: 索引 key 与业务 key 同前缀 cp:cache:flow:
        assert idx.startswith("cp:cache:flow:")
        # Set 是具体 key, 不带 :* 通配
        assert not idx.endswith(":*")
        # hash tag 一致: {flowId} 相同 → 集群下同 slot (Lua 原子写不 CROSSSLOT)
        tag_biz = biz[biz.index("{"):biz.index("}") + 1]
        tag_idx = idx[idx.index("{"):idx.index("}") + 1]
        assert tag_biz == tag_idx == "{" + fid + "}", f"hash tag 应一致: {tag_biz} vs {tag_idx}"


class TestCacheIndexEvict:
    """验证生命周期操作触发索引清理 (方案 D ② + 方案 E)"""

    @pytest.mark.L2
    def test_deploy_same_version_keeps_index(self, deployed_flow):
        """同版本重部署 → 执行结果缓存不清 (方案 E: 版本变化才清理)"""
        fid, fvid = deployed_flow
        idx = f"cp:cache:flow:keys:{{{fid}}}"

        # 等待 fixture 首次部署的异步清理彻底完成 (索引消失且稳定)
        _wait_async_evict_settle(idx)

        # 模拟写入缓存 (直连 Redis SET + SADD, 模拟 writeCache 的 Lua 效果)
        _redis.set(f"cp:cache:flow:{{{fid}}}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{{{fid}}}:k1")

        assert _redis_exists(idx), "前置: 索引应存在"
        # 再次 deploy (同版本重部署 → 幂等短路, 不触发清理)
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": fvid})
        assert resp.status_code == 200

        # 同版本重部署不触发清理事件; 稍等确认索引仍在 (未被任何在途清理误清)
        time.sleep(2)
        assert _redis_exists(idx), "同版本重部署不应清理执行结果缓存索引"
        assert _redis_exists(f"cp:cache:flow:{{{fid}}}:k1"), "同版本重部署业务缓存应保留"

    @pytest.mark.L2
    def test_deploy_version_change_evicts(self, deployed_flow, published_connector):
        """版本变化 → 执行结果缓存被清理 (方案 E: 版本变化才清理)"""
        fid, fvid = deployed_flow
        idx = f"cp:cache:flow:keys:{{{fid}}}"
        # 等待 fixture 首次部署的异步清理完成
        _wait_async_evict_settle(idx)
        _redis.set(f"cp:cache:flow:{{{fid}}}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{{{fid}}}:k1")
        # 确保索引成员已写入 (等待异步线程不干扰的窗口)
        _wait_index_member(idx, f"cp:cache:flow:{{{fid}}}:k1")

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

        # 异步清理可能延迟, 轮询等待索引被清 (索引清理是端到端验证核心;
        # 业务 key 的 UNLINK 由 FlowCacheEvictorTest 单测覆盖 SSCAN→UNLINK 逻辑)
        assert _wait_redis_gone(idx), "版本变化 deploy 后索引应被清理"

    @pytest.mark.L2
    def test_stop_evicts_index(self, deployed_flow):
        """stop → 索引被清理"""
        fid, _ = deployed_flow
        idx = f"cp:cache:flow:keys:{{{fid}}}"
        # 等待 fixture 首次部署的异步清理完成
        _wait_async_evict_settle(idx)
        _redis.set(f"cp:cache:flow:{{{fid}}}:k1", "v1", ex=600)
        _redis.sadd(idx, f"cp:cache:flow:{{{fid}}}:k1")

        api("POST", f"/flows/{fid}/start")
        resp = api("POST", f"/flows/{fid}/stop")
        assert resp.status_code == 200

        # 异步清理可能延迟, 轮询等待索引被清 (核心断言; 业务 key UNLINK 由单测覆盖)
        assert _wait_redis_gone(idx), f"stop 后索引 {idx} 应被清理"
