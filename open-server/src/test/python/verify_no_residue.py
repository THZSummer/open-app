#!/usr/bin/env python3
"""
集群漏删验证 — 确认 6 节点上 cp:cache:flow:{flowId}:* 全部清理无残留

用法:
    python3 verify_no_residue.py <flowId>           # 验证单个 flow
    python3 verify_no_residue.py <flowId> --index   # 同时验证索引 key 已删
    python3 verify_no_residue.py all                # 验证全部 flow 无残留

前置:
    - 6 节点 Redis 集群可达 (config.py REDIS_CLUSTER_NODES)
    - 已执行 stop/invalidate/delete 触发清理

输出: 每节点匹配数汇总, 非 0 即 FAIL
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "common"))

from client import _REDIS_CLUSTER_NODES, REDIS_PASSWORD  # noqa: E402
from redis.cluster import RedisCluster

PREFIX = "cp:cache:flow:"
INDEX_PREFIX = "cp:cache:flow:keys:"


def count_per_node(nodes, password, pattern):
    """逐节点 SCAN 统计匹配数"""
    results = {}
    for host, port in nodes:
        node = RedisCluster(
            host=host, port=int(port),
            password=password, decode_responses=True,
            skip_full_coverage_check=True,
        )
        count = 0
        for _ in node.scan_iter(match=pattern, count=1000):
            count += 1
        results[f"{host}:{port}"] = count
        node.close()
    return results


def verify_flow(flow_id, check_index=True):
    pattern = f"{PREFIX}{flow_id}:*"
    print(f">>> 验证 flow {flow_id} 残留 (pattern={pattern})")
    per_node = count_per_node(_REDIS_CLUSTER_NODES, REDIS_PASSWORD, pattern)
    total = 0
    for node, count in per_node.items():
        print(f"  {node}: {count} 个")
        total += count

    if check_index:
        idx_key = f"{INDEX_PREFIX}{flow_id}"
        idx_exists = False
        for host, port in _REDIS_CLUSTER_NODES:
            node = RedisCluster(
                host=host, port=int(port),
                password=REDIS_PASSWORD, decode_responses=True,
                skip_full_coverage_check=True,
            )
            if node.exists(idx_key):
                idx_exists = True
            node.close()
        print(f"  索引 key {idx_key} 存在: {idx_exists}")
        if idx_exists:
            total += 1

    if total == 0:
        print("✅ 通过: 全部节点 0 残留")
        return True
    print(f"❌ 失败: 共 {total} 个残留")
    return False


def verify_all():
    print(">>> 验证全部 flow 无 cp:cache:flow:* 残留")
    total = 0
    for host, port in _REDIS_CLUSTER_NODES:
        node = RedisCluster(
            host=host, port=int(port),
            password=REDIS_PASSWORD, decode_responses=True,
            skip_full_coverage_check=True,
        )
        count = 0
        for _ in node.scan_iter(match=f"{PREFIX}*", count=1000):
            count += 1
        print(f"  {host}:{port}: {count} 个")
        total += count
        node.close()
    if total == 0:
        print("✅ 通过: 全部节点 0 残留")
        return True
    print(f"❌ 失败: 共 {total} 个残留 (含存量 key, 属正常若未预置清理)")
    return False


def main():
    parser = argparse.ArgumentParser(description="集群漏删验证")
    parser.add_argument("flow_id", help="flowId 或 'all'")
    parser.add_argument("--no-index", action="store_true",
                        help="跳过索引 key 检查")
    args = parser.parse_args()

    if args.flow_id == "all":
        ok = verify_all()
    else:
        ok = verify_flow(args.flow_id, check_index=not args.no_index)

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
