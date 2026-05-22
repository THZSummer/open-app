# 测试用例文档 - open-server 服务

**版本**: v1.0
**编写日期**: 2026-04-22
**服务端口**: 18080
**上下文路径**: /open-server

---

## 一、测试环境

### 1.1 服务配置

| 配置项 | 值 |
|-------|-----|
| 服务端口 | 18080 |
| 上下文路径 | /open-server |
| 数据库连接 | jdbc:mysql://localhost:3306/openplatform_v2 |
| Redis连接 | redis://localhost:6379/0 |

### 1.2 测试数据准备

#### 分类数据

```sql
INSERT INTO openplatform_v2_category_t (id, category_alias, name_cn, name_en, parent_id, path, sort_order, status) VALUES
(1, 'app_type_a', 'A类应用权限', 'App Type A Permissions', NULL, '/1/', 0, 1),
(2, NULL, 'IM业务', 'IM Business', 1, '/1/2/', 0, 1),
(3, NULL, '云盘业务', 'Cloud Business', 1, '/1/3/', 0, 1),
(4, 'app_type_b', 'B类应用权限', 'App Type B Permissions', NULL, '/4/', 0, 1),
(5, NULL, '审批业务', 'Approval Business', 4, '/4/5/', 0, 1);
```

#### API数据

```sql
INSERT INTO openplatform_v2_api_t (id, name_cn, name_en, path, method, status) VALUES
(100, '发送消息', 'Send Message', '/api/v1/messages', 'POST', 2),
(101, '获取消息', 'Get Message', '/api/v1/messages/:id', 'GET', 2),
(102, '删除消息', 'Delete Message', '/api/v1/messages/:id', 'DELETE', 1);

INSERT INTO openplatform_v2_api_p_t (id, parent_id, property_name, property_value, status) VALUES
(1, 100, 'description_cn', '发送消息API描述', 1),
(2, 100, 'doc_url', 'https://docs.example.com/api/send-message', 1);
```

#### 事件数据

```sql
INSERT INTO openplatform_v2_event_t (id, name_cn, name_en, topic, status) VALUES
(200, '消息接收事件', 'Message Received Event', 'im.message.received', 2),
(201, '消息已读事件', 'Message Read Event', 'im.message.read', 2);
```

#### 回调数据

```sql
INSERT INTO openplatform_v2_callback_t (id, name_cn, name_en, status) VALUES
(300, '审批完成回调', 'Approval Completed Callback', 2),
(301, '文件上传回调', 'File Upload Callback', 1);
```

#### 权限数据

```sql
INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, status) VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message', 'api', 100, 2, 1),
(1001, '获取消息权限', 'Get Message Permission', 'api:im:get-message', 'api', 101, 2, 1),
(2000, '消息接收权限', 'Message Received Permission', 'event:im:message-received', 'event', 200, 2, 1),
(3000, '审批完成回调权限', 'Approval Completed Callback Permission', 'callback:approval:completed', 'callback', 300, 5, 1);
```

#### 审批流程数据

```sql
INSERT INTO openplatform_v2_approval_flow_t (id, name_cn, name_en, code, is_default, nodes, status) VALUES
(1, '默认审批流', 'Default Approval Flow', 'default', 1, '[{"type":"approver","userId":"admin","order":1}]', 1),
(2, 'API注册审批流', 'API Register Flow', 'api_register', 0, '[{"type":"approver","userId":"user001","order":1}]', 1);
```

#### 分类责任人数据

```sql
INSERT INTO openplatform_v2_category_owner_t (id, category_id, user_id) VALUES
(1, 2, 'user001'),
(2, 2, 'user002');
```

---

## 二、接口测试用例

### 2.1 分类管理

#### TC-CATEGORY-001: 获取分类列表（树形）

**接口**: GET /api/v1/categories
**优先级**: P0
**前置条件**: 
- 数据库中存在分类数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 发送请求获取分类树 | 无参数 | 返回200，返回完整树形结构 |
| 2 | 按分类别名过滤 | category_alias=app_type_a | 返回200，仅返回A类应用权限树 |
| 3 | 获取空数据 | category_alias=not_exist | 返回200，data为空数组 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1",
      "categoryAlias": "app_type_a",
      "nameCn": "A类应用权限",
      "nameEn": "App Type A Permissions",
      "parentId": null,
      "path": "/1/",
      "sortOrder": 0,
      "status": 1,
      "children": [
        {
          "id": "2",
          "categoryAlias": null,
          "nameCn": "IM业务",
          "nameEn": "IM Business",
          "parentId": "1",
          "path": "/1/2/",
          "sortOrder": 0,
          "status": 1,
          "children": []
        }
      ]
    }
  ],
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 无分类数据 | 数据库清空分类表 | 返回200，data为空数组 |
| 分类别名不存在 | category_alias=invalid | 返回200，data为空数组 |
| 分类别名含特殊字符 | category_alias=test';-- | 返回200，data为空数组（防SQL注入） |

---

#### TC-CATEGORY-002: 获取分类详情

**接口**: GET /api/v1/categories/:id
**优先级**: P0
**前置条件**: 
- 分类ID=1存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的分类详情 | id=1 | 返回200，返回完整分类信息 |
| 2 | 获取不存在的分类 | id=999999 | 返回404，分类不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories/1"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "1",
    "categoryAlias": "app_type_a",
    "nameCn": "A类应用权限",
    "nameEn": "App Type A Permissions",
    "parentId": null,
    "path": "/1/",
    "sortOrder": 0,
    "status": 1,
    "createTime": "2026-04-20T10:00:00.000Z",
    "createBy": "admin"
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| ID不存在 | id=999999 | 返回404 |
| ID格式错误 | id=abc | 返回400，参数格式错误 |
| ID为负数 | id=-1 | 返回400，参数格式错误 |

---

#### TC-CATEGORY-003: 创建分类（一级分类）

