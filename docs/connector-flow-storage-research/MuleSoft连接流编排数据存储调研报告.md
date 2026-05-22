# MuleSoft 连接流编排数据存储调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接流编排数据存储（编排定义存储 / 运行时状态 / 执行历史 / API 治理）

---

## 一、概述

MuleSoft 是企业级 iPaaS 领导者，其编排单元称为 **Flow**（在 Mule Runtime 中执行）。MuleSoft 的编排定义以 **XML 配置**（Mule XML）为主，辅以 DataWeave 脚本。与轻量级 iPaaS 不同，MuleSoft 的存储体系围绕**企业级 API 治理**和**混合部署**设计，支持本地化部署时数据主权可控。底层基础设施支持自管理（On-Premise）和托管（CloudHub）两种模式。

---

## 二、编排定义存储（Flow 配置存储）

### 2.1 核心实体模型

| 实体 | 职责 | 存储方式 |
|------|------|---------|
| **Mule Application** | 可部署的应用单元（.jar 包） | 包含 XML 配置 + Java 类 + DataWeave 脚本 |
| **Flow** | 编排流程定义 | Mule XML `<flow>` 元素 |
| **Sub Flow** | 可复用的子流程 | Mule XML `<sub-flow>` 元素 |
| **Config** | 连接器配置实例 | Mule XML `<*-config>` 元素 |
| **DataWeave Script** | 数据转换脚本 | `.dwl` 文件（独立的转换脚本） |
| **Property** | 环境配置属性 | `.properties` / YAML 文件 |
| **API Spec** | API 规范定义 | RAML / OAS 文件 |

### 2.2 Flow 定义存储（Mule XML）

MuleSoft 的编排定义以 **XML** 格式存储，是 5 个平台中**唯一使用声明式 XML 而非 JSON** 的：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mule xmlns:http="http://www.mulesoft.org/schema/mule/http"
      xmlns:ee="http://www.mulesoft.org/schema/mule/ee/core"
      xmlns="http://www.mulesoft.org/schema/mule/core">

    <!-- 连接器配置 -->
    <http:listener-config name="HTTP_Listener_config">
        <http:listener-connection host="0.0.0.0" port="8081"/>
    </http:request-config>

    <http:request-config name="OpenApp_API_Config">
        <http:request-connection host="api.open-app.com" port="443" protocol="HTTPS"/>
    </http:request-config>

    <!-- 主流程 -->
    <flow name="imMessageToTicketFlow">
        <!-- 入口：HTTP Listener（Webhook 触发） -->
        <http:listener config-ref="HTTP_Listener_config"
                       path="/webhook/im-message"
                       allowedMethods="POST"/>

        <!-- 数据转换：提取消息内容 -->
        <ee:transform>
            <ee:message>
                <ee:set-payload><![CDATA[
                    %dw 2.0
                    output application/json
                    ---
                    {
                        messageId: payload.message_id,
                        content: payload.content,
                        senderId: payload.sender.user_id,
                        chatId: payload.chat_id
                    }
                ]]></ee:set-payload>
            </ee:message>
        </ee:transform>

        <!-- 条件分支：根据消息类型分发 -->
        <choice>
            <when expression="#[vars.messageType == 'bug_report']">
                <flow-ref name="createBugTicket"/>
            </when>
            <when expression="#[vars.messageType == 'feature_request']">
                <flow-ref name="createFeatureRequest"/>
            </when>
            <otherwise>
                <flow-ref name="defaultHandler"/>
            </otherwise>
        </choice>

        <!-- 错误处理 -->
        <error-handler>
            <on-error-continue type="HTTP:CONNECTIVITY">
                <logger message="连接失败: #[error.description]" level="ERROR"/>
                <flow-ref name="sendErrorNotification"/>
            </on-error-continue>
            <on-error-propagate type="ANY">
                <logger message="未处理异常: #[error.description]" level="ERROR"/>
            </on-error-propagate>
        </error-handler>
    </flow>
