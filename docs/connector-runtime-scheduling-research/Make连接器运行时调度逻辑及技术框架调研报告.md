# Make 连接器运行时调度逻辑及技术框架调研报告

**版本**：V1.0
**日期**：2026年5月
**调研维度**：连接器平台运行时调度逻辑及技术框架（触发模型 / 任务队列 / 执行引擎 / 工作节点 / 错误编排 / 框架选型 / 高可用）

---

## 一、概述

Make（前身为 Integromat）的运行时调度架构围绕**视觉化场景编排**的核心体验设计。其场景（Scenario）模型支持 Router（多路分支）、Iterator（迭代器）、Aggregator（聚合器）等复杂控制流，对应的运行时调度逻辑比 Zapier 更复杂。Make 运行在 **Google Cloud Platform（GCP）** 上，利用 Cloud Run、GKE、Pub/Sub、Firestore 等 GCP 托管服务，调度引擎整体呈现**事件驱动 + 有状态执行**的特征。

---

## 二、触发模型

### 2.1 Trigger 类型

| 类型 | 机制 | 延迟 | 适用场景 |
|------|------|------|---------|
| **Webhook（即时）** | 外部系统 POST 到 Make Webhook URL，立即触发场景 | 秒级 | 实时消息推送、事件回调 |
| **Polling（轮询）** | Make Worker 按间隔轮询第三方 API | 1-15 分钟 | 定时检查新数据 |
| **Scheduled（定时）** | Cron 表达式调度 | 分钟级 | 每日报表、周期同步 |
| **Manual（手动）** | 用户点击"Run Once" | — | 调试测试 |

### 2.2 Webhook 触发架构

```
外部系统 → HTTP POST → 负载均衡器 → Webhook Router → Pub/Sub Topic → 场景执行
```

| 组件 | 技术选型 | 职责 |
|------|---------|------|
| **HTTP 入口** | Cloud Load Balancing | HTTPS 终止、负载分发 |
| **Webhook Router** | Cloud Run | 根据 URL 路径匹配到对应 Scenario，验证签名，投递到 Pub/Sub |
| **消息中间件** | Cloud Pub/Sub | 异步解耦，消息持久化，至少一次投递 |
| **去重** | Firestore | 记录 `event_id` 去重（TTL 24h），防止重复触发 |

### 2.3 Polling 触发

Make 的 Polling 机制与 Zapier 不同——它不依赖中心调度器，而是由场景的 **调度配置** 触发：

| 组件 | 说明 |
|------|------|
| **调度存储** | Scenario JSON 中的 `scheduling` 字段（PostgreSQL JSONB） |
| **调度器** | Cloud Scheduler（Cron 作业），每个活跃场景注册一个 Scheduler 作业 |
| **游标** | 存储在 Data Store 或 Scenario 运行时状态中 |
| **轮询 Worker** | 调度触发后，Worker 执行 Trigger 模块的 poll 操作 |

### 2.4 Schedule 触发

| 属性 | 说明 |
|------|------|
| **调度引擎** | Google Cloud Scheduler |
| **Cron 表达式** | 存储在 Scenario 定义的 `scheduling.cron_expression` 字段 |
| **触发流程** | Cloud Scheduler → HTTP Target → Cloud Run → Pub/Sub → 场景执行 |
| **生命周期** | 场景激活时创建 Scheduler Job，停用时删除 |

---

## 三、任务队列架构

### 3.1 消息传递模型

Make 采用 **Pub/Sub 架构** 而非 Point-to-Point Queue，这是与 Zapier 的关键差异：

```
Trigger 事件 → Pub/Sub Topic → 多个 Subscription → Worker 消费
```

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **事件总线** | Cloud Pub/Sub | 托管消息中间件，支持推送和拉取模式 |
| **执行队列** | Redis List (per-Scenario) | 场景级别的执行队列，控制单场景并发 |
| **延迟队列** | Pub/Sub + Redis Sorted Set | 需要延迟的消息先存 Redis Sorted Set，到期回写 Pub/Sub |
| **Stream (Bundle 流)** | 内存 + Redis | Iterator/Aggregator 场景下 Bundle 在内存中流转 |

