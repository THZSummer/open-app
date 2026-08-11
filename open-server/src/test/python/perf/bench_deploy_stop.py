#!/usr/bin/env python3
"""
部署/停止接口耗时基准 — 验证方案 D 后性能达标 (p95 < 100ms)

对 N 个 flow 循环执行 deploy → start → stop, 统计每个接口耗时。

用法:
    python3 perf/bench_deploy_stop.py              # 默认 20 个 flow
    python3 perf/bench_deploy_stop.py --flows 50
    python3 perf/bench_deploy_stop.py --report /tmp/bench.json

前置:
    - open-server(:18080) 运行
    - 测试数据可通过 pytest fixture 或预置 (flow 需已创建并含已发布版本)
    - 建议在含 50w 存量 key 的 Redis 环境执行 (prepare_50w_keys.py 预置)

输出: avg / p95 / p99 / max, 写入 reports/bench_result.json
"""
import argparse
import json
import os
import sys
import time
import statistics

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "common"))

from client import api  # noqa: E402


def _get_data(resp):
    """从 API 响应提取 data 字段; 业务失败(非 200)返回 None"""
    try:
        body = resp.json()
    except Exception:
        return None
    # 业务状态码检查: 仅 code==200 视为成功
    if body.get("code") not in (200, "200"):
        return None
    return body.get("data")


def _pct(data, p):
    if not data:
        return 0.0
    data = sorted(data)
    idx = min(len(data) - 1, int(len(data) * p))
    return data[idx]


def bench_flow(flow_id, version_id, rounds=3):
    """对单个 flow 执行 deploy/start/stop, 返回耗时"""
    results = {"deploy": [], "start": [], "stop": []}
    for _ in range(rounds):
        t0 = time.perf_counter()
        r = api("POST", f"/flows/{flow_id}/deploy", {"versionId": version_id})
        results["deploy"].append((time.perf_counter() - t0) * 1000)
        assert r.status_code == 200, f"deploy 失败: {r.status_code}"

        t0 = time.perf_counter()
        r = api("POST", f"/flows/{flow_id}/start")
        results["start"].append((time.perf_counter() - t0) * 1000)

        t0 = time.perf_counter()
        r = api("POST", f"/flows/{flow_id}/stop")
        results["stop"].append((time.perf_counter() - t0) * 1000)
    return results


def main():
    parser = argparse.ArgumentParser(description="部署/停止接口耗时基准")
    parser.add_argument("--flows", type=int, default=20, help="测试 flow 数量")
    parser.add_argument("--rounds", type=int, default=3, help="每 flow 轮数")
    parser.add_argument("--report", default="reports/bench_result.json",
                        help="报告输出路径")
    parser.add_argument("--flow-ids", nargs="*", type=int,
                        help="指定 flowId 列表 (需含已发布版本), 不指定则提示手动提供")
    args = parser.parse_args()

    if not args.flow_ids:
        print("⚠️  请提供 --flow-ids (已创建且含已发布版本的 flowId 列表)")
        print("   示例: python3 perf/bench_deploy_stop.py --flow-ids 100 101 102")
        sys.exit(1)

    flow_ids = args.flow_ids
    if len(flow_ids) > args.flows:
        flow_ids = flow_ids[:args.flows]

    print(f">>> 基准: {len(flow_ids)} flows × {args.rounds} rounds")
    all_results = {"deploy": [], "start": [], "stop": []}
    for fid in flow_ids:
        # 获取当前已部署版本 (若无则跳过)
        r = api("GET", f"/flows/{fid}")
        if r is None:
            print(f"  flow {fid}: 连接失败, 跳过")
            continue
        if r.status_code != 200:
            print(f"  flow {fid}: HTTP {r.status_code} 获取失败, 跳过")
            continue
        data = _get_data(r)
        if data is None:
            print(f"  flow {fid}: 业务返回非 200 (flow 可能不存在), 跳过")
            continue
        vid = data.get("deployedVersionId")
        if vid is None:
            print(f"  flow {fid}: 无已部署版本, 跳过")
            continue
        res = bench_flow(fid, vid, args.rounds)
        for k, v in res.items():
            all_results[k].extend(v)
        print(f"  flow {fid}: deploy={_pct(res['deploy'], 0.5):.1f}ms "
              f"start={_pct(res['start'], 0.5):.1f}ms stop={_pct(res['stop'], 0.5):.1f}ms")

    summary = {}
    for k, v in all_results.items():
        summary[k] = {
            "avg": round(statistics.mean(v), 2) if v else 0,
            "p50": round(_pct(v, 0.5), 2),
            "p95": round(_pct(v, 0.95), 2),
            "p99": round(_pct(v, 0.99), 2),
            "max": round(max(v), 2) if v else 0,
            "count": len(v),
        }

    if summary["deploy"]["count"] == 0 or summary["stop"]["count"] == 0:
        print("\n❌ 无有效测试数据: 所有 flow 均被跳过 (检查 flow-ids 是否为已部署的真实 flow)")
        sys.exit(2)

    os.makedirs(os.path.dirname(args.report), exist_ok=True)
    with open(args.report, "w", encoding="utf-8") as f:
        json.dump({"summary": summary, "raw": all_results}, f, ensure_ascii=False, indent=2)

    print("\n=== 汇总 ===")
    for k, v in summary.items():
        print(f"  {k:6s}: avg={v['avg']:8.2f}ms  p95={v['p95']:8.2f}ms  "
              f"p99={v['p99']:8.2f}ms  max={v['max']:8.2f}ms")
    print(f"\n报告: {args.report}")

    # 判定
    if summary["deploy"]["p95"] < 100 and summary["stop"]["p95"] < 100:
        print("✅ 通过: deploy/stop p95 < 100ms")
    else:
        print("❌ 未达标: p95 >= 100ms (检查是否在 50w 存量数据环境执行)")


if __name__ == "__main__":
    main()
