# Open Platform — 工程地图

| 工程 | 类型 | 端口 | 路径 | 启动方式 |
|------|:--:|------|------|---------|
| **wecodesite** | 前端 | 5173 | `/` | `bash wecodesite/scripts/restart.sh` |
| **market-web** | 前端 | 13000 | `/market-web/` | `bash market-web/scripts/restart.sh` |
| **open-server** | 后端 | 18080 | `/open-server` | `bash open-server/scripts/restart.sh` |
| **connector-api** | 后端 | 18180 | `/` | `bash connector-api/scripts/restart.sh` |
| **api-server** | 后端 | 18081 | `/api-server` | `bash api-server/scripts/restart.sh` |
| **event-server** | 后端 | 18082 | `/event-server` | `bash event-server/scripts/restart.sh` |
| **market-server** | 后端 | 18083 | `/market-server` | `bash market-server/scripts/restart.sh` |

## 依赖关系

```
wecodesite ──→ open-server (proxy /open-website/* → /service/*)

market-web ──→ market-server (proxy /market-web/service → /market-server/service)

open-server ──→ connector-api (debug proxy, flow invoke)
            ──→ DB (192.168.3.155:3306)
            ──→ Redis Cluster (192.168.3.201~206:6379)

connector-api ──→ DB (192.168.3.155:3306)
              ──→ Redis Cluster (192.168.3.201~206:6379)

market-server ──→ DB (192.168.3.155:3306)
              ──→ Redis (192.168.3.201:6379)
```

## 工程说明

| 工程 | 说明 |
|------|------|
| **wecodesite** | 开放平台主站前端 (Vue3 + Vite) |
| **market-web** | 应用市场前端 (Vue3 + Vite) |
| **open-server** | 开放平台主后端服务 (Spring Boot), 含连接器/连接流 CRUD, 审批, 调试代理 |
| **connector-api** | 连接流运行时引擎, 含 DAG 调度器, GraalJS 脚本沙箱, 缓存 |
| **api-server** | API 管理服务 |
| **event-server** | 事件管理服务 |
| **market-server** | 应用市场后端服务, 含 Lookup 数据字典管理 |

## 一键启动

```bash
bash wecodesite/scripts/restart.sh &
bash market-web/scripts/restart.sh &
bash open-server/scripts/restart.sh &
bash connector-api/scripts/restart.sh &
bash market-server/scripts/restart.sh &
wait
```

## Java 单元测试

快速运行（每个工程一条命令，仓库根目录执行）：

```bash
mvn -f open-server/pom.xml test
mvn -f connector-api/pom.xml test
mvn -f event-server/pom.xml test
mvn -f api-server/pom.xml test
mvn -f market-server/pom.xml test
```

| 工程 | 测试文件数 | 主要覆盖 |
|------|:--:|------|
| **open-server** | 38 | 连接器/连接流版本、审批(+引擎)、API、权限、调试代理、能力订阅、回调、同步、分类、安全 |
| **connector-api** | 23 | 运行时引擎（DAG 调度、节点执行、表达式）、GraalJS 沙箱、缓存、限流、日志脱敏 |
| **event-server** | 14 | 事件/回调网关、SSE/WebSocket/WebHook/MQ 通道、认证 |
| **api-server** | 13 | Scope 授权、数据查询、消费网关、用户角色、签名 |
| **market-server** | 11 | 能力管理 Admin CRUD、文件服务 |

## Python 集成测试

五级金字塔：`L0 冒烟 → L1 CRUD → L2 生命周期 → L3 端到端 → L4 边界反向`

| 工程 | 位置 | 测试文件数 | 组织方式 |
|------|------|:--:|------|
| **open-server** | `src/test/python/` | ~105 | 一接口一文件（modules/ 与源码 1:1）+ e2e/ 全流程（部署→发布→审批→调用）+ 跨模块 |
| **connector-api** | `src/test/python/inspect/` | 17 | 一场景一文件（触发/认证/脚本/超时/缓存/分支/执行记录） |
| **market-server** | `src/test/python/` | 6 | 能力 CRUD、文件上传、审批冒烟 |
| **api-server** | `src/test/python/` | 1 | 内部用户角色 |
| **event-server** | — | 0 | 无 pytest，仅 shell 脚本 |

快速运行（每个工程一条命令，仓库根目录执行）：

```bash
# open-server（全量，默认仅 L0 需显式 -m ""）
pytest open-server/src/test/python -m ""

# connector-api（默认全量）
pytest connector-api/src/test/python

# market-server（默认全量）
pytest market-server/src/test/python

# api-server（默认全量）
pytest api-server/src/test/python
```

> event-server 无 pytest 套件，集成测试走废弃的 Shell 脚本。

**前端 E2E**（Playwright + pytest，需先启动前端 dev server 与对应后端服务）：

| 工程 | 单元测试 | E2E 测试 |
|------|---------|---------|
| **wecodesite** | jest 已配置、暂无用例 | Playwright (pytest)：单节点/串行/并行 3 场景 |
| **market-web** | 无 | Playwright (pytest)：能力管理 5 用例 |

快速运行（仓库根目录执行）：

```bash
pytest wecodesite/tests
pytest market-web/tests
```

## Shell 脚本测试 (废弃)

| 工程 | 脚本 | 覆盖 |
|------|------|------|
| **open-server / market-server** | `test/scripts/test-all-apis-optimized.sh` | 51 接口 |
| **api-server** | `test/scripts/test-all-apis.sh` | 5 接口（Scope 授权 + 消费网关） |
| **event-server** | `test/scripts/test-all-apis.sh` | 2 接口 11 用例（事件发布 + 回调触发） |
