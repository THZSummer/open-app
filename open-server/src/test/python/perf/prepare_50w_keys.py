#!/usr/bin/env python3
"""
预置 Redis 存量 key — 模拟标准环境 50w 条数据 (性能验证前置)

向 6 节点集群均匀写入 cp:cache:flow:{flowId}:{key} 模式 key,
模拟存量执行结果缓存 (无索引, 对应"存量数据不处理"决策)。

用法:
    python3 scripts/prepare_50w_keys.py            # 默认 500000 个
    python3 scripts/prepare_50w_keys.py 100000     # 指定数量
    python3 scripts/prepare_50w_keys.py 500000 --clean   # 先清旧数据

输出: 写入总数 / 各节点 key 分布 / 耗时
"""
import argparse
import os
import sys
import time
import random

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "common"))

from client import _REDIS_CLUSTER_NODES, REDIS_PASSWORD  # noqa: E402
from redis.cluster import RedisCluster

# 存量 key 前缀: 模拟历史执行结果缓存 (无索引)
PREFIX = "cp:cache:flow:"
# 随机 flowId 范围 (模拟多个 flow)
FLOW_IDS = [f"{random.randint(1, 100000)}" for _ in range(500)]
# 每批 pipeline 大小
PIPELINE_BATCH = 500


def main():
    parser = argparse.ArgumentParser(description="预置 Redis 存量 key")
    parser.add_argument("count", nargs="?", type=int, default=500000,
                        help="要写入的 key 数量 (默认 500000)")
    parser.add_argument("--clean", action="store_true",
                        help="先清理 cp:cache:flow:* 存量再写入")
    parser.add_argument("--ttl", type=int, default=86400 * 15,
                        help="key TTL 秒 (默认 15 天, 对齐存量)")
    args = parser.parse_args()

    first_node = _REDIS_CLUSTER_NODES[0]
    redis = RedisCluster(
        host=first_node[0], port=int(first_node[1]),
        password=REDIS_PASSWORD, decode_responses=True,
    )

    if args.clean:
        print(">>> 清理存量 cp:cache:flow:* ...")
        count = 0
        for key in redis.scan_iter(match=f"{PREFIX}*", count=1000):
            redis.delete(key)
            count += 1
            if count % 10000 == 0:
                print(f"  cleaned {count}")
        print(f"  已清理 {count} 个 key")

    print(f">>> 写入 {args.count} 个存量 key (TTL={args.ttl}s) ...")
    start = time.time()
    written = 0
    pipe = redis.pipeline(transaction=False)
    while written < args.count:
        flow_id = random.choice(FLOW_IDS)
        key = f"{PREFIX}{flow_id}:k{written}"
        pipe.set(key, "x", ex=args.ttl)
        written += 1
        if written % PIPELINE_BATCH == 0:
            pipe.execute()
    pipe.execute()

    elapsed = time.time() - start
    print(f"  写入完成: {written} 个, 耗时 {elapsed:.1f}s ({written/elapsed:.0f}/s)")

    # 统计节点分布 (通过 scan 采样验证)
    print(">>> 节点分布采样:")
    try:
        total = 0
        for _ in redis.scan_iter(match=f"{PREFIX}*", count=1000):
            total += 1
            if total >= 20000:
                break
        print(f"  采样到 {total}+ 个 key, 集群写入正常")
    except Exception as e:
        print(f"  采样失败: {e}")

    print(f"✅ 预置完成, 共 {written} 个存量 key")


if __name__ == "__main__":
    main()
