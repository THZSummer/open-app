# connector-api

连接器平台运行时服务，基于 Spring WebFlux + R2DBC 全 Reactive 栈，承载连接流同步调度执行引擎、HTTP 触发入口、测试执行接口。

## 启动前配置

### 数据库（R2DBC）

修改 `application.yml`：

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://<你的MySQL地址>:3306/openapp
    username: <你的用户名>
    password: <你的密码>
```

### Redis（Cluster）

修改 `application.yml`：

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - <你的Redis节点1>:6379
          - <你的Redis节点2>:6379
          - <你的Redis节点N>:6379
```

## 端口

`application.yml`：`server.port: 18180`，无 context-path（根路径）

## 一键启停

```bash
# 启动（后台运行，日志在 logs/connector-api.log，PID 在 .pid）
./scripts/start.sh

# 停止
./scripts/stop.sh
```

## 手动启动

```bash
cd connector-api && mvn spring-boot:run
# 服务就绪后访问: http://localhost:18180/actuator/health
```

## 测试

```bash
cd connector-api && mvn test

# Python 集成测试（需先启动服务）
python3 connector-api/src/test/python/inspect/all.py
python3 connector-api/src/test/python/inspect/all.py --quiet
```
