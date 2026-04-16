# 前后端接口规范确认文档

**文档版本**: 1.0.0  
**创建日期**: 2026-03-24  
**状态**: ✅ 联调就绪

---

## 一、统一响应格式

### 1.1 成功响应格式

所有成功响应（HTTP 2xx）使用统一包装格式：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

**字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| code | number | 响应码，0 表示成功 |
| message | string | 响应消息 |
| data | T | 响应数据（泛型） |

### 1.2 错误响应格式

所有错误响应（HTTP 4xx/5xx）返回详细错误信息：

```json
{
  "code": "APP_404_001",
  "message": "应用不存在",
  "details": [
    {
      "field": "id",
      "rejectedValue": "123",
      "message": "无效的应用 ID"
    }
  ],
  "timestamp": "2026-03-24T10:30:00"
}
```

**字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| code | string | 错误码（格式：APP_{HTTP_CODE}_{序号}） |
| message | string | 错误消息 |
| details | array | 错误详情（可选，用于参数验证错误） |
| timestamp | string | 错误发生时间（ISO 8601） |

### 1.3 分页响应格式

列表接口返回分页数据：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

**分页字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 数据列表 |
| total | number | 总条数 |
| page | number | 当前页码（从 1 开始） |
| pageSize | number | 每页条数 |
| totalPages | number | 总页数 |

---

## 二、认证与用户标识

### 2.1 JWT Token 传递

- **Header**: `Authorization: Bearer {token}`
- **Token 来源**: 登录成功后存储在 localStorage

### 2.2 currentUserId 参数

- **传递方式**: 由前端 HTTP 拦截器自动从 JWT Token 解析并添加到请求参数
- **解析优先级**: `sub` > `userId` > `currentUserId`
- **所有接口**: 都需要 currentUserId 参数（用于权限校验和审计）

**前端实现**：
```typescript
// http.ts 请求拦截器自动处理
// 无需在 API 调用时手动添加 currentUserId
```

---

## 三、API 接口清单

### 3.1 应用管理接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/applications` | 创建应用 | 登录用户 |
| GET | `/applications` | 获取应用列表（分页） | 登录用户 |
| GET | `/applications/{id}` | 获取应用详情 | 登录用户 |
| PUT | `/applications/{id}` | 更新应用 | 应用所有者/管理员 |
| DELETE | `/applications/{id}` | 删除应用（软删除） | 应用所有者/管理员 |
| POST | `/applications/{id}/restore` | 恢复应用 | 管理员 |
| PATCH | `/applications/{id}/status` | 变更应用状态 | 管理员 |

### 3.2 接口详细定义

#### POST /applications - 创建应用

**请求参数**：
```typescript
{
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
  
  // Request Body
  name: string           // 必填，应用名称
  description?: string   // 可选，应用描述
  type: string           // 必填，应用类型：self_build | third_party | personal
  iconUrl?: string       // 可选，图标 URL
  callbackUrl?: string   // 可选，回调 URL
}
```

**响应**：
```typescript
{
  code: 0,
  message: "创建成功",
  data: {
    id: string,
    name: string,
    type: string,
    status: string,
    createdAt: string
  }
}
```

#### GET /applications - 获取应用列表

**请求参数**：
```typescript
{
  // Query Parameters
  currentUserId: string  // 由拦截器自动添加
  ownerId?: string       // 可选，按所有者筛选
  status?: string        // 可选，按状态筛选
  page: number           // 页码，从 1 开始，默认 1
  size: number           // 每页条数，默认 20
}
```

**响应**：
```typescript
{
  code: 0,
  message: "success",
  data: {
    items: Application[],
    total: number,
    page: number,
    size: number,
    totalPages: number
  }
}
```

#### GET /applications/{id} - 获取应用详情

**请求参数**：
```typescript
{
  // Path Parameters
  id: string
  
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
}
```

**响应**：
```typescript
{
  code: 0,
  message: "success",
  data: Application
}
```

#### PUT /applications/{id} - 更新应用

**请求参数**：
```typescript
{
  // Path Parameters
  id: string
  
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
  
  // Request Body (所有字段可选)
  name?: string
  description?: string
  iconUrl?: string
  callbackUrl?: string
}
```

**响应**：
```typescript
{
  code: 0,
  message: "更新成功",
  data: Application
}
```

#### DELETE /applications/{id} - 删除应用

**请求参数**：
```typescript
{
  // Path Parameters
  id: string
  
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
}
```

**响应**：
```typescript
{
  code: 0,
  message: "删除成功",
  data: null
}
```

#### POST /applications/{id}/restore - 恢复应用

**请求参数**：
```typescript
{
  // Path Parameters
  id: string
  
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
}
```

**响应**：
```typescript
{
  code: 0,
  message: "恢复成功",
  data: null
}
```

#### PATCH /applications/{id}/status - 变更应用状态

