# 前后端联调就绪确认清单

**生成时间**: 2026-03-24  
**项目**: open-app  
**模块**: application-management / application-management-web

---

## ✅ P0 任务完成状态

| 任务编号 | 任务名称 | 涉及文件 | 状态 |
|----------|----------|----------|------|
| TASK-001 | 后端统一响应格式包装器 | `ApiResponse.java`, `GlobalExceptionHandler.java` | ✅ 完成 |
| TASK-002 | 前端 currentUserId 参数传递 | `http.ts` | ✅ 完成 |
| TASK-003 | 后端分页响应格式转换 | `ApplicationController.java`, `PageResponse.java` | ✅ 完成 |
| TASK-004 | 前端 restore API | `applicationApi.ts` | ✅ 完成 |
| TASK-005 | 前端 changeStatus API | `applicationApi.ts` | ✅ 完成 |
| TASK-006 | 接口规范确认文档 | `api-spec-confirmation.md` | ✅ 完成 |

---

## 📝 代码变更清单

### 后端变更 (application-management)

#### 新增文件
- `src/main/java/com/openapp/application/dto/response/ApiResponse.java` - 统一响应包装类

#### 修改文件
- `src/main/java/com/openapp/application/controller/ApplicationController.java`
  - 所有接口返回类型改为 `ApiResponse<T>` 包装
  - 列表接口返回 `ApiResponse<PageResponse<T>>`
  - 添加成功消息

- `src/main/java/com/openapp/application/exception/GlobalExceptionHandler.java`
  - 添加 ApiResponse 导入（保持 ErrorResponse 用于错误响应）

### 前端变更 (application-management-web)

#### 修改文件
- `src/services/http.ts`
  - 添加 `parseJwtPayload()` 函数解析 JWT
  - 添加 `getCurrentUserIdFromToken()` 函数提取用户 ID
  - 请求拦截器自动从 JWT Token 解析 currentUserId 并添加到请求参数

- `src/services/applicationApi.ts`
  - 所有方法添加返回类型注解
  - 新增 `restore()` 方法
  - 新增 `changeStatus()` 方法

- `src/types/api.ts`
  - 新增 `RestoreApplicationResponse` 类型
  - 新增 `ChangeStatusResponse` 类型
  - 更新 `ErrorResponse` 类型（支持 string/number code，添加 timestamp）
  - 更新 `PaginationData` 注释（页码从 1 开始）

- `src/types/index.ts`
  - 导出新增的响应类型

### 文档变更
- `docs/api-spec-confirmation.md` - 接口规范确认文档（新增）
- `docs/READY_FOR_INTEGRATION.md` - 联调就绪确认清单（本文件）

---

## 🔧 联调前验证步骤

### 1. 后端验证

```bash
cd application-management

# 编译检查
./mvnw compile

# 运行单元测试（如有）
./mvnw test

# 启动开发服务器
./mvnw spring-boot:run
```

**验证点**：
- [ ] 应用启动无错误
- [ ] 访问 `http://localhost:8080/actuator/health` 返回健康状态
- [ ] 测试任意 API 返回格式包含 code/message/data

### 2. 前端验证

```bash
cd application-management-web

# 安装依赖（如未安装）
npm install

# 类型检查
npm run type-check

# 启动开发服务器
npm run dev
```

**验证点**：
- [ ] TypeScript 类型检查通过
- [ ] 应用启动无错误
- [ ] 访问 `http://localhost:5173` 正常加载

### 3. 联调验证

**浏览器 DevTools Network 面板检查**：

1. **请求头验证**
   ```
   Authorization: Bearer <jwt_token>
   ```

2. **请求参数验证**
   ```
   currentUserId=<parsed_from_jwt>
   ```

3. **响应格式验证**
   ```json
   {
     "code": 0,
     "message": "success",
     "data": { ... }
   }
   ```

4. **分页响应验证**
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

---

## 🎯 接口测试清单

### 基础 CRUD 接口

| 接口 | 方法 | 测试状态 | 备注 |
|------|------|----------|------|
| 创建应用 | POST /applications | ⬜ 待测试 | 需要 JWT Token |
| 获取列表 | GET /applications | ⬜ 待测试 | 验证分页格式 |
| 获取详情 | GET /applications/{id} | ⬜ 待测试 | |
| 更新应用 | PUT /applications/{id} | ⬜ 待测试 | |
| 删除应用 | DELETE /applications/{id} | ⬜ 待测试 | 软删除 |

### 管理员接口

| 接口 | 方法 | 测试状态 | 备注 |
|------|------|----------|------|
| 恢复应用 | POST /applications/{id}/restore | ⬜ 待测试 | 需要管理员权限 |
| 变更状态 | PATCH /applications/{id}/status | ⬜ 待测试 | 需要管理员权限 |

---

## 🔐 权限验证清单

| 场景 | 预期结果 | 测试状态 |
|------|----------|----------|
| 未登录访问 API | 返回 401 | ⬜ 待测试 |
| 访问他人应用详情 | 返回 403 | ⬜ 待测试 |
| 修改他人应用 | 返回 403 | ⬜ 待测试 |
| 非管理员恢复应用 | 返回 403 | ⬜ 待测试 |
| 非管理员变更状态 | 返回 403 | ⬜ 待测试 |

---

## ⚠️ 注意事项

### JWT Token 要求
- Token 必须包含 `sub`、`userId` 或 `currentUserId` 字段之一
- Token 格式：`header.payload.signature`（标准 JWT）
- Token 存储在 localStorage 的 `access_token` 键

### 页码约定
- 前端传参：页码从 **1** 开始
- 后端接收：Spring Pageable 页码从 **0** 开始
- Controller 自动转换：`pageNumber + 1`

### currentUserId 传递
- **无需手动传递**：由 http.ts 拦截器自动处理
- 如需调试，检查 Network 面板的请求参数

---

## 📋 问题排查指南

### 问题：响应格式不正确

**检查**：
1. Controller 方法返回类型是否为 `ApiResponse<T>`
2. 是否使用了 `ApiResponse.success()` 包装

### 问题：currentUserId 为 null

**检查**：
1. localStorage 是否有 `access_token`
2. JWT Token 是否包含 `sub`/`userId`/`currentUserId` 字段
3. Token 格式是否正确（3 段式）

### 问题：分页页码不对

**检查**：
1. 前端传参 page 是否从 1 开始
2. Controller 是否正确转换（`page.getNumber() + 1`）

### 问题：401 未授权

**检查**：
1. JWT Token 是否过期
2. Authorization header 是否正确添加
3. Token 前缀是否为 `Bearer `

---

## 📞 联系信息

如有疑问，请参考：
- 接口规范文档：`docs/api-spec-confirmation.md`
- 后端代码：`application-management/src/main/java/com/openapp/application/`
- 前端代码：`application-management-web/src/`

---

## ✅ 联调就绪签字

| 角色 | 姓名 | 日期 | 签字 |
|------|------|------|------|
| 后端开发 | | 2026-03-24 | ⬜ |
| 前端开发 | | 2026-03-24 | ⬜ |
| 测试工程师 | | 2026-03-24 | ⬜ |
| 技术负责人 | | 2026-03-24 | ⬜ |

---

**状态**: 🟢 **联调就绪**

**下一步**: 开始前后端联合调试测试
