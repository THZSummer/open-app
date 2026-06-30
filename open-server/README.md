# open-server

## 启动前配置

以下配置按环境修改 `src/main/resources/application-{profile}.yml`。

### 数据库

修改 `application-dev.yml`（开发）或 `application-prod.yml`（生产）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<你的MySQL地址>:3306/openapp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: <你的用户名>
    password: <你的密码>
```

prod 环境支持环境变量：`MYSQL_HOST`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`

### Redis

开发环境默认使用 **集群模式**，修改 `application-dev.yml`：

```yaml
spring:
  redis:
    cluster:
      nodes:
        - <你的Redis节点1>:6379
        - <你的Redis节点2>:6379
        - <你的Redis节点N>:6379
      max-redirects: 3
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0
```

如使用单机 Redis，注释掉 `cluster` 段，启用文件中已注释的单机配置段。

prod 环境需额外设置环境变量 `REDIS_PASSWORD`。

### 端口 & 上下文路径

`application.yml`：
```yaml
server:
  port: 18080
  servlet:
    context-path: /open-server
```

### 激活 Profile

`application.yml` 中 `spring.profiles.active` 默认 `dev`，生产环境改为 `prod`。

## 启动命令

### 一键启停（推荐）

```bash
# 启动（优先 java -jar，无 jar 自动编译 → 后台运行 → 2s 间隔轮询就绪）
./scripts/start.sh

# 停止
./scripts/stop.sh
```

脚本自动完成：检查/编译 jar → `java -jar` 启动 → 日志输出到 `logs/open-server.log` → PID 记录到 `.pid` → 2s 间隔轮询 `/actuator/health`（最多 60s）。

可通过环境变量切换 profile：`SPRING_PROFILES_ACTIVE=prod ./scripts/start.sh`

### 手动启动

```bash
# 打包
mvn package -DskipTests

# 启动
java -jar target/open-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

服务启动后访问：
- API 地址：`http://localhost:18080/open-server`
- Swagger：`http://localhost:18080/open-server/swagger-ui.html`
- Actuator：`http://localhost:18080/open-server/actuator/health`

## 测试

### Java 单元测试

```bash
cd open-server && mvn test
cd open-server && mvn test -Dtest=FlowPublishValidatorTest
```

### Python 集成测试（需先启动服务）

```bash
pip install requests
python3 open-server/src/test/python/inspect/all.py
python3 open-server/src/test/python/inspect/all.py --quiet
python3 open-server/src/test/python/inspect/flow_create.py
```