**请求参数**：
```typescript
{
  // Path Parameters
  id: string
  
  // Query Parameters (由拦截器自动添加)
  currentUserId: string
  
  // Request Body
  status: string   // 必填，目标状态：draft | active | disabled | deleted
  reason?: string  // 可选，变更原因
}
```

**响应**：
```typescript
{
  code: 0,
  message: "状态变更成功",
  data: null
}
```

---

## 四、错误码清单

| 错误码 | HTTP 状态码 | 说明 |
|--------|------------|------|
| APP_404_001 | 404 | 应用不存在 |
| APP_409_001 | 409 | 应用名称已存在 |
| APP_400_001 | 400 | 无效的状态转换 |
| APP_403_001 | 403 | 权限不足 |
| APP_400_002 | 400 | 无效的所有者信息 |
| APP_400_003 | 400 | 参数验证失败 |
| APP_410_001 | 410 | 应用已被删除 |
| APP_409_002 | 409 | 数据已被修改，请刷新后重试 |
| APP_400_004 | 400 | 当前状态不允许执行此操作 |
| APP_500_001 | 500 | 数据库操作失败 |
| APP_500_002 | 500 | 系统内部错误 |
| APP_503_001 | 503 | 服务暂时不可用 |

---

## 五、应用状态枚举

| 状态值 | 中文 | 说明 |
|--------|------|------|
| draft | 草稿 | 应用已创建但未发布 |
| active | 已启用 | 应用正常运行中 |
| disabled | 已禁用 | 应用被管理员禁用 |
| deleted | 已删除 | 应用已软删除（可恢复） |

---

## 六、应用类型枚举

| 类型值 | 中文 | 说明 |
|--------|------|------|
| self_build | 自建应用 | 企业自主开发的应用 |
| third_party | 第三方应用 | 第三方开发的应用 |
| personal | 个人应用 | 个人开发者创建的应用 |

---

## 七、前端 API 调用示例

```typescript
import { applicationApi } from '@/services'

// 获取应用列表
const list = await applicationApi.getList({
  page: 1,
  pageSize: 20,
  status: 'active'
})

// 获取应用详情
const detail = await applicationApi.getDetail('app-123')

// 创建应用
const created = await applicationApi.create({
  name: '我的应用',
  type: 'self_build',
  description: '应用描述'
})

// 更新应用
const updated = await applicationApi.update('app-123', {
  name: '新名称'
})

// 删除应用
await applicationApi.delete('app-123')

// 恢复应用（管理员）
await applicationApi.restore('app-123')

// 变更状态（管理员）
await applicationApi.changeStatus('app-123', 'active', '审批通过')
```

**注意**：currentUserId 参数由 http.ts 拦截器自动从 JWT Token 解析并添加，无需手动传递。

---

## 八、联调检查清单

### 后端检查项
- [x] ApiResponse 统一响应包装类已创建
- [x] GlobalExceptionHandler 已配置
- [x] PageResponse 分页响应包装已实现
- [x] Controller 所有接口已使用 ApiResponse 包装
- [x] currentUserId 参数由前端传递

### 前端检查项
- [x] http.ts 拦截器可从 JWT Token 解析 currentUserId
- [x] http.ts 拦截器自动添加 currentUserId 到请求参数
- [x] applicationApi 包含所有 CRUD 方法
- [x] applicationApi 包含 restore 方法
- [x] applicationApi 包含 changeStatus 方法
- [x] 类型定义与后端响应格式一致

### 联合检查项
- [x] 成功响应格式一致（code: 0, message, data）
- [x] 错误响应格式一致（code, message, details, timestamp）
- [x] 分页响应格式一致（items, total, page, pageSize, totalPages）
- [x] currentUserId 传递机制已确认

---

## 九、联调就绪确认

**所有 P0 任务已完成**：

| 任务编号 | 任务名称 | 状态 |
|----------|----------|------|
| TASK-001 | 后端统一响应格式包装器 | ✅ 完成 |
| TASK-002 | 前端 currentUserId 参数传递 | ✅ 完成 |
| TASK-003 | 后端分页响应格式转换 | ✅ 完成 |
| TASK-004 | 前端 restore API | ✅ 完成 |
| TASK-005 | 前端 changeStatus API | ✅ 完成 |
| TASK-006 | 接口规范确认文档 | ✅ 完成 |

**联调状态**: 🟢 就绪

---

## 十、后续步骤

1. **启动后端服务**: `cd application-management && ./mvnw spring-boot:run`
2. **启动前端服务**: `cd application-management-web && npm run dev`
3. **验证接口连通性**: 访问前端应用，测试所有 API 调用
4. **检查浏览器 Network 面板**: 确认请求参数和响应格式正确
5. **检查后端日志**: 确认 currentUserId 正确传递和解析

---

**文档维护**: 接口变更时请同步更新此文档