**接口**: POST /api/v1/categories
**优先级**: P0
**前置条件**: 
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 创建一级分类 | 完整参数 | 返回200，创建成功 |
| 2 | 创建子分类 | parent_id=1 | 返回200，创建成功 |
| 3 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |
| 4 | 重复创建 | 相同category_alias | 返回409，分类别名已存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/categories" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryAlias": "test_category",
    "nameCn": "测试分类",
    "nameEn": "Test Category",
    "parentId": null,
    "sortOrder": 0
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "categoryAlias": "test_category",
    "nameCn": "测试分类",
    "nameEn": "Test Category",
    "parentId": null,
    "path": "/100/",
    "sortOrder": 0,
    "status": 1
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 名称过长 | name_cn超过100字符 | 返回400，参数校验失败 |
| category_alias过长 | category_alias超过50字符 | 返回400，参数校验失败 |
| parent_id不存在 | parent_id=999999 | 返回404，父分类不存在 |
| 一级分类无category_alias | parent_id=null, category_alias=null | 返回400，一级分类必须设置别名 |

---

#### TC-CATEGORY-004: 更新分类

**接口**: PUT /api/v1/categories/:id
**优先级**: P0
**前置条件**: 
- 分类ID=2存在
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 更新分类名称 | name_cn=IM业务能力 | 返回200，更新成功 |
| 2 | 更新不存在的分类 | id=999999 | 返回404，分类不存在 |
| 3 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/categories/2" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "IM业务能力",
    "nameEn": "IM Business Capability",
    "sortOrder": 1
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "nameCn": "IM业务能力",
    "nameEn": "IM Business Capability",
    "sortOrder": 1
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 更新为已存在名称 | 与其他分类重名 | 返回200，允许重名 |
| sortOrder为负数 | sortOrder=-1 | 返回400，参数校验失败 |

---

#### TC-CATEGORY-005: 删除分类

**接口**: DELETE /api/v1/categories/:id
**优先级**: P0
**前置条件**: 
- 分类ID=5存在且无关联资源
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 删除无关联资源的分类 | id=5 | 返回200，删除成功 |
| 2 | 删除有关联资源的分类 | id=2 | 返回409，存在关联资源 |
| 3 | 删除不存在的分类 | id=999999 | 返回404，分类不存在 |
| 4 | 删除有子分类的分类 | id=1 | 返回409，存在子分类 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18080/open-server/api/v1/categories/5"
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "分类删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**预期响应（失败）**:
```json
{
  "code": "409",
  "messageZh": "分类下存在 5 个 API 资源，无法删除",
  "messageEn": "Conflict",
  "data": null,
  "page": null
}
```

---

#### TC-CATEGORY-006: 添加分类责任人

**接口**: POST /api/v1/categories/:id/owners
**优先级**: P0
**前置条件**: 
- 分类ID=2存在
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 添加责任人 | user_id=user003 | 返回200，添加成功 |
| 2 | 添加已存在的责任人 | user_id=user001 | 返回409，责任人已存在 |
| 3 | 添加到不存在的分类 | category_id=999999 | 返回404，分类不存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/categories/2/owners" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user003",
    "userName": "王五"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "categoryId": "2",
    "userId": "user003",
    "userName": "王五"
  },
  "page": null
}
```

---

#### TC-CATEGORY-007: 获取分类责任人列表

**接口**: GET /api/v1/categories/:id/owners
**优先级**: P0
**前置条件**: 
- 分类ID=2存在，有责任人数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取责任人列表 | id=2 | 返回200，返回责任人列表 |
| 2 | 获取无责任人的分类 | id=5 | 返回200，data为空数组 |
| 3 | 获取不存在的分类 | id=999999 | 返回404，分类不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories/2/owners"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1",
      "categoryId": "2",
      "userId": "user001",
      "userName": "张三"
    },
    {
      "id": "2",
      "categoryId": "2",
      "userId": "user002",
      "userName": "李四"
    }
  ],
  "page": null
}
```

---

#### TC-CATEGORY-008: 移除分类责任人

**接口**: DELETE /api/v1/categories/:id/owners/:userId
**优先级**: P0
**前置条件**: 
- 分类ID=2存在
- 责任人user001存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 移除责任人 | user_id=user001 | 返回200，移除成功 |
| 2 | 移除不存在的责任人 | user_id=not_exist | 返回404，责任人不存在 |
| 3 | 移除不存在的分类 | category_id=999999 | 返回404，分类不存在 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18080/open-server/api/v1/categories/2/owners/user001"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "责任人移除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

### 2.2 API 管理

#### TC-API-001: 获取API列表

**接口**: GET /api/v1/apis
**优先级**: P0
**前置条件**: 
- 数据库中存在API数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取全部API列表 | 无参数 | 返回200，返回分页列表 |
| 2 | 按分类过滤 | category_id=2 | 返回200，返回指定分类API |
| 3 | 按状态过滤 | status=2 | 返回200，返回已发布API |
| 4 | 关键词搜索 | keyword=发送 | 返回200，返回匹配API |
| 5 | 分页查询 | curPage=1&pageSize=10 | 返回200，返回分页数据 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/apis?category_id=2&status=2&curPage=1&pageSize=20"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "100",
      "nameCn": "发送消息",
      "nameEn": "Send Message",
      "path": "/api/v1/messages",
      "method": "POST",
      "categoryId": "2",
      "categoryName": "IM业务",
      "status": 2,
      "permission": {
        "id": "1000",
        "scope": "api:im:send-message",
        "status": 1
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 50
  }
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| 页码超出范围 | curPage=10000 | 返回200，data为空数组 |
| pageSize超大 | pageSize=10000 | 返回200，返回最大100条 |
| 无效状态值 | status=99 | 返回200，data为空数组 |

---

#### TC-API-002: 获取API详情

