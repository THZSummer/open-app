# open-app 部署信息 — 部署信息

> **文档定位**: sddu-docs-deploy — 部署信息文档 — 拓扑、资源、CI/CD  
> **输出文件名**: deploy.md  
> **数据来源**: 代码扫描生成 — 各服务 application*.yml + 项目结构  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 部署拓扑

```
                     ┌──────────────────────────┐
                     │       前端层             │
                     │ wecodesite / market-web  │
                     │ qiankunProject / Demo    │
                     └────────────┬─────────────┘
                                  │ HTTP
       ┌──────────────────────────┼──────────────────────────┐
       │                          │                          │
┌──────▼───────┐        ┌─────────▼────────┐       ┌─────────▼────────┐
│ open-server  │        │ market-server    │       │  api-server      │
│ :18080       │        │ :18083           │       │  :18081          │
│ /open-server │        │ /market-server   │       │  /api-server     │
└──────┬───────┘        └─────────┬────────┘       └─────────┬────────┘
       │                          │                          │
┌──────▼──────────────────────────┼──────────────────────────┼────────┐
│          数据层                 │                          │        │
│ MySQL openapp (192.168.3.155:3306) ◄───────── MyBatis ─────┘        │
│ Redis Cluster (192.168.3.201~206:6379) ◄──────── Redis ────────────┘
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                     ┌────────────▼────────────┐
                     │  connector-api :18180   │  R2DBC → MySQL
                     │  /connector-api (WebFlux)│  Reactive Redis
                     └────────────┬────────────┘
                                  │
                     ┌────────────▼────────────┐
                     │  event-server :18082    │
                     │  /event-server          │  Redis (dev 单机 / prod 集群)
                     └─────────────────────────┘
```

> 📊 **Archify 交互图**：[部署拓扑](archify/deployment-topology.html)（可交互）· [PNG](archify/deployment-topology.png)

## 2. 服务端口与上下文

| 服务 | 端口 | context-path | 数据库访问 | Redis |
|------|:----:|--------------|-----------|-------|
| open-server | 18080 | /open-server | MyBatis MySQL | 集群 |
| api-server | 18081 | /api-server | MyBatis MySQL | 集群 |
| market-server | 18083 | /market-server | MyBatis MySQL | 集群 |
| connector-api | 18180 | /connector-api | R2DBC MySQL | Reactive 集群 |
| event-server | 18082 | /event-server | 无 DB | 单机 dev / 集群 prod |

## 3. 基础设施依赖

| 依赖 | 开发环境 | 生产环境 |
|------|---------|---------|
| MySQL | 192.168.3.155:3306/openapp | ${MYSQL_HOST}/${MYSQL_DATABASE} |
| Redis Cluster | 192.168.3.201~206:6379（6 节点） | 192.168.3.201~205:6379 + ${REDIS_PASSWORD} |
| Flyway | open-flyway 工程执行迁移 | 同 |
| 内部网关 | — | ${INTERNAL_GATEWAY_URL:http://internal-gateway:9090} |
| 通讯录 API | wecontact.api-url | 同 |
| 文件存储 | 本地临时目录（dev） | 生产需配置持久化存储 |

## 4. 环境变量清单

| 环境变量 | 默认值 | 服务 | 用途 |
|---------|--------|------|------|
| MYSQL_HOST | localhost | api/market/open-server | 数据库主机 |
| MYSQL_DATABASE | openapp | api/market/open-server | 库名 |
| MYSQL_USERNAME | openapp | api/market/open-server | 账号 |
| MYSQL_PASSWORD | openapp | api/market/open-server | 密码 |
| REDIS_PASSWORD | changeme | api/market/open/event-server | Redis 密码 |
| INTERNAL_GATEWAY_URL | http://internal-gateway:9090 | api-server | 内部网关 |
| INTERNAL_ACCOUNT_PLACEHOLDER | change-me-in-production | api-server | 白名单账号 |

## 5. 健康检查

- 各服务暴露 `/actuator/health`（management.health.db / redis 开关差异）
- open-server / market-server dev 环境关闭 DB 健康检查（避免依赖未就绪）
- connector-api 开启 DB + Redis 健康检查

## 6. 部署注意事项

1. **数据库迁移**：先执行 open-flyway 迁移（V1~V7），再启动服务
2. **Redis 集群**：open-server / api-server / market-server 使用 6 节点集群；event-server dev 为单机
3. **connector-api 特殊性**：WebFlux + R2DBC，base-path=/connector-api，端口 18180 与 Web 服务不同网段
4. **生产环境变量**：所有 DB/Redis 凭证通过环境变量注入，避免明文
5. **静态资源**：market-server 静态资源指向 ${java.io.tmpdir}（dev），生产需改

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
