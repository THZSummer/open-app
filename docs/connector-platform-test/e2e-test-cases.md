# 连接器平台 E2E 测试用例

**Feature**: CONN-PLAT-001  
**版本**: MVP  
**创建日期**: 2026-05-24

---

## 测试覆盖

| 用户故事 | 测试场景 | 覆盖任务 |
|---------|---------|---------|
| US-01 连接器管理 | 创建→列表→详情→编辑→删除 | TASK-003 |
| US-02 连接配置 | 配置编辑→查看→编辑即生效 | TASK-003 |
| US-03 连接流管理 | 创建→列表→编排→启停→删除 | TASK-004 |
| US-04 编排与测试 | 编排保存→测试运行→HTTP触发 | TASK-005~008 |

---

## TC-01: 连接器完整生命周期

### 前置条件
- open-server 运行中 (端口 18080)
- MySQL 已初始化 V1+V2 DDL
- 管理员已登录

### 测试步骤

| 步骤 | 操作 | 预期结果 |
|------|------|---------|
| 1.1 | POST /api/v1/connectors 创建连接器 (HTTP类型) | 返回 200, id 非空 |
| 1.2 | GET /api/v1/connectors 列表查询 | 列表包含刚创建的连接器 |
| 1.3 | GET /api/v1/connectors/{id} 详情查询 | 返回基本信息, connectorType=1 |
| 1.4 | PUT /api/v1/connectors/{id} 编辑名称 | 返回 200, 再次详情查询名称已更新 |
| 1.5 | DELETE /api/v1/connectors/{id} 删除 (无引用) | 返回 200 |
| 1.6 | GET /api/v1/connectors/{id} 再次查询 | 返回 404 |

### 验证命令
```bash
# 创建
curl -X POST http://localhost:18080/open-server/api/v1/connectors \
  -H 'Content-Type: application/json' \
  -d '{"nameCn":"IM发送消息","nameEn":"IM Send","connectorType":1}'

# 详情
curl http://localhost:18080/open-server/api/v1/connectors/{id}

# 编辑
curl -X PUT http://localhost:18080/open-server/api/v1/connectors/{id} \
  -H 'Content-Type: application/json' \
  -d '{"nameCn":"IM发送消息-已更新"}'

# 删除
curl -X DELETE http://localhost:18080/open-server/api/v1/connectors/{id}
```

---

## TC-02: 连接器连接配置

### 测试步骤

| 步骤 | 操作 | 预期结果 |
|------|------|---------|
| 2.1 | GET {connectorId}/config (新创建无配置) | hasConfig=false |
| 2.2 | PUT {connectorId}/config 配置connectionConfig | 返回 200 |
| 2.3 | GET {connectorId}/config 查看配置 | hasConfig=true, 配置内容与写入一致 |
| 2.4 | PUT {connectorId}/config 更新配置 (全文替换) | 返回 200, 查看已更新 |

### 验证命令
```bash
curl -X PUT http://localhost:18080/open-server/api/v1/connectors/{id}/config \
  -H 'Content-Type: application/json' \
  -d '{"connectionConfig":{"protocol":"HTTP","protocolConfig":{"url":"https://api.example.com","method":"POST"},"authTypeSchema":{"type":"AKSK","fields":[{"name":"accessKey","carrier":"header","fieldName":"AK","required":true,"sensitive":true}]},"inputSchema":{"type":"object"},"outputSchema":{"type":"object"},"timeoutMs":30000,"rateLimit":{"maxQps":10}}}'
```

---

## TC-03: 连接流完整生命周期

### 前置条件
- 存在可引用的连接器 (TC-01 创建)

### 测试步骤

| 步骤 | 操作 | 预期结果 |
|------|------|---------|
| 3.1 | POST /api/v1/flows 创建连接流 | 返回 200, id 非空, lifecycleStatus=1 |
| 3.2 | GET /api/v1/flows 列表查询 | 列表包含刚创建的连接流 |
| 3.3 | PUT /api/v1/flows/{id} 编辑名称 | 返回 200 |
| 3.4 | PUT /api/v1/flows/{id}/config 保存编排配置 | 返回 200 |
| 3.5 | GET /api/v1/flows/{id}/config 查看编排配置 | 配置内容与保存一致 |
| 3.6 | POST /api/v1/flows/{id}/stop 停止 | lifecycleStatus=2 |
| 3.7 | POST /api/v1/flows/{id}/start 启动 | lifecycleStatus=1 |
| 3.8 | POST /api/v1/flows/{id}/stop → DELETE | 删除成功 |

### 验证命令
```bash
# 创建
curl -X POST http://localhost:18080/open-server/api/v1/flows \
  -H 'Content-Type: application/json' \
  -d '{"nameCn":"新消息通知","nameEn":"New Message Notification"}'

# 保存编排配置
curl -X PUT http://localhost:18080/open-server/api/v1/flows/{flowId}/config \
  -H 'Content-Type: application/json' \
  -d '{"orchestrationConfig":{"nodes":[{"id":"node_entry","type":"entry","labelCn":"接收","labelEn":"Receive"},{"id":"node_exit","type":"exit","labelCn":"返回","labelEn":"Return","outputFields":["result"]}],"edges":[{"id":"e1","sourceNodeId":"node_entry","targetNodeId":"node_exit"}]}}'

# 启停
curl -X POST http://localhost:18080/open-server/api/v1/flows/{flowId}/stop
curl -X POST http://localhost:18080/open-server/api/v1/flows/{flowId}/start
```

---

## TC-04: 编排与测试运行

### 前置条件
- 存在可引用的连接器
- 存在连接流 (配置了编排 + running 状态)

### 测试步骤

| 步骤 | 操作 | 预期结果 |
|------|------|---------|
| 4.1 | POST /{flowId}/config 保存编排配置 (含connector节点) | 返回 200 |
| 4.2 | POST /{flowId}/test-run 测试运行 | 返回完整ExecutionResult, 含各步骤详情 |
| 4.3 | 编排为空(无节点)时POST config | 返回400, 提示至少添加一个节点 |
| 4.4 | POST /trigger/{flowId}/invoke HTTP触发 | 返回执行结果 |

### 验证命令
```bash
# 测试运行
curl -X POST http://localhost:18080/open-server/api/v1/flows/{flowId}/test-run \
  -H 'Content-Type: application/json' \
  -d '{"mockTriggerData":{"sender":"test","content":"hello"},"credentials":{}}'

# 编排为空 (应拒绝)
curl -X PUT http://localhost:18080/open-server/api/v1/flows/{flowId}/config \
  -H 'Content-Type: application/json' \
  -d '{"orchestrationConfig":{"nodes":[],"edges":[]}}'
# 预期: 400

# HTTP 触发 (直接调用 connector-api)
curl -X POST http://localhost:18180/api/v1/trigger/{flowId}/invoke \
  -H 'X-Sys-Token: test-token' \
  -H 'Content-Type: application/json' \
  -d '{"sender":"ext_sys","content":"hello"}'
```

---

## TC-05: 边界场景

| 场景 | 操作 | 预期结果 |
|------|------|---------|
| 删除有引用的连接器 | 流引用连接器时删除连接器 | 400, 提示有引用 |
| 停止运行中的流再删除 | running状态删除 | 400, 提示需先停止 |
| 编排配置JSON格式错误 | 非法的JSON | 400 |
| 限流超额 | 超过maxQps发送请求 | 429 (Too Many Requests) |