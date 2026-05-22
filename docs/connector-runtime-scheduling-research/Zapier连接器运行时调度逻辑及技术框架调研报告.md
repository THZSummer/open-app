# Zapier 连接器运行时调度逻辑及技术框架调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接器平台运行时调度逻辑及技术框架（触发模型 / 任务队列 / 执行引擎 / 工作节点 / 错误编排 / 框架选型 / 高可用）

---

## 一、概述

Zapier 作为全球最大的 iPaaS 平台（7000+ 应用），其运行时调度体系经过多年演进，形成了以 **AWS 基础设施** 为底座、**事件驱动 + 轮询** 双模式触发、**Redis 队列 + 分布式 Worker** 为核心的调度架构。Zapier 的调度设计充分体现了"轻量 SaaS"的架构哲学——优先使用托管服务（AWS Lambda/SQS/SNS），较少自建调度框架，以运维简化为首要目标。

---

## 二、触发模型

Zapier 的 Zap 执行由触发器（Trigger）驱动，支持三种触发模式：

### 2.1 Polling Trigger（轮询触发）

| 属性 | 说明 |
|------|------|
| **机制** | Zapier Worker 按固定间隔轮询第三方 API，检测是否有新数据 |
| **轮询间隔** | Free: 15 分钟，Starter: 5 分钟，Pro: 2 分钟，Team/Company: 1 分钟 |
| **游标管理** | 使用 Redis + PostgreSQL 双写记录 `cursor`（最后处理时间戳或 ID），保证不丢不错 |
| **增量拉取** | 每次轮询带上游标参数 `since=<cursor>`，拉取增量数据 |
| **并发控制** | Redis 分布式锁防止同一个 Zap 的多次轮询重叠执行 |

### 2.2 Instant Trigger（Webhook 触发）

| 属性 | 说明 |
|------|------|
| **机制** | 第三方服务通过 Webhook URL 实时推送事件到 Zapier |
| **注册方式** | Zap 创建时生成唯一 Webhook URL，注册到第三方应用 |
| **接收层** | AWS API Gateway + Lambda 接收 Webhook 请求 |
| **验证** | 支持可选的签名验证（OAuth / Shared Secret） |
| **去重** | Webhook 请求携带唯一 `event_id`，Redis Set 去重，防止重复触发 |

### 2.3 Schedule Trigger（定时触发）

| 属性 | 说明 |
|------|------|
| **机制** | 基于 AWS EventBridge（CloudWatch Events）调度定时任务 |
| **实现** | 每个 Zap 的调度在 EventBridge 中创建一条规则，Cron 表达式触发 Lambda |
| **精度** | 分钟级，支持秒级延迟绑定 |
| **生命周期** | Zap 激活时创建规则，暂停/删除时自动清理 |

---

## 三、任务队列架构

### 3.1 队列层级

```
触发事件 → 入队 → 调度分发 → Worker 消费 → 步骤执行 → 输出入队/完成
```

| 层级 | 组件 | 职责 |
|------|------|------|
| **入口队列** | AWS SQS | 接收所有触发事件（Webhook / Polling / Schedule），统一入队 |
| **执行队列** | Redis List / Stream | 待执行的 Zap 任务队列，Worker 从 Redis 拉取任务 |
| **延迟队列** | AWS SQS (Delay) | 需要延迟执行的任务（重试、定时等待），利用 SQS 可见性超时 |
| **死信队列** | AWS SQS DLQ | 超过最大重试次数的任务进入死信，触发告警 |

### 3.2 调度策略

| 策略 | 实现方式 |
|------|---------|
| **公平调度** | 多租户共享队列，按 Zap 创建时间轮转，避免单 Zap 饿死 |
| **优先级** | 付费等级高的用户 Zap 优先入队（Free < Pro < Team < Company） |
| **限流** | 基于 Redis 的令牌桶算法，限制每个 Connection 的并发 API 调用数 |
| **背压** | 当 Worker 消费能力不足时，SQS 自动积压消息，触发 Worker 自动扩容 |

### 3.3 任务模型

```
Task = {
  zap_id: string,
  execution_id: string,
  trigger_data: any,
  step_index: number,    // 当前执行到第几步
  retry_count: number,
  priority: number,
  created_at: timestamp
}
```

Zapier 的任务粒度是 **单步骤级别**（per-step），而非整个 Zap —— 每一步执行完成后将下一步入队，这种设计天然支持长流程执行和断点续传。

---

## 四、执行引擎

### 4.1 架构模型

| 维度 | Zapier 方案 | 说明 |
|------|------------|------|
| **引擎类型** | 分布式 Worker 池 | 无中心调度器，Worker 从队列拉取任务执行 |
| **执行模型** | 函数式/无状态 | 每个任务独立执行，不依赖 Worker 本地状态 |
| **隔离** | 进程级隔离 | 每个 Zap 执行在独立的沙箱进程中运行，互不影响 |
| **运行时** | Node.js (Sandboxed) | 自定义连接的 Action/Trigger 代码在 Node.js 沙箱中运行 |
| **超时控制** | 单步 30 分钟 | 每个步骤有独立的超时计时器，超时标记为失败 |

