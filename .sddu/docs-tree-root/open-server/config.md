# open-server — 配置项

> **文档定位**: sddu-docs-config — 配置项文档 — 环境变量、开关、参数说明  
> **输出文件名**: open-server-config.md  
> **数据来源**: 代码扫描生成 — open-server/src/main/resources/application*.yml  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 配置项

### 1.1 基础配置（application.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| server.port | 18080 | 服务端口 |
| server.servlet.context-path | /open-server | 上下文根 |
| server.servlet.encoding.charset | UTF-8 | 编码 |
| spring.application.name | open-server | 应用名 |
| spring.profiles.active | dev | 默认 profile |
| springdoc.api-docs.path | /api-docs | OpenAPI JSON |
| springdoc.swagger-ui.path | /swagger-ui.html | Swagger UI |
| management.endpoints.web.exposure.include | health,info,metrics | Actuator 暴露 |
| platform.approval-url-prefix | https://platform.example.com/approval/ | 审批跳转地址 |

### 1.2 开发环境（application-dev.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://192.168.3.155:3306/openapp | 开发库 |
| spring.datasource.username | openapp | 账号 |
| hikari.minimum-idle / maximum-pool-size | 5 / 20 | 连接池 |
| spring.data.redis.cluster.nodes | 192.168.3.201~206:6379 | Redis 集群 6 节点 |
| mybatis.mapper-locations | classpath:mapper/*.xml | Mapper XML |
| management.health.db / redis | false / true | 健康检查开关 |
| ability.file.storage-mode | dev | 文件存储模式（本地） |
| platform.approval-url-prefix | http://localhost:3000/approval/ | 开发审批地址 |
| card-service.default-tenant-id | openapp-tenandId | 卡片默认租户 |

### 1.3 生产环境（application-prod.yml）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| spring.datasource.url | jdbc:mysql://${MYSQL_HOST:localhost}:3306/${MYSQL_DATABASE:openapp} | 环境变量注入 |
| spring.datasource.username | ${MYSQL_USERNAME:openapp} | 环境变量 |
| spring.datasource.password | ${MYSQL_PASSWORD:openapp} | 环境变量 |
| hikari.minimum-idle / maximum-pool-size | 10 / 50 | 生产连接池 |
| spring.redis.cluster.nodes | 192.168.3.201~206:6379 | Redis 集群 |
| spring.redis.password | ${REDIS_PASSWORD:changeme} | Redis 密码 |
| platform.approval-url-prefix | https://platform.example.com/approval/ | 生产审批地址 |

### 1.4 环境变量清单

| 环境变量 | 默认值 | 用途 |
|---------|--------|------|
| MYSQL_HOST | localhost | 数据库主机 |
| MYSQL_DATABASE | openapp | 数据库名 |
| MYSQL_USERNAME | openapp | 数据库账号 |
| MYSQL_PASSWORD | openapp | 数据库密码 |
| REDIS_PASSWORD | changeme | Redis 密码 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
