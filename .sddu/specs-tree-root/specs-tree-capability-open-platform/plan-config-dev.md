# 开发环境配置

> 本文档为 `plan.md` 的子文档，定义能力开放平台的开发环境配置。

---

## 1. 环境变量配置

### 1.1 数据库配置

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=openapp
DB_USERNAME=openapp
DB_PASSWORD=openapp
```

### 1.2 Redis 配置

```bash
# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=openapp
REDIS_DATABASE=0
```

---

## 2. 服务端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| open-server | 8080 | 管理服务 |
| api-server | 8081 | API认证鉴权服务 |
| event-server | 8082 | 事件/回调网关服务 |
| open-web | 3000 | 前端应用 |

---

## 3. 应用配置示例

### 3.1 open-server/application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:openapp}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:openapp}
    password: ${DB_PASSWORD:openapp}
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:openapp}
    database: ${REDIS_DATABASE:0}

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

# Mock 配置
mock:
  enabled: true
```

### 3.2 api-server/application.yml

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:openapp}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:openapp}
    password: ${DB_PASSWORD:openapp}
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:openapp}
    database: ${REDIS_DATABASE:0}

# 内部API网关配置
internal:
  gateway:
    url: http://internal-gateway:9090
```

### 3.3 event-server/application.yml

```yaml
server:
  port: 8082

spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:openapp}
    database: ${REDIS_DATABASE:0}

# api-server 调用配置
api-server:
  url: http://localhost:8081

# 内部消息网关配置
internal:
  message-gateway:
    url: http://internal-message-gateway:9091
```

### 3.4 open-web/.env.development

```bash
# API 基础路径
VITE_API_BASE_URL=http://localhost:8080

# 应用标题
VITE_APP_TITLE=能力开放平台
```

---

## 4. .gitignore 配置示例

### 4.1 后端工程 (.gitignore)

```gitignore
# Maven
target/
!.mvn/wrapper/maven-wrapper.jar
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# IDE
.idea/
*.iml
*.ipr
*.iws
.project
.classpath
.settings/
.vscode/

# Logs
logs/
*.log

# OS
.DS_Store
Thumbs.db
```

### 4.2 前端工程 (.gitignore)

```gitignore
# Dependencies
node_modules/
.pnp
.pnp.js

# Build
dist/
build/
*.local

# IDE
.idea/
.vscode/
*.swp
*.swo

# Environment
.env
.env.local
.env.development.local
.env.test.local
.env.production.local

# Logs
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# OS
.DS_Store
Thumbs.db
```

---

## 5. 数据库初始化

### 5.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS openapp 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'openapp'@'%' IDENTIFIED BY 'openapp';
GRANT ALL PRIVILEGES ON openapp.* TO 'openapp'@'%';
FLUSH PRIVILEGES;
```

### 5.2 初始化表结构

```bash
mysql -u openapp -p openapp < docs/sql/init-schema.sql
mysql -u openapp -p openapp < docs/sql/insert-default-data.sql
```

---

## 6. 本地开发启动

### 6.1 启动后端服务

```bash
# 启动 open-server
cd open-server && mvn spring-boot:run

# 启动 api-server
cd api-server && mvn spring-boot:run

# 启动 event-server
cd event-server && mvn spring-boot:run
```

### 6.2 启动前端应用

```bash
cd open-web
npm install
npm run dev
```

---

**文档版本**: v1.0  
**创建日期**: 2026-04-21  
**作者**: SDDU Plan Agent