### 3.2 场景执行任务模型

```
ExecutionTask = {
  scenario_id: string,
  execution_id: string,
  module_index: number,       // 当前模块索引
  bundle: Bundle,              // 数据包
  cycle_id: string,            // 迭代周期 ID
  retry_count: number,
  created_at: timestamp
}
```

关键区别：Make 的任务粒度是 **模块级别**（per-module），每个模块执行完成后，将下一个模块入队。对于 Iterator，每个迭代元素产生一个独立的 Cycle 任务。

### 3.3 并发控制

| 级别 | 机制 | 说明 |
|------|------|------|
| **场景级别** | Redis 分布式锁 + 并发计数器 | 同一场景同时最多执行 N 次（按订阅计划） |
| **模块级别** | 连接池 + 令牌桶 | 避免对同一个 API 的并发调用超过限流 |
| **租户级别** | Cloud Pub/Sub Subscription 配额 | 按组织/团队的订阅配额限制总并发 |

---

## 四、执行引擎

### 4.1 引擎架构

| 维度 | Make 方案 | 说明 |
|------|----------|------|
| **引擎类型** | 分布式 Worker + 有状态执行 | Bundle/Cycle 在 Worker 内存中流转，必要时持久化 |
| **执行模型** | 模块链式执行 | 每个模块独立执行，输出作为下游模块输入 |
| **隔离** | Cloud Run 容器级隔离 | 每个场景执行在独立的 Cloud Run 容器中运行 |
| **运行时** | Java / Node.js | 核心引擎 Java，连接器模块支持多语言 |
| **内存状态** | Bundle 在内存中传递，Cycle/Iterator 产出的多 Bundle 暂存 Redis |

### 4.2 Iterator/Aggregator 执行模型

这是 Make 最核心的调度创新——**拆分-处理-合并** 模式：

```
触发 → [Iterator] → [模块 A] → [模块 A] → [模块 A] → ... → [Aggregator] → 完成
         ↓           ↓         ↓         ↓                ↓
      [数组]     [Bundle1] [Bundle2] [Bundle3]       [合并为 1 Bundle]
```

| 阶段 | 调度行为 |
|------|---------|
| **Iterator 拆分** | 将输入数组拆分为 N 个独立 Bundle，逐个入队执行 |
| **并行处理** | 非严格串行 —— 多个 Bundle 可被不同 Worker 并行处理 |
| **Aggregator 聚合** | 收集所有子 Bundle 的输出，存入 Redis，待所有子 Bundle 完成后聚合 |
| **完成条件** | Aggregator 有超时和最大等待数两个完成条件 |

### 4.3 数据传递模型

```
Module Input → 参数映射 → API 调用 → 响应解析 → Bundle 输出 → 路由分发
                                                                  ↓
                                                       Router → 多路分发
                                                       Iterator → 拆分
                                                       Aggregator → 等待合并
```

---

## 五、工作节点与扩缩容

### 5.1 Worker 架构

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **Worker 运行时** | Cloud Run (Serverless) | 无服务器容器，自动伸缩到 0 |
| **核心引擎** | 自研 Java 引擎 | 负责模块执行、Bundle 路由、状态管理 |
| **连接器** | Node.js / Python 插件 | 各个连接器模块以插件形式加载 |
| **任务拉取** | Pub/Sub Pull + Redis | Worker 从对应的 Subscription 拉取消息 |

### 5.2 弹性扩缩

| 维度 | 机制 |
|------|------|
| **基础扩缩** | Cloud Run 自动伸缩（基于请求并发数） |
| **预热策略** | 最小实例数配置（付费计划不同，Free: 0, Pro: 1, Teams: 2+） |
| **并发控制** | 每个 Cloud Run 容器最大并发请求数配置（默认 80） |
| **冷启动优化** | 使用 Cloud Run 的 min-instance 特性 + 预热请求 |
| **区域分布** | 多 GCP Region 部署，请求路由到最近区域 |