**接口**: GET /api/v1/apis/:id
**优先级**: P0
**前置条件**: 
- API ID=100存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的API详情 | id=100 | 返回200，返回完整API信息 |
| 2 | 获取不存在的API | id=999999 | 返回404，API不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/apis/100"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息",
    "nameEn": "Send Message",
    "path": "/api/v1/messages",
    "method": "POST",
    "categoryId": "2",
    "status": 2,
    "createTime": "2026-04-20T10:00:00.000Z",
    "createBy": "user001",
    "permission": {
      "id": "1000",
      "nameCn": "发送消息权限",
      "nameEn": "Send Message Permission",
      "scope": "api:im:send-message",
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message" }
    ]
  },
  "page": null
}
```

---

#### TC-API-003: 注册API

**接口**: POST /api/v1/apis
**优先级**: P0
**前置条件**: 
- 用户具有分组责任人权限
- 分类ID=2存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 注册API（完整参数） | 完整JSON | 返回200，注册成功，状态为待审 |
| 2 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |
| 3 | Scope重复 | 已存在的scope | 返回409，Scope已存在 |
| 4 | 分类不存在 | category_id=999999 | 返回404，分类不存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apis" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "发送消息",
    "nameEn": "Send Message",
    "path": "/api/v1/messages",
    "method": "POST",
    "categoryId": "2",
    "permission": {
      "nameCn": "发送消息权限",
      "nameEn": "Send Message Permission",
      "scope": "api:im:send-message"
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message" }
    ]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "API注册成功，等待审批",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息",
    "nameEn": "Send Message",
    "path": "/api/v1/messages",
    "method": "POST",
    "status": 1,
    "permission": {
      "id": "1000",
      "scope": "api:im:send-message",
      "status": 1
    }
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| path格式错误 | path=invalid-path | 返回400，路径格式错误 |
| method不支持 | method=PATCH | 返回400，不支持的HTTP方法 |
| Scope格式错误 | scope=invalid scope | 返回400，Scope格式错误 |

---

#### TC-API-004: 更新API

**接口**: PUT /api/v1/apis/:id
**优先级**: P0
**前置条件**: 
- API ID=100存在
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 更新API信息 | 修改name_cn | 返回200，更新成功 |
| 2 | 修改核心属性 | 修改path/method | 返回200，需重新审批 |
| 3 | 更新不存在的API | id=999999 | 返回404，API不存在 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/apis/100" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "发送消息V2",
    "nameEn": "Send Message V2",
    "categoryId": "2",
    "permission": {
      "nameCn": "发送消息权限V2",
      "nameEn": "Send Message Permission V2"
    }
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "100",
    "nameCn": "发送消息V2",
    "status": 1,
    "message": "API更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### TC-API-005: 删除API

**接口**: DELETE /api/v1/apis/:id
**优先级**: P0
**前置条件**: 
- API ID=102存在且无订阅
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 删除无订阅的API | id=102 | 返回200，删除成功 |
| 2 | 删除有订阅的API | id=100 | 返回409，存在订阅 |
| 3 | 删除不存在的API | id=999999 | 返回404，API不存在 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18080/open-server/api/v1/apis/102"
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "API删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

**预期响应（失败）**:
```json
{
  "code": "409",
  "messageZh": "API被3个应用订阅，无法删除",
  "messageEn": "Conflict",
  "data": null,
  "page": null
}
```

---

#### TC-API-006: 撤回审核中的API

**接口**: POST /api/v1/apis/:id/withdraw
**优先级**: P1
**前置条件**: 
- API ID=102存在，状态为待审(1)
- 用户为API创建者

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审API | id=102 | 返回200，状态变为草稿 |
| 2 | 撤回已发布API | id=100 | 返回400，非待审状态不可撤回 |
| 3 | 撤回不存在的API | id=999999 | 返回404，API不存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apis/102/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "102",
    "status": 0,
    "message": "API已撤回，状态变为草稿"
  },
  "page": null
}
```

---

### 2.3 事件管理

#### TC-EVENT-001: 获取事件列表

**接口**: GET /api/v1/events
**优先级**: P0
**前置条件**: 
- 数据库中存在事件数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取全部事件列表 | 无参数 | 返回200，返回分页列表 |
| 2 | 按分类过滤 | category_id=2 | 返回200，返回指定分类事件 |
| 3 | 按状态过滤 | status=2 | 返回200，返回已发布事件 |
| 4 | 关键词搜索 | keyword=消息 | 返回200，返回匹配事件 |
| 5 | 分页查询 | curPage=1&pageSize=10 | 返回200，返回分页数据 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/events?category_id=2&status=2"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "200",
      "nameCn": "消息接收事件",
      "nameEn": "Message Received Event",
      "topic": "im.message.received",
      "categoryId": "2",
      "status": 2,
      "permission": {
        "id": "2000",
        "scope": "event:im:message-received"
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 30
  }
}
```

---

#### TC-EVENT-002: 获取事件详情

**接口**: GET /api/v1/events/:id
**优先级**: P0
**前置条件**: 
- 事件ID=200存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的事件详情 | id=200 | 返回200，返回完整事件信息 |
| 2 | 获取不存在的事件 | id=999999 | 返回404，事件不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/events/200"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "200",
    "nameCn": "消息接收事件",
    "nameEn": "Message Received Event",
    "topic": "im.message.received",
    "categoryId": "2",
    "status": 2,
    "permission": {
      "id": "2000",
      "nameCn": "消息接收权限",
      "nameEn": "Message Received Permission",
      "scope": "event:im:message-received",
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "消息接收事件描述" },
      { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/event/message-received" }
    ]
  },
  "page": null
}
```

---

#### TC-EVENT-003: 注册事件