</mule>
```

### 2.3 项目文件结构

MuleSoft 的编排定义以**标准 Maven 项目**组织，天然支持 Git 版本控制：

```
open-app-integration/
├── src/
│   ├── main/
│   │   ├── java/               # Java 类（自定义连接器/处理器）
│   │   ├── resources/
│   │   │   ├── mule/
│   │   │   │   ├── im-flow.xml           # 主流程定义
│   │   │   │   ├── meeting-flow.xml      # 会议流程
│   │   │   │   ├── contact-sync.xml      # 通讯录同步流程
│   │   │   │   └── error-handler.xml     # 错误处理子流程
│   │   │   ├── dw/
│   │   │   │   ├── im-to-ticket.dwl      # DataWeave 脚本
│   │   │   │   └── format-notification.dwl
│   │   │   ├── api/
│   │   │   │   └── open-app-api.raml     # API 规范
│   │   │   ├── log4j2.xml
│   │   │   └── application.properties     # 环境配置
│   │   └── mule-artifact.json             # 应用打包配置
│   └── test/
│       └── munit/
│           ├── im-flow-test.xml           # MUnit 测试
│           └── meeting-flow-test.xml
├── pom.xml                                # Maven 构建
└── exchange.json                          # Exchange 发布元数据
```

### 2.4 存储技术

| 存储对象 | 技术选型 | 说明 |
|---------|---------|------|
| Flow 定义（XML） | 文件系统 / Git | XML 文件存储在项目目录中，由 Git 管理版本 |
| DataWeave 脚本 | 文件系统 / Git | `.dwl` 独立文件，与 Flow XML 分离 |
| 应用打包 | Maven 构建为 .jar | `mvn package` 构建为可部署的 Mule 应用包 |
| 环境属性 | `.properties` / YAML | 按环境分离（dev/test/prod），CI/CD 注入 |
| API 规范 | RAML / OAS | 存储在 `src/main/resources/api/` 目录 |
| 连接器配置 | Mule XML / Mule SDK | `*-config.xml` 或 Java 注解配置 |
| 运行时状态 | ObjectStore（内置） | Mule Runtime 内置的轻量级键值存储 |

### 2.5 版本管理与 ALM

| 能力 | 实现方式 | 存储策略 |
|------|---------|---------|
| **源码版本** | Git（标准 Maven 项目） | 完整历史，分支策略 |
| **构建版本** | Maven + CI/CD（Jenkins/GitHub Actions） | SNAPSHOT / Release 语义版本 |
| **部署版本** | CloudHub / RTF 部署版本管理 | 保留最近 5 个部署版本 |
| **回滚** | 滚动回滚到前一个部署版本 | CloudHub 自动保留部署包 |
| **配置分离** | 环境属性文件 + 加密配置 | 敏感配置使用 Secure Properties |

---

## 三、运行时状态存储

### 3.1 ObjectStore（内置状态存储）

MuleSoft 提供内置的 **ObjectStore**，用于运行时状态持久化：

| 特性 | 说明 |
|------|------|
| 类型 | 分布式键值存储 |
| 存储引擎 | 内存（默认） / Hazelcast（集群） / 数据库（持久化） |
| 分区 | 按应用分区，应用间隔离 |
| 过期策略 | TTL 支持，自动清理过期键 |
| 持久化 | 可选持久化到数据库（MySQL/PostgreSQL/Oracle） |
| 容量 | CloudHub: 10MB/应用，可扩展 |

**典型用途**：
- 轮询游标：记录最后处理时间戳
- 去重集合：已处理记录 ID 集合
- 计数/状态：跨 Flow 共享计数器
- 临时缓存：频繁查询的结果缓存

### 3.2 执行上下文

Mule Runtime 的执行上下文通过 **Mule Event** 传递：

```
MuleEvent = {
  "correlationId": "uuid",
  "message": {
    "payload": { /* 数据负载 */ },
    "attributes": { /* 元数据 */ },
    "dataType": { "mimeType": "application/json" }
  },
  "variables": {
    "flowVars": { /* Flow 级别变量 */ },
    "sessionVars": { /* 会话级别变量 */ },
    "recordVars": { /* 批量处理记录变量 */ }
  },
  "error": { /* 错误信息 */ },
  "securityContext": { /* 安全上下文 */ }
}
```

### 3.3 执行队列

| 队列类型 | 存储 | 说明 |
|---------|------|------|
| **VM Connector** | Mule Runtime 内部队列 | Flow 间异步通信，内存队列 |
| **JMS/AMQP** | 外部消息队列 | 持久化消息队列（ActiveMQ/RabbitMQ/Kafka） |
| **Anypoint MQ** | CloudHub 托管队列 | SaaS 版消息队列服务 |
| **Scheduler** | Quartz + 数据库 | 定时任务调度器，任务定义持久化 |

---

## 四、执行历史存储

### 4.1 监控数据模型

MuleSoft 的监控数据分为三个层次：

| 层次 | 数据内容 | 存储位置 | 保留策略 |
|------|---------|---------|---------|
| **Flow 级别** | Flow 执行次数、成功率、平均耗时 | CloudHub Analytics / 自建 | 可配置（默认 30 天） |
| **Transaction 级别** | 单次事务的完整链路 | 自建 ELK / Splunk | 按需配置 |
| **日志级别** | 应用日志（INFO/WARN/ERROR） | CloudHub Logs / 自建 | 7 天（CloudHub） |

### 4.2 CloudHub Monitoring 存储

| 监控维度 | 技术选型 | 说明 |
|---------|---------|------|
| **Flow 执行统计** | 内置 Metrics 系统 | 聚合统计（min/max/avg/p99 延迟） |
| **错误追踪** | CloudHub Error Logs | 结构化错误日志 |
| **自定义指标** | Micrometer + 自定义 Reporter | 用户自定义业务指标 |
| **审计日志** | Anypoint Platform Audit Log | 平台操作审计，保留 1 年 |

### 4.3 自建监控方案（企业自管理）

MuleSoft 的企业版支持将监控数据导出到自建系统：

```
Mule Runtime → Log4j2 / Splunk / ELK → 自定义存储