### 4.2 步骤执行流水线

```
Consumer API 调用 → 认证凭证注入 → 参数映射 → HTTP 请求 → 响应解析 → 输出入队
```

| 阶段 | 说明 |
|------|------|
| **凭证注入** | 从加密存储解密 Connection 凭证，注入到请求上下文中 |
| **参数映射** | 根据 Step config 的 `source + field` 模式解析参数值 |
| **HTTP 执行** | 调用目标 API，支持自动重定向和 3xx 处理 |
| **响应解析** | 按 Connector 定义的输出 Schema 提取字段 |
| **后续入队** | 将当前步骤输出作为下一步的输入，推入执行队列 |

### 4.3 数据传递

步骤间通过 **Bundle 对象** 传递数据，Bundle 在内存中构建，序列化后入队：

```
Bundle = {
  inputData: {},         // 当前步骤输入参数
  outputData: {},        // 当前步骤执行结果
  authData: {},          // 解密后的凭证
  meta: {                // 执行元数据
    zap_id, execution_id, step_id, attempt_number, cursor
  },
  environment: {}        // 环境变量
}
```

Bundle 大小限制约 1MB，超大数据通过 S3 引用传递。

---

## 五、工作节点与扩缩容

### 5.1 Worker 架构

| 组件 | 说明 |
|------|------|
| **Worker 进程** | 运行在 AWS ECS（Fargate）上的 Node.js 进程 |
| **Worker 数量** | 动态伸缩，基于 SQS 队列深度自动调整 |
| **扩缩策略** | SQS ApproximateNumberOfMessages > 阈值 → 扩容 CW Alarm → ECS Service Auto Scaling |
| **Worker 生命周期** | 每个 Worker 启动后不断从队列拉取任务，空闲时自动缩减 |

### 5.2 并发模型

| 模型 | 说明 |
|------|------|
| **每 Worker 并发** | 单 Worker 可同时处理多个任务（基于 Promise 并发），默认并发度 10 |
| **全局并发** | 受 Redis 分布式锁控制，每个 Zap 最多 1 个活跃执行 |
| **租户隔离** | 按计划等级分配资源池，Company 用户有专用 Worker 池 |
| **Connection 限流** | 每个 Connection 的 API 调用速率受令牌桶限制，避免触发第三方限流 |

### 5.3 弹性伸缩实现

```
SQS 队列深度指标
    ↓
CloudWatch Alarm（深度 > 1000 → 告警）
    ↓
ECS Service Auto Scaling（+2 Worker / 告警）
    ↓
Worker 注册到 Redis 集群
    ↓
Worker 开始拉取任务
...
队列深度降低 → 缩减 Worker（Cooldown 300s）
```

---

## 六、错误编排与重试

### 6.1 重试层级

| 层级 | 策略 | 存储 |
|------|------|------|
| **单步骤重试** | 最多 3 次，指数退避（10s → 30s → 60s） | Redis + PostgreSQL 记录重试次数 |
| **整体 Zap 重试** | 最终失败后，根据 Zap 配置决定是否自动禁用 Zap | PostgreSQL 更新 Zap 状态 |
| **API 限流重试** | 收到 429 响应时，解析 `Retry-After` 头，延迟重试 | SQS Delay Queue |

### 6.2 错误分类

| 错误类别 | 处理方式 | 示例 |
|---------|---------|------|
| **可重试（临时）** | 自动重试，指数退避 | 网络超时、429 限流、5xx 服务端错误 |
| **不可重试（永久）** | 直接失败，标记 Zap 执行错误 | 400 Bad Request、401 认证失败、404 资源不存在 |
| **需人工干预** | 发送通知给 Zap 创建者，记录错误详情 | 连接凭证过期、API 版本不兼容 |

### 6.3 死信处理

超过最大重试次数的任务进入 SQS DLQ：
- DLQ 消息触发 CloudWatch Alarm
- 通知 Zap 创建者（邮件/推送）
- 运维人员手动分析失败原因
- 修复后可从 DLQ 重新投递

---

## 七、编排执行框架选型

### 7.1 框架选择

Zapier 在运行时调度层面 **未采用第三方编排框架**（如 Temporal / Camunda / Airflow），原因：

| 框架 | 未采用原因 |
|------|-----------|
| **Temporal** | 太重，Zapier 的任务模型较简单（线性 + 有限分支），不需要复杂的工作流状态机 |
| **Camunda** | BPMN/流程引擎适合企业级审批流，不适合轻量 SaaS 场景 |
| **AWS Step Functions** | 单步执行时间限制（15 分钟），不适合长轮询场景 |
| **Apache Airflow** | 面向批处理和数据管道，不适合事件驱动的任务执行 |