### 5.3 资源隔离

| 租户等级 | Worker 资源 | 并发上限 | 调度优先级 |
|---------|------------|---------|-----------|
| **Free** | 共享 Cloud Run | 1 场景/次 | 低 |
| **Core** | 共享 Cloud Run | 2 场景/次 | 低 |
| **Pro** | 共享 + 保留 | 5 场景/次 | 中 |
| **Teams** | 共享 + 保留 | 10 场景/次 | 高 |
| **Enterprise** | 专用 Cloud Run | 无限制 | 最高 |

---

## 六、错误编排与重试

### 6.1 错误处理器（一等公民）

Make 将 **Error Handler** 作为一等公民模块，这是与其他平台最大的区别：

```
模块 A → [正常] → 模块 B
         [错误] → Error Handler → 模块 C（告警通知）
```

| 策略 | 行为 | 说明 |
|------|------|------|
| **Ignore** | 忽略错误，继续执行后续模块 | 适用于非关键路径 |
| **Break** | 中断执行，回滚已提交的操作 | 适用于需要事务一致性的场景 |
| **Commit** | 提交已完成的操作，然后中断 | 适用于部分完成仍有意义的场景 |

### 6.2 重试策略

| 属性 | 配置 |
|------|------|
| **最大重试次数** | 默认 3，可配置（0-10） |
| **重试间隔** | 默认 5000ms，可配置 |
| **退避算法** | 指数退避（`interval * backoff_multiplier^attempt`） |
| **退避乘数** | 默认 2，可配置 |
| **最大间隔** | 默认 1 小时 |
| **错误处理器触发** | 超过最大重试次数后，触发 Error Handler 模块 |

### 6.3 可重试错误判断

| 错误类型 | 是否重试 | 说明 |
|---------|---------|------|
| HTTP 429（限流） | 是 | 解析 `Retry-After` 头 |
| HTTP 5xx | 是 | 指数退避 |
| 网络超时 | 是 | 快速重试 1-2 次 |
| HTTP 4xx（非 429） | 否 | 直接失败 |
| 认证失败（401） | 否 | 需要人工刷新凭证 |

---

## 七、编排执行框架选型

### 7.1 框架选择

Make **未采用** 第三方工作流引擎，核心原因：

| 框架 | 未采用原因 |
|------|-----------|
| **Temporal** | Make 引擎比 Temporal 先诞生，自研引擎深度绑定场景编辑器体验 |
| **Camunda** | BPMN 不适合 Iterator/Aggregator/Router 的视觉化模型 |
| **Apache Beam** | 适合批处理和流处理，不适合 API 调用为主的工作流 |

### 7.2 自研引擎关键能力

| 能力 | 实现方式 |
|------|---------|
| **有向无环图执行** | 模块数组 + 路由表 → 拓扑排序 → 递归执行 |
| **Split-Merge** | Iterator 拆分 → 并行执行 → Aggregator 等待合并（Redis 计数） |
| **条件路由** | Router 模块的 routes 配置 → 条件匹配 → 分发到对应模块 |
| **错误路由** | Error Handler 附着到模块 → 错误时跳转到错误子流程 |
| **Bundle 流** | Bundle 在模块间传递，支持一对多（Router）和多对一（Aggregator） |

### 7.3 与 Zapier 对比

| 维度 | Zapier | Make |
|------|--------|------|
| **调度模型** | 线性 + 有限分支（Paths） | DAG + Router + Iterator + Aggregator |
| **任务粒度** | 单步骤 | 单模块（含 Cycle 级别） |
| **引擎风格** | 函数式/无状态 | 有状态（Bundle/Cycle 跟踪） |
| **队列模型** | SQS Point-to-Point | Pub/Sub Pub-Sub + Redis Stream |
| **框架选型** | 自研轻量 + 托管服务 | 自研引擎 + GCP 托管服务 |
| **复杂性** | 低 | 中高 |