**接口**: POST /api/v1/events
**优先级**: P0
**前置条件**: 
- 用户具有分组责任人权限
- 分类ID=2存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 注册事件（完整参数） | 完整JSON | 返回200，注册成功，状态为待审 |
| 2 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |
| 3 | Topic重复 | 已存在的topic | 返回409，Topic已存在 |
| 4 | Scope重复 | 已存在的scope | 返回409，Scope已存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/events" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "消息接收事件",
    "nameEn": "Message Received Event",
    "topic": "im.message.received",
    "categoryId": "2",
    "permission": {
      "nameCn": "消息接收权限",
      "nameEn": "Message Received Permission",
      "scope": "event:im:message-received"
    }
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "事件注册成功，等待审批",
  "messageEn": "Success",
  "data": {
    "id": "200",
    "nameCn": "消息接收事件",
    "topic": "im.message.received",
    "status": 1,
    "permission": {
      "id": "2000",
      "scope": "event:im:message-received"
    }
  },
  "page": null
}
```

---

#### TC-EVENT-004: 更新事件

**接口**: PUT /api/v1/events/:id
**优先级**: P0
**前置条件**: 
- 事件ID=200存在
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 更新事件信息 | 修改name_cn | 返回200，更新成功 |
| 2 | 修改核心属性 | 修改topic | 返回200，需重新审批 |
| 3 | 更新不存在的事件 | id=999999 | 返回404，事件不存在 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/events/200" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "消息接收事件V2",
    "categoryId": "2"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "200",
    "nameCn": "消息接收事件V2",
    "status": 1,
    "message": "事件更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### TC-EVENT-005: 删除事件

**接口**: DELETE /api/v1/events/:id
**优先级**: P0
**前置条件**: 
- 事件存在且无订阅
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 删除无订阅的事件 | id=201 | 返回200，删除成功 |
| 2 | 删除有订阅的事件 | id=200 | 返回409，存在订阅 |
| 3 | 删除不存在的事件 | id=999999 | 返回404，事件不存在 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18080/open-server/api/v1/events/201"
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "事件删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

#### TC-EVENT-006: 撤回审核中的事件

**接口**: POST /api/v1/events/:id/withdraw
**优先级**: P1
**前置条件**: 
- 事件存在，状态为待审(1)
- 用户为事件创建者

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审事件 | id=待审事件 | 返回200，状态变为草稿 |
| 2 | 撤回已发布事件 | id=已发布事件 | 返回400，非待审状态不可撤回 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/events/201/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "201",
    "status": 0,
    "message": "事件已撤回"
  },
  "page": null
}
```

---

### 2.4 回调管理

#### TC-CALLBACK-001: 获取回调列表

**接口**: GET /api/v1/callbacks
**优先级**: P0
**前置条件**: 
- 数据库中存在回调数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取全部回调列表 | 无参数 | 返回200，返回分页列表 |
| 2 | 按分类过滤 | category_id=5 | 返回200，返回指定分类回调 |
| 3 | 按状态过滤 | status=2 | 返回200，返回已发布回调 |
| 4 | 关键词搜索 | keyword=审批 | 返回200，返回匹配回调 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/callbacks?category_id=5&status=2"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "300",
      "nameCn": "审批完成回调",
      "nameEn": "Approval Completed Callback",
      "categoryId": "5",
      "status": 2,
      "permission": {
        "id": "3000",
        "scope": "callback:approval:completed"
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 20
  }
}
```

---

#### TC-CALLBACK-002: 获取回调详情

**接口**: GET /api/v1/callbacks/:id
**优先级**: P0
**前置条件**: 
- 回调ID=300存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的回调详情 | id=300 | 返回200，返回完整回调信息 |
| 2 | 获取不存在的回调 | id=999999 | 返回404，回调不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/callbacks/300"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "300",
    "nameCn": "审批完成回调",
    "nameEn": "Approval Completed Callback",
    "categoryId": "5",
    "status": 2,
    "permission": {
      "id": "3000",
      "nameCn": "审批完成回调权限",
      "nameEn": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed",
      "status": 1
    },
    "properties": [
      { "propertyName": "descriptionCn", "propertyValue": "审批完成后的回调通知" }
    ]
  },
  "page": null
}
```

---

#### TC-CALLBACK-003: 注册回调

**接口**: POST /api/v1/callbacks
**优先级**: P0
**前置条件**: 
- 用户具有分组责任人权限
- 分类ID=5存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 注册回调（完整参数） | 完整JSON | 返回200，注册成功，状态为待审 |
| 2 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |
| 3 | Scope重复 | 已存在的scope | 返回409，Scope已存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/callbacks" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "审批完成回调",
    "nameEn": "Approval Completed Callback",
    "categoryId": "5",
    "permission": {
      "nameCn": "审批完成回调权限",
      "nameEn": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed"
    }
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "回调注册成功，等待审批",
  "messageEn": "Success",
  "data": {
    "id": "300",
    "nameCn": "审批完成回调",
    "status": 1,
    "permission": {
      "id": "3000",
      "scope": "callback:approval:completed"
    }
  },
  "page": null
}
```

---

#### TC-CALLBACK-004: 更新回调

**接口**: PUT /api/v1/callbacks/:id
**优先级**: P0
**前置条件**: 
- 回调ID=300存在
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 更新回调信息 | 修改name_cn | 返回200，更新成功 |
| 2 | 修改核心属性 | 修改scope | 返回200，需重新审批 |
| 3 | 更新不存在的回调 | id=999999 | 返回404，回调不存在 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/callbacks/300" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "审批完成回调V2",
    "categoryId": "5"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "300",
    "nameCn": "审批完成回调V2",
    "status": 1,
    "message": "回调更新成功，核心属性变更需重新审批"
  },
  "page": null
}
```

---

#### TC-CALLBACK-005: 删除回调

**接口**: DELETE /api/v1/callbacks/:id
**优先级**: P0
**前置条件**: 
- 回调存在且无订阅
- 用户具有分组责任人权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 删除无订阅的回调 | id=301 | 返回200，删除成功 |
| 2 | 删除有订阅的回调 | id=已订阅回调 | 返回409，存在订阅 |
| 3 | 删除不存在的回调 | id=999999 | 返回404，回调不存在 |

**请求示例**:
```bash
curl -X DELETE "http://localhost:18080/open-server/api/v1/callbacks/301"
```

**预期响应（成功）**:
```json
{
  "code": "200",
  "messageZh": "回调删除成功",
  "messageEn": "Success",
  "data": null,
  "page": null
}
```

---

#### TC-CALLBACK-006: 撤回审核中的回调

