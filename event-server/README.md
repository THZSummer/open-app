# event-server

事件/回调网关服务，负责接收业务事件和回调触发，向订阅方分发推送。无独立数据库，通过 api-server 接口获取权限和订阅数据。

## 启动前配置

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

prod 环境使用 Redis Cluster，需设环境变量 `REDIS_PASSWORD`。

### api-server 地址

修改 `application-dev.yml`：

```yaml
api-server:
  url: http://<api-server地址>:18081/api-server
```

prod 环境设环境变量 `API_SERVER_URL`。

### 端口

`application.yml`：`server.port: 18082`，`context-path: /event-server`

## 一键启停

```bash
# 启动（后台运行，日志在 logs/event-server.log，PID 在 .pid）
./scripts/start.sh

# 停止
./scripts/stop.sh
```

## 手动启动

```bash
cd event-server && mvn spring-boot:run
# 服务就绪后访问: http://localhost:18082/event-server/actuator/health
```

## 测试

```bash
cd event-server && mvn test
```
