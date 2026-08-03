# connector-api — 配置项

> **文档定位**: sddu-docs-config — 配置项文档 — 环境变量、开关、参数说明  
> **输出文件名**: connector-api-config.md  
> **数据来源**: 代码扫描生成 — connector-api/src/main/resources/application.yml  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 配置项

### 1.1 基础配置（application.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| server.port | 18180 | 服务端口 |
| spring.webflux.base-path | /connector-api | 响应式上下文根 |
| spring.application.name | connector-api | 应用名 |

### 1.2 数据源（R2DBC）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.r2dbc.url | r2dbc:mysql://192.168.3.155:3306/openapp | 响应式 MySQL |
| spring.r2dbc.username | openapp | 账号 |
| spring.r2dbc.password | openapp | 密码 |
| spring.r2dbc.pool.max-size | 20 | 连接池最大 |
| spring.r2dbc.pool.initial-size | 5 | 初始连接 |
| spring.r2dbc.pool.max-idle-time | 30m | 最大空闲 |
| spring.r2dbc.pool.validation-query | SELECT 1 | 校验查询 |

### 1.3 Redis（Reactive 集群）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.data.redis.cluster.nodes | 192.168.3.201~206:6379 | 6 节点集群 |
| spring.data.redis.cluster.max-redirects | 3 | 重定向次数 |
| spring.data.redis.timeout | 5000ms | 超时 |
| spring.data.redis.lettuce.pool.max-active | 8 | 最大活跃 |
| spring.data.redis.lettuce.pool.max-idle | 8 | 最大空闲 |

### 1.4 其他

| 配置项 | 值 | 说明 |
|--------|-----|------|
| management.endpoints.web.exposure.include | health,info | Actuator |
| management.health.db.enabled | true | 数据库健康检查 |
| management.health.redis.enabled | true | Redis 健康检查 |
| script.http.client.enabled | true | 脚本 HTTP 客户端开关（脚本可调用 ctx.http.request） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