**接口**: POST /api/v1/callbacks/:id/withdraw
**优先级**: P1
**前置条件**: 
- 回调存在，状态为待审(1)
- 用户为回调创建者

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审回调 | id=待审回调 | 返回200，状态变为草稿 |
| 2 | 撤回已发布回调 | id=已发布回调 | 返回400，非待审状态不可撤回 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/callbacks/301/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "301",
    "status": 0,
    "message": "回调已撤回，状态变为草稿"
  },
  "page": null
}
```

---

### 2.5 API 权限管理

#### TC-API-PERM-001: 获取应用API权限列表

**接口**: GET /api/v1/apps/:appId/apis
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 应用有API权限订阅记录

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取应用API权限列表 | appId=10 | 返回200，返回权限列表 |
| 2 | 按状态过滤 | status=1 | 返回200，返回已授权权限 |
| 3 | 关键词搜索 | keyword=发送 | 返回200，返回匹配权限 |
| 4 | 分页查询 | curPage=1&pageSize=10 | 返回200，返回分页数据 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/apps/10/apis?status=1&curPage=1&pageSize=20"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "300",
      "appId": "10",
      "permissionId": "1000",
      "permission": {
        "nameCn": "发送消息权限",
        "scope": "api:im:send-message"
      },
      "api": {
        "path": "/api/v1/messages",
        "method": "POST",
        "docUrl": "https://docs.example.com/api/send-message"
      },
      "category": {
        "id": "2",
        "nameCn": "IM业务",
        "path": "/1/2/",
        "categoryPath": ["A类应用权限", "IM业务"]
      },
      "approver": {
        "userId": "user001",
        "userName": "张三"
      },
      "status": 1,
      "authType": 0,
      "approvalUrl": "https://platform.example.com/approval/300",
      "createTime": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 50
  }
}
```

---

#### TC-API-PERM-002: 获取分类下API权限列表

**接口**: GET /api/v1/categories/:id/apis
**优先级**: P0
**前置条件**: 
- 分类ID=2存在
- 分类下有API权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取分类下权限列表 | id=2 | 返回200，返回权限列表 |
| 2 | 包含子分类权限 | include_children=true | 返回200，递归返回所有子分类权限 |
| 3 | 仅当前分类 | include_children=false | 返回200，仅返回直接权限 |
| 4 | 关键词搜索 | keyword=发送 | 返回200，返回匹配权限 |
| 5 | 按审核需求过滤 | need_approval=1 | 返回200，返回需审核权限 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories/2/apis?include_children=true&curPage=1&pageSize=20"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1000",
      "nameCn": "发送消息权限",
      "nameEn": "Send Message Permission",
      "scope": "api:im:send-message",
      "status": 1,
      "needApproval": 1,
      "isSubscribed": 1,
      "api": {
        "path": "/api/v1/messages",
        "method": "POST",
        "docUrl": "https://docs.example.com/api/send-message"
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 30
  }
}
```

---

#### TC-API-PERM-003: 申请API权限（批量）

**接口**: POST /api/v1/apps/:appId/apis/subscribe
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 权限ID=1000, 1001存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 批量申请权限 | permission_ids=[1000,1001] | 返回200，申请成功 |
| 2 | 重复申请 | 已申请过的权限 | 返回200，幂等处理 |
| 3 | 申请不存在的权限 | permission_ids=[999999] | 返回404，权限不存在 |
| 4 | 空权限列表 | permission_ids=[] | 返回400，参数校验失败 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/apis/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionIds": ["1000", "1001", "1002"]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "300", "appId": "10", "permissionId": "1000", "status": 0 },
      { "id": "301", "appId": "10", "permissionId": "1001", "status": 0 },
      { "id": "302", "appId": "10", "permissionId": "1002", "status": 0 }
    ]
  },
  "page": null
}
```

---

#### TC-API-PERM-004: 撤回API权限申请

**接口**: POST /api/v1/apps/:appId/apis/:id/withdraw
**优先级**: P1
**前置条件**: 
- 订阅ID=300存在，状态为待审(0)

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审申请 | id=待审订阅 | 返回200，状态变为已取消 |
| 2 | 撤回已授权申请 | id=已授权订阅 | 返回400，非待审状态不可撤回 |
| 3 | 撤回不存在的申请 | id=999999 | 返回404，申请不存在 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/apis/300/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "300",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

---

### 2.6 事件权限管理

#### TC-EVENT-PERM-001: 获取应用事件订阅列表

**接口**: GET /api/v1/apps/:appId/events
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 应用有事件订阅记录

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取应用事件订阅列表 | appId=10 | 返回200，返回订阅列表 |
| 2 | 按状态过滤 | status=1 | 返回200，返回已授权订阅 |
| 3 | 关键词搜索 | keyword=消息 | 返回200，返回匹配订阅 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/apps/10/events?status=1"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "301",
      "appId": "10",
      "permissionId": "2000",
      "permission": {
        "nameCn": "消息接收权限",
        "scope": "event:im:message-received"
      },
      "event": {
        "topic": "im.message.received"
      },
      "category": {
        "id": "2",
        "nameCn": "IM业务",
        "path": "/1/2/",
        "categoryPath": ["A类应用权限", "IM业务"]
      },
      "status": 1,
      "channelType": 1,
      "channelAddress": "https://webhook.example.com/events",
      "authType": 0,
      "createTime": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 30
  }
}
```

---

#### TC-EVENT-PERM-002: 获取分类下事件权限列表

**接口**: GET /api/v1/categories/:id/events
**优先级**: P0
**前置条件**: 
- 分类ID=2存在
- 分类下有事件权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取分类下权限列表 | id=2 | 返回200，返回权限列表 |
| 2 | 包含子分类权限 | include_children=true | 返回200，递归返回所有子分类权限 |
| 3 | 关键词搜索 | keyword=消息 | 返回200，返回匹配权限 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories/2/events?include_children=true"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "2000",
      "nameCn": "消息接收权限",
      "nameEn": "Message Received Permission",
      "scope": "event:im:message-received",
      "status": 1,
      "needApproval": 1,
      "isSubscribed": 1,
      "event": {
        "topic": "im.message.received",
        "docUrl": "https://docs.example.com/event/message-received"
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 20
  }
}
```

