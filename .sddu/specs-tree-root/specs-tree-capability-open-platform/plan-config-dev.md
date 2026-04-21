# 开发环境配置

> 本文档为 `plan.md` 的子文档，定义能力开放平台的开发环境配置。
> 采用 Spring Boot Profile 机制，配置文件分为 application.yml 和 application-dev.yml。

---

## 1. 配置文件说明

| 文件 | 说明 | 使用场景 |
|------|------|----------|
| application.yml | 主配置文件，包含公共配置 | 所有环境 |
| application-dev.yml | 开发环境配置 | 开发环境（spring.profiles.active=dev） |
| application-prod.yml | 生产环境配置 | 生产环境（spring.profiles.active=prod） |

---

## 2. 服务端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| open-server | 18080 | 管理服务 |
| api-server | 18081 | API认证鉴权服务 |
| event-server | 18082 | 事件/回调网关服务 |
| open-web | 13000 | 前端应用 |

---

## 3. open-server 配置

### 3.1 application.yml（主配置）

```yaml
spring:
  profiles:
    active: dev
  application:
    name: open-server

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

### 3.2 application-dev.yml（开发环境配置）

```yaml
server:
  port: 18080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openapp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: openapp
    password: openapp
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: openapp
    database: 0

# Mock 配置
mock:
  enabled: true
```

---

## 4. api-server 配置

### 4.1 application.yml（主配置）

```yaml
spring:
  profiles:
    active: dev
  application:
    name: api-server
```

### 4.2 application-dev.yml（开发环境配置）

```yaml
server:
  port: 18081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openapp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: openapp
    password: openapp
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: openapp
    database: 0

# 内部API网关配置
internal:
  gateway:
    url: http://internal-gateway:9090
```

---

## 5. event-server 配置

### 5.1 application.yml（主配置）

```yaml
spring:
  profiles:
    active: dev
  application:
    name: event-server
```

### 5.2 application-dev.yml（开发环境配置）

```yaml
server:
  port: 18082

spring:
  redis:
    host: localhost
    port: 6379
    password: openapp
    database: 0

# api-server 调用配置
api-server:
  url: http://localhost:18081

# 内部消息网关配置
internal:
  message-gateway:
    url: http://internal-message-gateway:9091
```

---

## 6. open-web 前端配置

### 6.1 .env.development

```bash
# API 基础路径
VITE_API_BASE_URL=http://localhost:18080

# 应用标题
VITE_APP_TITLE=能力开放平台
```

---

## 7. .gitignore 配置示例

### 7.1 后端工程 (.gitignore)

```gitignore
# Maven
target/
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
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

### 7.2 前端工程 (.gitignore)

```gitignore
# Dependencies
node_modules/

# Build
dist/
build/
*.local

# Environment
.env.local
.env.development.local
.env.test.local
.env.production.local

# Logs
npm-debug.log*

# OS
.DS_Store
```

---

## 8. 数据库初始化

### 8.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS openapp 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'openapp'@'%' IDENTIFIED BY 'openapp';
GRANT ALL PRIVILEGES ON openapp.* TO 'openapp'@'%';
FLUSH PRIVILEGES;
```

### 8.2 初始化表结构

```bash
mysql -u openapp -p openapp < docs/sql/init-schema.sql
mysql -u openapp -p openapp < docs/sql/insert-default-data.sql
```

---

## 9. 本地开发启动

### 9.1 启动后端服务

```bash
# 启动 open-server
cd open-server && mvn spring-boot:run

# 启动 api-server
cd api-server && mvn spring-boot:run

# 启动 event-server
cd event-server && mvn spring-boot:run
```

### 9.2 启动前端应用

```bash
cd open-web
npm install
npm run dev
```

---

**文档版本**: v1.0  
**创建日期**: 2026-04-21  
**作者**: SDDU Plan Agent