### 7.2 自研调度框架

Zapier 的自研调度框架核心组件：

| 组件 | 技术选型 | 职责 |
|------|---------|------|
| **任务队列** | AWS SQS + Redis | 任务存储和分发 |
| **调度协调** | Redis | 分布式锁、游标管理、并发控制 |
| **执行引擎** | Node.js Worker | 无状态任务执行 |
| **状态持久化** | PostgreSQL | 执行结果持久化 |
| **流程定义** | Zap Config (JSONB) | 编排定义存储在 PostgreSQL |
| **定时调度** | AWS EventBridge | 定时触发，Cron 表达式管理 |

### 7.3 设计哲学

```
"Keep it simple" — 用托管服务替代自建框架，用队列替代工作流引擎
```

Zapier 的设计理念是：**人月级别的任务不需要复杂的状态机**。大多数 Zap 只有 2-5 个步骤，线性执行，条件分支有限。通过"单步入队 + 步骤链"的模式，天然支持了"分布式状态机"的能力，而无须引入额外的编排框架。

---

## 八、高可用与后端框架

### 8.1 物理部署

| 维度 | 方案 |
|------|------|
| **云厂商** | AWS |
| **多 AZ** | 所有服务跨 3 个可用区部署 |
| **数据库** | Amazon RDS PostgreSQL Multi-AZ，跨区自动故障转移 |
| **缓存** | Amazon ElastiCache Redis Cluster Mode，多 AZ 自动故障转移 |
| **计算** | ECS Fargate 跨可用区分布，Spot + On-Demand 混合 |
| **对象存储** | Amazon S3，跨 AZ 冗余（11 个 9 持久性） |
| **消息队列** | Amazon SQS，托管高可用 |
| **API 网关** | Amazon API Gateway，多 AZ 自动负载均衡 |
| **CDN** | CloudFront，全球边缘节点 |

### 8.2 后端框架

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **API 服务** | Express.js (Node.js) | Zapier 的后端 API 基于 Express.js |
| **执行引擎** | 自研 Node.js Sandbox | 连接器代码在隔离的 Node.js 沙箱中执行 |
| **数据层** | Sequelize ORM (PostgreSQL) | 关系数据库 ORM 层 |
| **缓存** | ioredis (Node.js Redis Client) | Redis 客户端 |
| **消息消费** | 自研队列消费库 | 基于 SQS SDK + Redis 的消费库 |
| **配置管理** | 环境变量 + Parameter Store | AWS SSM 管理配置 |

### 8.3 高可用保障机制

| 机制 | 实现 | 说明 |
|------|------|------|
| **无状态 Worker** | 所有执行状态在任务消息中传递 | Worker 可随时重启，不影响执行 |
| **任务持久化** | SQS 消息持久化 + 数据库状态 | 消息不丢失，Worker 崩溃后可恢复 |
| **优雅降级** | 限流时自动降级非核心功能 | 优先保障付费用户的 Zap 执行 |
| **熔断** | 当第三方 API 持续失败时，自动熔断 | 减少不必要的重试和资源消耗 |
| **全局限流** | Redis 令牌桶 | 防止突发流量压垮下游系统 |
| **自动灾备** | Route53 DNS 故障转移 | AZ 级故障自动切换到健康 AZ |

### 8.4 关键技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| **异步 vs 同步** | 纯异步（队列驱动） | 解耦触发和执行，天然支持背压和重试 |
| **有状态 vs 无状态** | 无状态 Worker | 简化扩缩容和运维 |
| **日志收集** | CloudWatch Logs → S3 | 托管日志，免运维 |
| **监控告警** | CloudWatch + PagerDuty | 标准监控体系 |
| **链路追踪** | AWS X-Ray | 分布式追踪 |

---

## 九、关键设计模式总结

| 设计模式 | Zapier 做法 | 对我们的启示 |
|---------|-----------|------------|
| **入队即调度** | 每个步骤完成后将下一步入队，天然分布式状态机 | 简化调度逻辑，避免中心化调度器性能瓶颈 |
| **无状态 Worker** | 所有上下文在任务消息中传递 | 弹性伸缩零负担，Worker 任意启停 |
| **托管服务优先** | 使用 SQS/EventBridge/ECS 而非自建 | MVP 阶段尽量使用托管服务降低运维 |
| **步骤级粒度** | 单任务 = 单步骤，而非整个 Zap | 支持长流程执行、断点续传、部分失败 |
| **Redis + 数据库双写** | 游标/锁/状态同时写入 Redis 和数据库 | 兼顾高性能和持久化 |
| **计划等级限流** | Free/PRO/Team 不同资源配额 | 从第一天设计多租户资源隔离 |