存储方案选项：
1. Elasticsearch：Flow 执行日志全文搜索
2. InfluxDB + Grafana：时序指标可视化
3. Prometheus + AlertManager：自定义告警
4. 关系型数据库：自定义业务报表
```

---

## 五、连接器凭证存储

### 5.1 安全存储方案

| 凭证类型 | 存储方式 | 说明 |
|---------|---------|------|
| **Secure Properties** | 加密的 .properties 文件 | Mule Runtime 内置加密，AES-256 |
| **Anypoint Vault** | 托管密钥管理服务 | 云版本密钥管理 |
| **外部 KMS** | 集成 HashiCorp Vault / Azure Key Vault | 企业版支持 |
| **配置属性** | 环境变量 / 外部配置服务 | 运行时注入 |

### 5.2 加密配置模式

```properties
# application.properties
# 加密属性使用 !{...} 语法
app.client.id=myClientId
app.client.secret=!{AES256:encryptedSecret}
app.api.key=!{AES256:anotherEncryptedKey}

# 运行时解密，Mule Runtime 启动时加载密钥
# 密钥文件：secure-key.properties（需在部署时注入）
```

---

## 六、关键设计模式总结

| 设计模式 | MuleSoft 做法 | 对我们的启示 |
|---------|-------------|------------|
| **文件即编排** | Flow 定义以 XML 文件存储在 Git 项目中 | 编排定义文件化 + Git 版本控制是企业级的标准模式 |
| **配置环境分离** | 环境属性文件与 Flow 定义分离 | 配置分离是 CI/CD 的基础，必须从第一天支持 |
| **ObjectStore** | 内置键值存储 + 可选数据库持久化 | 轻量级内置存储解决运行时状态需求 |
| **DataWeave 脚本独立** | 数据转换脚本与 Flow XML 分离 | 数据转换逻辑独立存储和复用 |
| **无中心化编排存储** | 编排定义随应用部署，不依赖中心数据库 | 本地化部署场景下，编排定义可在应用内自包含 |
| **监控分层** | Flow 级统计 + Transaction 级链路 + 日志级详情 | 三层监控数据模型兼顾性能与诊断能力 |
| **连接器配置声明式** | 连接器配置在 XML 中声明，运行时通过连接池管理 | 配置声明式化 + 连接池复用是资源管理的最佳实践 |