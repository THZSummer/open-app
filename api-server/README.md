# api-server

API 认证鉴权服务，负责 API 网关代理鉴权、审批回调处理、Scope 授权管理、权限数据查询。

## 启动前配置

### 数据库

修改 `application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<你的MySQL地址>:3306/openplatform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: <你的用户名>
    password: <你的密码>
```

prod 环境支持环境变量：`MYSQL_HOST`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`

### Redis

修改 `application-dev.yml`（开发环境使用单机模式）：

```yaml
spring:
  data:
    redis:
      host: <你的Redis地址>
      port: 6379
      password: <你的密码>
      database: 0
```

### 端口

`application.yml`：`server.port: 18081`，`context-path: /api-server`

## 一键启停

```bash
# 启动（后台运行，日志在 logs/api-server.log，PID 在 .pid）
./scripts/start.sh

# 停止
./scripts/stop.sh
```

## 手动启动

```bash
cd api-server && mvn spring-boot:run
# 服务就绪后访问: http://localhost:18081/api-server/actuator/health
```

## 测试

```bash
cd api-server && mvn test
```
