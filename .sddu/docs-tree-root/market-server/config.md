# market-server — 配置项

> **文档定位**: sddu-docs-config — 配置项文档 — 环境变量、开关、参数说明  
> **输出文件名**: market-server-config.md  
> **数据来源**: 代码扫描生成 — market-server/src/main/resources/application*.yml  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 配置项

### 1.1 基础配置（application.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| server.port | 18083 | 服务端口 |
| server.servlet.context-path | /market-server | 上下文根 |
| spring.profiles.active | dev | 默认 profile |
| spring.application.name | market-server | 应用名 |
| spring.web.resources.static-locations | file:${java.io.tmpdir}/ | 静态文件目录（dev） |
| springdoc.api-docs.path | /api-docs | OpenAPI |
| springdoc.swagger-ui.enabled | true | Swagger UI |
| management.endpoints.web.exposure.include | health,info,metrics | Actuator |
| platform.approval-url-prefix | https://platform.example.com/approval/ | 审批跳转地址 |
| wecontact.api-url | https://xxx.example.com | 通讯录 API（#ASSUMED） |
| wecontact.tenant-id | 运维人工填入 | x-welink-tenantid |
| wecontact.token | 动态获取 | Authorization |

### 1.2 开发环境（application-dev.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://192.168.3.155:3306/openapp | 开发库 |
| spring.datasource.username / password | openapp / openapp | 账号 |
| hikari.minimum-idle / maximum-pool-size | 5 / 20 | 连接池 |
| spring.data.redis.cluster.nodes | 192.168.3.201~206:6379 | Redis 集群 |
| management.health.db / redis | false / false | 健康检查关闭（避免依赖） |
| ability.file.storage-mode | dev | 文件存储（本地） |
| common.file.storage-mode | dev | 通用文件存储（本地） |
| platform.approval-url-prefix | http://localhost:3000/approval/ | 开发审批地址 |

### 1.3 生产环境（application-prod.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://${MYSQL_HOST:localhost}:3306/${MYSQL_DATABASE:openapp} | 环境变量 |
| spring.datasource.username / password | ${MYSQL_USERNAME} / ${MYSQL_PASSWORD} | 环境变量 |
| hikari.minimum-idle / maximum-pool-size | 10 / 50 | 生产连接池 |
| spring.redis.cluster.nodes | 192.168.3.201~205:6379 | Redis 集群（5 节点） |
| spring.redis.password | ${REDIS_PASSWORD:changeme} | Redis 密码 |
| platform.approval-url-prefix | https://platform.example.com/approval/ | 生产审批地址 |

### 1.4 环境变量清单

| 环境变量 | 默认值 | 用途 |
|---------|--------|------|
| MYSQL_HOST / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD | openapp 系 | 数据库连接 |
| REDIS_PASSWORD | changeme | Redis 密码 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
