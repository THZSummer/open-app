# api-server — 配置项

> **文档定位**: sddu-docs-config — 配置项文档 — 环境变量、开关、参数说明  
> **输出文件名**: api-server-config.md  
> **数据来源**: 代码扫描生成 — api-server/src/main/resources/application*.yml  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 配置项

### 1.1 基础配置（application.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| server.port | 18081 | 服务端口 |
| server.servlet.context-path | /api-server | 上下文根 |
| spring.profiles.active | dev | 默认 profile |
| spring.application.name | api-server | 应用名 |
| mybatis.mapper-locations | classpath:mapper/*.xml | Mapper XML |
| mybatis.configuration.map-underscore-to-camel-case | true | 驼峰映射 |
| springdoc.api-docs.path | /api-docs | OpenAPI |
| springdoc.swagger-ui.enabled | true | Swagger UI |
| management.endpoints.web.exposure.include | health,info,metrics | Actuator |
| internal.auth.bypass | false | 内部凭证绕过开关（false=校验） |

### 1.2 开发环境（application-dev.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://192.168.3.155:3306/openapp | 开发库 |
| spring.datasource.username / password | openapp / openapp | 账号 |
| hikari.minimum-idle / maximum-pool-size | 5 / 20 | 连接池 |
| spring.data.redis.cluster.nodes | 192.168.3.201~206:6379 | Redis 集群 |
| internal.auth.allowed-accounts | dev-token-001,dev-token-002 | 白名单账号 |

### 1.3 生产环境（application-prod.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://${MYSQL_HOST:localhost}:3306/${MYSQL_DATABASE:openapp} | 环境变量 |
| spring.datasource.username / password | ${MYSQL_USERNAME} / ${MYSQL_PASSWORD} | 环境变量 |
| hikari.minimum-idle / maximum-pool-size | 10 / 50 | 生产连接池 |
| spring.redis.cluster.nodes | 192.168.3.201~205:6379 | Redis 集群（5 节点） |
| spring.redis.password | ${REDIS_PASSWORD:changeme} | Redis 密码 |
| internal.gateway.url | ${INTERNAL_GATEWAY_URL:http://internal-gateway:9090} | 内部网关地址 |
| internal.auth.allowed-accounts | ${INTERNAL_ACCOUNT_PLACEHOLDER} | 生产白名单 |
| mock.enabled | false | Mock 开关 |

### 1.4 环境变量清单

| 环境变量 | 默认值 | 用途 |
|---------|--------|------|
| MYSQL_HOST / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD | openapp 系 | 数据库连接 |
| REDIS_PASSWORD | changeme | Redis 密码 |
| INTERNAL_GATEWAY_URL | http://internal-gateway:9090 | 内部网关 |
| INTERNAL_ACCOUNT_PLACEHOLDER | change-me-in-production | 白名单账号 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