---

#### TC-EVENT-PERM-003: 申请事件权限（批量）

**接口**: POST /api/v1/apps/:appId/events/subscribe
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 权限ID=2000存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 批量申请权限 | permission_ids=[2000,2001] | 返回200，申请成功 |
| 2 | 重复申请 | 已申请过的权限 | 返回200，幂等处理 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/events/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionIds": ["2000", "2001", "2002"]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "301", "appId": "10", "permissionId": "2000", "status": 0 },
      { "id": "302", "appId": "10", "permissionId": "2001", "status": 0 },
      { "id": "303", "appId": "10", "permissionId": "2002", "status": 0 }
    ]
  },
  "page": null
}
```

---

#### TC-EVENT-PERM-004: 配置事件消费参数

**接口**: PUT /api/v1/apps/:appId/events/:id/config
**优先级**: P0
**前置条件**: 
- 订阅ID=301存在，状态为已授权(1)

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 配置WebHook通道 | channel_type=1, channel_address | 返回200，配置成功 |
| 2 | 配置内部消息队列 | channel_type=0 | 返回200，配置成功 |
| 3 | WebHook缺少地址 | channel_type=1, channel_address为空 | 返回400，参数校验失败 |
| 4 | 配置待审订阅 | 状态为待审 | 返回400，仅已授权可配置 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/apps/10/events/301/config" \
  -H "Content-Type: application/json" \
  -d '{
    "channelType": 1,
    "channelAddress": "https://webhook.example.com/events",
    "authType": 0
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "301",
    "channelType": 1,
    "channelAddress": "https://webhook.example.com/events",
    "authType": 0,
    "message": "事件消费参数配置成功"
  },
  "page": null
}
```

**边界测试**:

| 场景 | 输入 | 预期结果 |
|-----|------|---------|
| channel_address格式错误 | 非URL格式 | 返回400，参数格式错误 |
| auth_type无效 | auth_type=99 | 返回400，无效认证类型 |
| channel_type无效 | channel_type=99 | 返回400，无效通道类型 |

---

#### TC-EVENT-PERM-005: 撤回事件权限申请

**接口**: POST /api/v1/apps/:appId/events/:id/withdraw
**优先级**: P1
**前置条件**: 
- 订阅ID=待审订阅存在，状态为待审(0)

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审申请 | id=待审订阅 | 返回200，状态变为已取消 |
| 2 | 撤回已授权申请 | id=已授权订阅 | 返回400，非待审状态不可撤回 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/events/301/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "301",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

---

### 2.7 回调权限管理

#### TC-CALLBACK-PERM-001: 获取应用回调订阅列表

**接口**: GET /api/v1/apps/:appId/callbacks
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 应用有回调订阅记录

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取应用回调订阅列表 | appId=10 | 返回200，返回订阅列表 |
| 2 | 按状态过滤 | status=1 | 返回200，返回已授权订阅 |
| 3 | 关键词搜索 | keyword=审批 | 返回200，返回匹配订阅 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/apps/10/callbacks?status=1"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "302",
      "appId": "10",
      "permissionId": "3000",
      "permission": {
        "nameCn": "审批完成回调权限",
        "scope": "callback:approval:completed"
      },
      "category": {
        "id": "5",
        "nameCn": "审批回调",
        "path": "/4/5/",
        "categoryPath": ["B类应用权限", "审批回调"]
      },
      "status": 1,
      "channelType": 0,
      "channelAddress": "https://webhook.example.com/callbacks",
      "authType": 0,
      "createTime": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 20
  }
}
```

---

#### TC-CALLBACK-PERM-002: 获取分类下回调权限列表

**接口**: GET /api/v1/categories/:id/callbacks
**优先级**: P0
**前置条件**: 
- 分类ID=5存在
- 分类下有回调权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取分类下权限列表 | id=5 | 返回200，返回权限列表 |
| 2 | 包含子分类权限 | include_children=true | 返回200，递归返回所有子分类权限 |
| 3 | 关键词搜索 | keyword=审批 | 返回200，返回匹配权限 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/categories/5/callbacks?include_children=true"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "3000",
      "nameCn": "审批完成回调权限",
      "nameEn": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed",
      "status": 1,
      "needApproval": 1,
      "isSubscribed": 1,
      "callback": {
        "docUrl": "https://docs.example.com/callback/approval-completed"
      }
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 15
  }
}
```

---

#### TC-CALLBACK-PERM-003: 申请回调权限（批量）

**接口**: POST /api/v1/apps/:appId/callbacks/subscribe
**优先级**: P0
**前置条件**: 
- 应用ID=10存在
- 权限ID=3000存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 批量申请权限 | permission_ids=[3000,3001] | 返回200，申请成功 |
| 2 | 重复申请 | 已申请过的权限 | 返回200，幂等处理 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/callbacks/subscribe" \
  -H "Content-Type: application/json" \
  -d '{
    "permissionIds": ["3000", "3001", "3002"]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "申请已提交，共3条，等待审批",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "records": [
      { "id": "302", "appId": "10", "permissionId": "3000", "status": 0 },
      { "id": "303", "appId": "10", "permissionId": "3001", "status": 0 },
      { "id": "304", "appId": "10", "permissionId": "3002", "status": 0 }
    ]
  },
  "page": null
}
```

---

#### TC-CALLBACK-PERM-004: 配置回调消费参数

**接口**: PUT /api/v1/apps/:appId/callbacks/:id/config
**优先级**: P0
**前置条件**: 
- 订阅ID=302存在，状态为已授权(1)

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 配置WebHook通道 | channel_type=0, channel_address | 返回200，配置成功 |
| 2 | 配置SSE通道 | channel_type=1 | 返回200，配置成功 |
| 3 | 配置WebSocket通道 | channel_type=2 | 返回200，配置成功 |
| 4 | 配置待审订阅 | 状态为待审 | 返回400，仅已授权可配置 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/apps/10/callbacks/302/config" \
  -H "Content-Type: application/json" \
  -d '{
    "channelType": 0,
    "channelAddress": "https://webhook.example.com/callbacks",
    "authType": 0
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "302",
    "channelType": 0,
    "channelAddress": "https://webhook.example.com/callbacks",
    "authType": 0,
    "message": "回调消费参数配置成功"
  },
  "page": null
}
```

