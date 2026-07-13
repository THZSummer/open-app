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

## 集成测试

```bash
cd open-server/src/test/python
pytest -m "L0 or L1 or L2 or L3 or L4"
```