---

## 八、高可用与后端框架

### 8.1 物理部署

| 维度 | 方案 |
|------|------|
| **云厂商** | Google Cloud Platform |
| **多 Region** | 主 Region + 灾备 Region（us-central1 + europe-west1） |
| **计算** | Cloud Run（无服务器）+ GKE（有状态组件） |
| **数据库** | Cloud SQL PostgreSQL（跨 AZ）+ Firestore（多 Region） |
| **缓存** | Memorystore Redis（跨 AZ） |
| **消息** | Cloud Pub/Sub（全球多 Region） |
| **对象存储** | Cloud Storage（多 Region / Dual-Region） |

### 8.2 后端框架

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **Web API** | Spring Boot (Java) | 场景管理、模块配置等管理面 API |
| **执行引擎** | 自研 Java 引擎 | 模块执行、Bundle 路由、Cycle 管理 |
| **Webhook 入口** | Cloud Run (Java) | 接收和验证 Webhook 请求 |
| **调度器** | Cloud Scheduler + 自研 | Cron 作业管理 |
| **数据层** | JDBC + Firestore SDK + Redis | 多数据源 |
| **认证服务** | Spring Security + OAuth 2.0 | 认证授权 |
| **监控** | Cloud Monitoring + Logging | GCP 原生监控 |

### 8.3 高可用机制

| 机制 | 实现 | 说明 |
|------|------|------|
| **无状态 API** | Cloud Run 无状态服务 | 管理面 API 可随时扩缩容 |
| **有状态引擎** | 任务状态持久化到 Redis + Firestore | Worker 崩溃可恢复 |
| **消息持久化** | Pub/Sub 至少一次投递 | 消息不会丢失 |
| **优雅关闭** | Cloud Run 的 SIGTERM 处理 | 当前任务完成后退出 |
| **多 AZ 冗余** | Cloud SQL 跨 AZ + Redis 跨 AZ | AZ 级故障不影响 |
| **灾备切换** | 跨 Region DNS 切换 | 分钟级灾备切换 |

### 8.4 关键技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| **引擎语言** | Java | 成熟的线程模型、丰富的生态系统、适合复杂业务逻辑 |
| **执行模型** | 有状态 Worker | Iterator/Aggregator 需要跟踪部分执行状态 |
| **消息中间件** | Pub/Sub（非 Kafka） | GCP 托管、无需运维、支持全球分发 |
| **无服务器计算** | Cloud Run（非 GKE） | 降低运维复杂度、自动伸缩 |
| **编排存储** | PostgreSQL JSONB | 灵活、可查询、成熟稳定 |

---

## 九、关键设计模式总结

| 设计模式 | Make 做法 | 对我们的启示 |
|---------|----------|------------|
| **Iteration as First-Class** | Iterator + Aggregator 是原生模块，调度引擎原生支持 Split-Merge | 如果业务场景需要批量数据处理，调度引擎需要原生支持拆分-合并模式 |
| **Error Handler as Module** | 错误处理是编排定义的一部分，而非运行时配置 | 错误处理逻辑应该可视化和可编排 |
| **Pub/Sub 去耦** | 事件总线和执行队列分离 | 触发和执行解耦，支持不同的消费速度和策略 |
| **有状态 Bundle 流转** | Bundle 在内存中传递，必要时持久化到 Redis | 复杂控制流（Router/Iterator）需要在执行引擎层面管理状态 |
| **Serverless Worker** | Cloud Run 自动伸缩，无需管理 Worker 集群 | 使用 Serverless 容器可极大降低运维成本 |
| **场景级并发控制** | 每个场景独立配置并发上限 | 多租户资源隔离应该在场景级别而非全局级别 |