---

#### TC-CALLBACK-PERM-005: 撤回回调权限申请

**接口**: POST /api/v1/apps/:appId/callbacks/:id/withdraw
**优先级**: P1
**前置条件**: 
- 订阅ID=待审订阅存在，状态为待审(0)

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤回待审申请 | id=待审订阅 | 返回200，状态变为已取消 |
| 2 | 撤回已授权申请 | id=已授权订阅 | 返回400，非待审状态不可撤回 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/apps/10/callbacks/302/withdraw"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "302",
    "status": 3,
    "message": "申请已撤回"
  },
  "page": null
}
```

---

### 2.8 审批管理

#### TC-APPROVAL-001: 获取审批流程模板列表

**接口**: GET /api/v1/approval-flows
**优先级**: P0
**前置条件**: 
- 数据库中存在审批流程数据

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取审批流程列表 | 无参数 | 返回200，返回流程列表 |
| 2 | 关键词搜索 | keyword=默认 | 返回200，返回匹配流程 |
| 3 | 分页查询 | curPage=1&pageSize=10 | 返回200，返回分页数据 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/approval-flows"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "1",
      "nameCn": "默认审批流",
      "nameEn": "Default Approval Flow",
      "code": "default",
      "isDefault": 1,
      "status": 1
    },
    {
      "id": "2",
      "nameCn": "API注册审批流",
      "nameEn": "API Registration Approval Flow",
      "code": "api_register",
      "isDefault": 0,
      "status": 1
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 5
  }
}
```

---

#### TC-APPROVAL-002: 获取审批流程模板详情

**接口**: GET /api/v1/approval-flows/:id
**优先级**: P0
**前置条件**: 
- 审批流程ID=2存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的流程详情 | id=2 | 返回200，返回完整流程信息含节点 |
| 2 | 获取不存在的流程 | id=999999 | 返回404，流程不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/approval-flows/2"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "nameCn": "API注册审批流",
    "nameEn": "API Registration Approval Flow",
    "code": "api_register",
    "isDefault": 0,
    "status": 1,
    "nodes": [
      { "type": "approver", "userId": "user001", "userName": "张三", "order": 1 },
      { "type": "approver", "userId": "user002", "userName": "李四", "order": 2 }
    ]
  },
  "page": null
}
```

---

#### TC-APPROVAL-003: 创建审批流程模板

**接口**: POST /api/v1/approval-flows
**优先级**: P0
**前置条件**: 
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 创建审批流程 | 完整JSON | 返回200，创建成功 |
| 2 | 缺少必填字段 | name_cn为空 | 返回400，参数校验失败 |
| 3 | code重复 | 已存在的code | 返回409，code已存在 |
| 4 | 设置默认流程 | is_default=1 | 返回200，原默认流程自动取消 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approval-flows" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "API注册审批流",
    "nameEn": "API Registration Approval Flow",
    "code": "api_register",
    "isDefault": 0,
    "nodes": [
      { "type": "approver", "userId": "user001", "order": 1 },
      { "type": "approver", "userId": "user002", "order": 2 }
    ]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "nameCn": "API注册审批流",
    "code": "api_register",
    "isDefault": 0,
    "status": 1
  },
  "page": null
}
```

---

#### TC-APPROVAL-004: 更新审批流程模板

**接口**: PUT /api/v1/approval-flows/:id
**优先级**: P0
**前置条件**: 
- 审批流程ID=2存在
- 用户具有平台运营方权限

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 更新审批流程 | 修改名称和节点 | 返回200，更新成功 |
| 2 | 更新不存在的流程 | id=999999 | 返回404，流程不存在 |
| 3 | 更新禁用流程 | status=0的流程 | 返回200，更新成功 |

**请求示例**:
```bash
curl -X PUT "http://localhost:18080/open-server/api/v1/approval-flows/2" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "API注册审批流V2",
    "nodes": [
      { "type": "approver", "userId": "user003", "order": 1 }
    ]
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "2",
    "nameCn": "API注册审批流V2",
    "status": 1
  },
  "page": null
}
```

---

#### TC-APPROVAL-005: 获取待审批列表

**接口**: GET /api/v1/approvals/pending
**优先级**: P0
**前置条件**: 
- 当前用户有待审批记录

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取待审批列表 | 无参数 | 返回200，返回当前用户待审批列表 |
| 2 | 按类型过滤 | type=resource_register | 返回200，返回资源注册审批 |
| 3 | 关键词搜索 | keyword=发送 | 返回200，返回匹配审批 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/approvals/pending?type=resource_register"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "查询成功",
  "messageEn": "Success",
  "data": [
    {
      "id": "500",
      "type": "resource_register",
      "businessType": "api",
      "businessId": "100",
      "businessName": "发送消息",
      "applicantId": "user003",
      "applicantName": "王五",
      "status": 0,
      "currentNode": 1,
      "createTime": "2026-04-20T10:00:00.000Z"
    }
  ],
  "page": {
    "curPage": 1,
    "pageSize": 20,
    "total": 10
  }
}
```

---

#### TC-APPROVAL-006: 获取审批详情

**接口**: GET /api/v1/approvals/:id
**优先级**: P0
**前置条件**: 
- 审批ID=500存在

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 获取存在的审批详情 | id=500 | 返回200，返回完整审批信息 |
| 2 | 获取不存在的审批 | id=999999 | 返回404，审批不存在 |

**请求示例**:
```bash
curl -X GET "http://localhost:18080/open-server/api/v1/approvals/500"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "type": "resource_register",
    "businessType": "api",
    "businessId": "100",
    "businessData": {
      "nameCn": "发送消息",
      "path": "/api/v1/messages",
      "method": "POST"
    },
    "applicantId": "user003",
    "applicantName": "王五",
    "status": 0,
    "flowId": "2",
    "currentNode": 1,
    "nodes": [
      { "order": 1, "userId": "user001", "userName": "张三", "status": 0 },
      { "order": 2, "userId": "user002", "userName": "李四", "status": null }
    ],
    "logs": []
  },
  "page": null
}
```

---

#### TC-APPROVAL-007: 同意审批

**接口**: POST /api/v1/approvals/:id/approve
**优先级**: P0
**前置条件**: 
- 审批ID=500存在，状态为待审(0)
- 当前用户为当前节点审批人

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 同意审批 | comment=同意 | 返回200，审批通过或流转下一节点 |
| 2 | 非审批人操作 | 非审批人 | 返回403，无权限 |
| 3 | 重复审批 | 已审批过 | 返回400，已处理 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approvals/500/approve" \
  -H "Content-Type: application/json" \
  -d '{
    "comment": "API设计合理，同意上架"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 1,
    "message": "审批通过，API已上架"
  },
  "page": null
}
```

