# 性能测试（perf）

独立于功能测试（`modules/` 单接口 + `e2e/` 全流程）的性能验证目录。

> **定位**：功能测试验证"对不对"，性能测试验证"快不快"。两者分离——`modules/`/`e2e/` 走 pytest 分层（L0~L4），`perf/` 走独立脚本 + 基准报告。

## 目录结构

```
perf/
├── README.md              # 本文件 — 性能测试标准
├── prepare_50w_keys.py    # 存量数据预置（模拟标准环境规模）
└── bench_deploy_stop.py   # 接口耗时基准（deploy/start/stop）
```

## 脚本说明

| 脚本 | 用途 | 输出 |
|------|------|------|
| `prepare_50w_keys.py` | 向 Redis 集群预置 N 个存量 key，模拟标准环境数据规模 | 写入数 / 耗时 / 节点分布 |
| `bench_deploy_stop.py` | 对指定 flow 循环执行 deploy→start→stop，统计接口耗时 | avg / p50 / p95 / p99 / max → JSON 报告 |

## 运行标准

### 1. 前置条件

| 项目 | 要求 |
|------|------|
| 服务 | open-server(:18080) + connector-api(:18180) 运行中 |
| Redis | 6 节点集群，性能验证前需预置存量数据（模拟标准环境） |
| 测试数据 | flow 需已创建且含已发布版本（`--flow-ids` 指定） |

### 2. 执行步骤

```bash
cd open-server/src/test/python

# ① 预置存量数据（默认 50w，模拟标准环境）
python3 perf/prepare_50w_keys.py 500000

# ② 跑基准（修复前/修复后各跑一轮对比）
python3 perf/bench_deploy_stop.py --flow-ids 100 101 102 --rounds 3

# ③ 查看报告
cat reports/bench_result.json
```

### 3. 判定标准

| 指标 | 目标 | 说明 |
|------|------|------|
| deploy p95 | < 100ms | 部署接口耗时（含缓存清理） |
| stop p95 | < 100ms | 停止接口耗时（含缓存清理） |
| Redis 慢日志 | 无 SCAN 慢命令 | `SLOWLOG GET 50` 无 evict 相关 >100ms 记录 |

> ⚠️ **对比原则**：性能验证必须是"修复前 vs 修复后"**同环境对比**，而非只看绝对数值。
> 开发环境存在跨机 RTT（远程 MySQL/Redis 192.168.3.x），基础接口（如 GET flow 详情）可能本身 ~150ms，
> 此时绝对 <100ms 目标不可达。正确做法：先测基础接口基线，再对比修复前后清理相关接口的增量差异。
>
> **判定公式**：`清理相关接口耗时 - 基础接口耗时` 应在修复后趋近于 0（清理不再是瓶颈）。

### 4. 报告要求

- 报告写入 `reports/bench_result.json`（`summary` 含 avg/p50/p95/p99/max）
- 必须注明：测试环境（开发/预发/生产）、数据规模、服务版本（修复前/后）
- 修复前后报告需在同一环境、同一数据规模下采集，保证可比性

---

## 与功能测试的分工

| 目录 | 类型 | 验证目标 | 运行方式 |
|------|------|---------|---------|
| `modules/` | 单接口功能测试 | 每个接口行为正确 | `pytest modules/flow/` |
| `e2e/` | 全流程端到端 | 部署→启动→调用链路 | `pytest e2e/` |
| `test_cache_evict.py` | 跨接口行为验证 | 缓存索引写入/清理（方案 D/E） | `pytest test_cache_evict.py -m L2` |
| `verify_no_residue.py` | 集群一致性验证 | 6 节点漏删检查 | `python3 verify_no_residue.py <flowId>` |
| `perf/` | 性能测试 | 接口耗时、清理开销 | `python3 perf/*.py` |

## 关联文档

- bugfix 文档：`.sddu/specs-tree-root/specs-tree-connector-platform-v3/bugfix-flow-cache-evict-slow.md` §6（验证方案）
