# open-app — 数据流

> **文档定位**: sddu-docs-relation-flow — 描述本级组件之间的数据流向，含数据源、数据目标、数据格式和转换规则  
> **输出文件名**: relation-flow.md  
> **数据来源**: 代码扫描生成 — 各服务 API/事件/执行链路  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 核心数据流

### 1.1 能力注册 → 订阅 → 消费（API 流）

```
能力提供方 ──► open-server POST /apis ──► v2_api_t（草稿）
    │
    ├──► 审批（approval_record_t）──► 已发布
    │
    └──► 消费方 POST /apps/{appId}/apis/subscribe ──► 审批 ──► subscription_t（已授权）
         │
         └──► 消费方调用 api-server /gateway/api/** ──► 权限校验（permissions/check）──► 下游能力
```

### 1.2 事件发布流（event-server）

```
外部事件源 ──► POST /gateway/events/publish ──► EventGatewayService
    │  校验 topic（v2_event_t）
    ├──► 查订阅缓存（Redis）
    ├──► SSE 推送（/sse/connect/{connectionId}）
    ├──► WebSocket 推送（/ws）
    └──► 内部消息队列
```

### 1.3 回调触发流（event-server）

```
外部系统 ──► POST /gateway/callbacks/invoke ──► CallbackGatewayService
    │  校验 callbackScope
    ├──► 查订阅（Redis）
    └──► 调用消费方 WebHook（channel_address）
```

### 1.4 连接流执行流（connector-api）

```
外部触发 ──► POST /api/v1/flows/{flowId}/invoke ──► FlowInvokeService
    ├──► 读取 flow_t.deployed_version_id ──► flow_version_t.orchestration_config
    ├──► 解析 DAG（trigger → connector → script → parallel → exit）
    │        ├──► connector 节点: HTTP 调用下游连接器
    │        ├──► script 节点: GraalJS 沙箱执行
    │        └──► exit 节点: 组装响应
    ├──► 写 execution_record_t（总览）
    └──► 写 execution_step_t（每节点输入/输出快照）
```

### 1.5 数据开放查询流（api-server）

```
三方平台 ──► GET /gateway/permissions/check ──► 权限校验（user_authorization + subscription）
    ├──► GET /gateway/subscriptions/config ──► 订阅配置
    └──► （授权后）数据查询
```

## 2. 数据格式与转换

| 环节 | 格式 | 转换规则 |
|------|------|---------|
| 编排配置 | JSON（orchestration_config） | React Flow 格式（nodes/edges）→ 执行器 DAG |
| 节点输入/输出 | JSON（input_data/output_data） | 上游输出 → ctx.{nodeId}.output.field |
| 脚本上下文 | JS 对象（ctx） | ES2022 函数参数传递 |
| 连接配置 | JSON（connection_config） | 协议/认证/入参/出参 Schema |
| 事件载荷 | JSON（payload） | 透传 + 脱敏 |
| 订阅缓存 | Redis Key | topic/scope → 订阅方列表 |

## 3. 跨域依赖方向总结

| 源 | 目标 | 数据 |
|----|------|------|
| open-server | api-server | 订阅/权限数据 |
| open-server | event-server | 订阅关系（缓存清除） |
| event-server | api-server | 认证凭证 |
| connector-api | MySQL | 执行记录（open-server 读取展示） |
| connector-api | 下游连接器 | 业务数据 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