---

#### TC-APPROVAL-008: 驳回审批

**接口**: POST /api/v1/approvals/:id/reject
**优先级**: P0
**前置条件**: 
- 审批ID=500存在，状态为待审(0)
- 当前用户为当前节点审批人

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 驳回审批 | reason=文档缺失 | 返回200，审批驳回 |
| 2 | 缺少驳回原因 | reason为空 | 返回400，驳回原因必填 |
| 3 | 非审批人操作 | 非审批人 | 返回403，无权限 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approvals/500/reject" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "API文档缺失，请补充后重新提交"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 2,
    "message": "审批已驳回"
  },
  "page": null
}
```

---

#### TC-APPROVAL-009: 撤销审批

**接口**: POST /api/v1/approvals/:id/cancel
**优先级**: P1
**前置条件**: 
- 审批ID=500存在，状态为待审(0)
- 当前用户为申请人

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 撤销审批 | 无参数 | 返回200，审批撤销 |
| 2 | 非申请人撤销 | 非申请人 | 返回403，无权限 |
| 3 | 撤销已完成的审批 | status=1 | 返回400，已完成不可撤销 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approvals/500/cancel"
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "id": "500",
    "status": 3,
    "message": "审批已撤销"
  },
  "page": null
}
```

---

#### TC-APPROVAL-010: 批量同意审批

**接口**: POST /api/v1/approvals/batch-approve
**优先级**: P1
**前置条件**: 
- 存在多条待审批记录
- 当前用户为审批人

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 批量同意 | approval_ids=[500,501] | 返回200，批量通过 |
| 2 | 空列表 | approval_ids=[] | 返回400，参数校验失败 |
| 3 | 部分非审批人 | 包含非当前用户审批的 | 返回200，部分成功部分失败 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approvals/batch-approve" \
  -H "Content-Type: application/json" \
  -d '{
    "approvalIds": ["500", "501", "502"],
    "comment": "批量审批通过"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "message": "批量审批通过，共3条"
  },
  "page": null
}
```

---

#### TC-APPROVAL-011: 批量驳回审批

**接口**: POST /api/v1/approvals/batch-reject
**优先级**: P1
**前置条件**: 
- 存在多条待审批记录
- 当前用户为审批人

**测试步骤**:

| 步骤 | 操作 | 输入数据 | 预期结果 |
|-----|------|---------|---------|
| 1 | 批量驳回 | approval_ids + reason | 返回200，批量驳回 |
| 2 | 缺少驳回原因 | reason为空 | 返回400，驳回原因必填 |

**请求示例**:
```bash
curl -X POST "http://localhost:18080/open-server/api/v1/approvals/batch-reject" \
  -H "Content-Type: application/json" \
  -d '{
    "approvalIds": ["500", "501", "502"],
    "reason": "文档不完整，请补充后重新提交"
  }'
```

**预期响应**:
```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "message": "批量驳回成功，共3条"
  },
  "page": null
}
```

---

## 三、测试总结

### 3.1 测试覆盖率

| 模块 | 接口数 | 测试用例数 | 覆盖率 |
|-----|-------|----------|-------|
| 分类管理 | 8 | 8 | 100% |
| API管理 | 6 | 6 | 100% |
| 事件管理 | 6 | 6 | 100% |
| 回调管理 | 6 | 6 | 100% |
| API权限管理 | 4 | 4 | 100% |
| 事件权限管理 | 5 | 5 | 100% |
| 回调权限管理 | 5 | 5 | 100% |
| 审批管理 | 11 | 11 | 100% |
| **总计** | **51** | **51** | **100%** |

### 3.2 测试统计

| 测试类型 | 用例数 | 占比 |
|---------|-------|------|
| 正常流程 | 51 | 50% |
| 异常流程 | 35 | 35% |
| 边界测试 | 15 | 15% |
| **总计** | **101** | **100%** |

### 3.3 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

### 3.4 资源状态枚举

**API/事件/回调状态**:

| 值 | 说明 |
|-----|------|
| 0 | 草稿 |
| 1 | 待审 |
| 2 | 已发布 |
| 3 | 已下线 |

**订阅状态**:

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已授权 |
| 2 | 已拒绝 |
| 3 | 已取消 |

**审批状态**:

| 值 | 说明 |
|-----|------|
| 0 | 待审 |
| 1 | 已通过 |
| 2 | 已拒绝 |
| 3 | 已撤销 |

### 3.5 通道类型枚举

**事件通道类型**:

| 值 | 说明 |
|-----|------|
| 0 | 企业内部消息队列 |
| 1 | WebHook |

**回调通道类型**:

| 值 | 说明 |
|-----|------|
| 0 | WebHook |
| 1 | SSE |
| 2 | WebSocket |

### 3.6 认证类型枚举

| 值 | 说明 |
|-----|------|
| 0 | 应用类凭证A |
| 1 | 应用类凭证B |
| 2 | AKSK |
| 3 | Bearer Token |

---

**文档状态**: ✅ 测试用例编写完成
