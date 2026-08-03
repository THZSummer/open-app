# event-server — 配置项

> **文档定位**: sddu-docs-config — 配置项文档 — 环境变量、开关、参数说明  
> **输出文件名**: event-server-config.md  
> **数据来源**: 代码扫描生成 — event-server/src/main/resources/application*.yml  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 配置项

### 1.1 基础配置（application.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| server.port | 18082 | 服务端口 |
| server.servlet.context-path | /event-server | 上下文根 |
| spring.profiles.active | dev | 默认 profile |
| spring.application.name | event-server | 应用名 |
| springdoc.api-docs.path | /api-docs | OpenAPI |
| springdoc.swagger-ui.enabled | true | Swagger UI |
| management.endpoints.web.exposure.include | health,info,metrics | Actuator |

### 1.2 Redis 配置（默认单机）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.data.redis.host | localhost | 单机主机（dev） |
| spring.data.redis.port | 6379 | 端口 |
| spring.data.redis.database | 0 | 库 |
| spring.data.redis.timeout | 5000ms | 超时 |
| spring.data.redis.lettuce.pool.max-active | 8 | 连接池 |
| （集群切换，注释保留） | 192.168.3.201~206:6379 | 集群模式可切换 |

### 1.3 认证头配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| auth.headers.soa-token | X-SOA-TOKEN | SOA 凭证头 |
| auth.headers.apig-app-id | X-APIG-APPID | APIG 应用 ID |
| auth.headers.apig-app-key | X-APIG-APPKEY | APIG 应用 Key |
| auth.headers.aksk-token | X-AKSK-TOKEN | AKSK Token |

### 1.4 API Server 对接

| 配置项 | 值 | 说明 |
|--------|-----|------|
| api-server.url | http://localhost:18081/api-server | 上游 api-server |
| api-server.auth.enabled | true | 是否启用认证 |
| （凭证获取） | getApiServerCredential() | 动态获取凭证头 |

### 1.5 开发/生产差异

| Profile | Redis | 说明 |
|---------|-------|------|
| dev | 单机 localhost:6379（密码 openapp） | 本地开发 |
| prod | 集群 192.168.3.201~206:6379（密码 ${REDIS_PASSWORD}） | 生产（见 application-prod.yml） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
