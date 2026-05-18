# Open-Server 后端与 Wecodesite 前端正则校验对比文档

## 1. 概述

本文档对比分析 open-server（后端）与 wecodesite（前端）表单输入时的正则校验规则，主要针对管理后台新增/修改资源时的数据校验。

---

## 2. 差异场景总览

### 2.1 发现的差异场景统计

| 场景分类 | 数量 | 说明 |
|---------|------|------|
| **前端有校验，后端无校验** | 5 | 存在数据安全隐患 |
| **前后端都有校验，但规则不一致** | 3 | 可能导致用户体验问题 |
| **前后端校验一致** | 4 | 符合预期 |

---

## 3. 前端有校验，后端无校验的场景

这类场景可能导致用户绕过前端校验直接调用接口时，后端无法拦截非法数据。

### 3.1 英文名称（nameEn）

| 位置 | 前端校验 | 后端校验 |
|------|---------|---------|
| **API注册** | `/^[a-zA-Z0-9\s\-_()]+$/` | 仅 `@NotBlank` |
| **Event注册** | `/^[a-zA-Z0-9\s\-_()()]+$/` | 仅 `@NotBlank` |
| **Callback注册** | `/^[a-zA-Z0-9\s\-_()]+$/` | 仅 `@NotBlank` |
| **Category分类** | 无正则（仅必填） | 仅 `@NotBlank` |

**前端文件**：[ResourceRegister.jsx](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/ResourceRegister.jsx#L239), [ApiRegister.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Admin/Api/ApiRegister.jsx#L189)

**后端文件**：[ApiCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/dto/ApiCreateRequest.java#L34-L36), [EventCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/dto/EventCreateRequest.java#L34-L36), [CallbackCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/callback/dto/CallbackCreateRequest.java#L33-L34)

**风险评估**：🟡 中风险 - 允许输入中文字符可能不符合业务规范

---

### 3.2 Topic 标识（仅 Event）

| 位置 | 前端校验 | 后端校验 |
|------|---------|---------|
| **Event注册** | `/^[a-zA-Z][a-zA-Z0-9]*\.[a-zA-Z][a-zA-Z0-9.]*$/` | 仅 `@NotBlank` |

**前端文件**：[constants.js](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js#L39)

**后端文件**：[EventCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/dto/EventCreateRequest.java#L41)

**示例**：
- 前端要求格式：`user.status`, `im.message.received`
- 后端仅检查非空

**风险评估**：🟡 中风险 - 可能导致数据格式不统一

---

### 3.3 API 路径（path）

| 位置 | 前端校验 | 后端校验 |
|------|---------|---------|
| **API注册** | `/^\//` | 仅 `@NotBlank` |

**前端文件**：[constants.js](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js#L67), [ApiRegister.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Admin/Api/ApiRegister.jsx#L215)

**后端文件**：[ApiCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/dto/ApiCreateRequest.java#L41)

**风险评估**：🟢 低风险 - 后端实际使用时可能自动处理

---

### 3.4 回调/事件订阅地址（URL）

| 位置 | 前端校验 | 后端校验 |
|------|---------|---------|
| **回调配置** | `/^https?:\/\/.+/` | 无校验（字段可选） |
| **事件订阅** | `/^https?:\/\/.+/` | 无校验（字段可选） |

**前端文件**：[CallbackConfigDrawer.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Callbacks/CallbackConfigDrawer.jsx#L102), [EventSubscriptionDrawer.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Events/EventSubscriptionDrawer.jsx#L116)

**后端文件**：[SubscriptionConfigRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/permission/dto/SubscriptionConfigRequest.java#L30) - 字段定义为 `channelAddress`，无校验注解

**风险评估**：🟡 中风险 - 允许非HTTP协议地址可能导致功能异常

---

### 3.5 审批流程代码（code）

| 位置 | 前端校验 | 后端校验 |
|------|---------|---------|
| **审批流程创建** | `/^[a-z_]+$/` | 无正则，仅检查唯一性 |

**前端文件**：[ApprovalFlowFormModal.jsx](file:///d:/myProject/open-app/wecodesite/src/components/ApprovalFlowFormModal/ApprovalFlowFormModal.jsx#L114)

**后端文件**：[ApprovalFlowCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/dto/ApprovalFlowCreateRequest.java) - 无任何校验注解

**Service层**：[ApprovalService.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/service/ApprovalService.java#L143) - 仅检查 `code` 唯一性

**示例**：
- 前端要求：`api_register`, `global`, `im_permission_apply`
- 后端不限制：任何字符串只要不重复即可

**风险评估**：🔴 高风险 - 可能创建不符合规范的审批流程代码

---

## 4. 前后端都有校验但规则不一致的场景

### 4.1 Scope 标识校验（核心差异）

Scope 标识是 API、事件、回调的核心权限标识，三种资源类型都存在校验差异。

#### 4.1.1 API Scope

| 层级 | 正则表达式 | 说明 |
|------|-----------|------|
| **后端** | `^api:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]*$` | 要求小写字母开头 |
| **前端** | `/^api:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$/` | 允许大小写 |

**后端文件**：[ApiService.java#L44](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/service/ApiService.java#L44), [PermissionCreateRequest.java#L40](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/dto/PermissionCreateRequest.java#L40)

**前端文件**：[constants.js#L56](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js#L56), [ApiRegister.jsx#L272](file:///d:/myProject/open-app/wecodesite/src/pages/Admin/Api/ApiRegister.jsx#L272)

**差异详情**：
| 字段部分 | 后端 | 前端 | 风险 |
|---------|------|------|------|
| 模块名（第二段） | `[a-z][a-z0-9_]*` | `[a-zA-Z0-9_-]+` | 🔴 高 |
| 资源标识（第三段） | `[a-z][a-z0-9_-]*` | `[a-zA-Z0-9_-]+` | 🔴 高 |

**问题示例**：
- `api:IM:sendMessage` - 前端通过 ❌ → 后端拒绝 ✅
- `api:a:` - 前端拒绝 ✅ → 后端拒绝 ✅（一致）
- `api::send` - 前端通过 ❌ → 后端拒绝 ✅

---

#### 4.1.2 Event Scope

| 层级 | 正则表达式 | 说明 |
|------|-----------|------|
| **后端** | `^event:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]*$` | 要求小写字母开头 |
| **前端** | `/^event:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$/` | 允许大小写 |

**后端文件**：[EventService.java#L43](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/service/EventService.java#L43), [PermissionDto.java#L47](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/dto/PermissionDto.java#L47)

**前端文件**：[constants.js#L28](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js#L28)

---

#### 4.1.3 Callback Scope

| 层级 | 正则表达式 | 说明 |
|------|-----------|------|
| **后端** | `^callback:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]*$` | 要求小写字母开头 |
| **前端** | `/^callback:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$/` | 允许大小写 |

**后端文件**：[CallbackService.java#L54](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/callback/service/CallbackService.java#L54)

**前端文件**：[constants.js#L12](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js#L12)

---

## 5. 前后端校验一致的字段

### 5.1 必填字段校验

以下字段前后端均使用 `@NotBlank` / `required: true` 校验，一致性良好：

| 字段 | API | Event | Callback | Category | 审批节点 |
|------|-----|-------|----------|----------|---------|
| nameCn（中文名称） | ✅ | ✅ | ✅ | ✅ | ✅ |
| nameEn（英文名称） | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ✅（仅必填） |
| categoryId（分类ID） | ✅ | ✅ | ✅ | - | - |
| method（HTTP方法） | ✅ | - | - | - | - |
| permissionNameCn（权限中文名） | ✅ | ✅ | ✅ | - | - |
| permissionNameEn（权限英文名） | ⚠️ | ⚠️ | ⚠️ | - | - |
| scope（Scope标识） | ⚠️ | ⚠️ | ⚠️ | - | - |
| userId（用户ID） | - | - | - | ✅ | ✅ |
| userName（用户姓名） | - | - | - | - | ✅ |
| propertyName（属性名） | ✅ | ✅ | ✅ | - | - |
| propertyValue（属性值） | ✅ | ✅ | ✅ | - | - |

**图例**：
- ✅ = 前后端校验一致
- ⚠️ = 有校验但规则不一致
- - = 不适用

---

## 6. 风险汇总与修复建议

### 6.1 风险等级分类

| 风险等级 | 字段/场景 | 说明 |
|---------|----------|------|
| 🔴 高 | Scope 校验不一致 | 大小写敏感问题，优先级最高 |
| 🔴 高 | ApprovalFlow.code 无后端校验 | 允许非法格式代码 |
| 🟡 中 | nameEn 无后端正则 | 允许输入中文字符 |
| 🟡 中 | Topic 无后端正则 | 允许非法格式 |
| 🟡 中 | URL 无后端正则 | 允许非HTTP协议 |
| 🟢 低 | API Path 前置校验 | 可接受差异 |

---

### 6.2 修复建议

#### 优先级 P0（必须修复）

**1. 统一 Scope 正则校验**

建议修改前端正则与后端一致：

```javascript
// 修改文件：wecodesite/src/components/ResourceRegister/constants.js

// API Scope
scopePattern: /^api:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]+$/,

// Event Scope
scopePattern: /^event:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]+$/,

// Callback Scope
scopePattern: /^callback:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]+$/,
```

同时修改 ApiRegister.jsx：
```javascript
// 修改文件：wecodesite/src/pages/Admin/Api/ApiRegister.jsx
{ pattern: /^api:[a-z][a-z0-9_]*:[a-z][a-z0-9_-]+$/, message: '格式不正确，应为：api:{模块}:{资源标识}' }
```

**2. 添加 ApprovalFlow.code 后端校验**

建议在 `ApprovalFlowCreateRequest.java` 添加注解：
```java
@NotBlank(message = "流程代码不能为空")
@Pattern(regexp = "^[a-z_]+$", message = "流程代码只能包含小写字母和下划线")
private String code;
```

#### 优先级 P1（建议修复）

**3. 添加 nameEn 后端正则**

建议在相关 DTO 中添加：
```java
@NotBlank(message = "英文名称不能为空")
@Pattern(regexp = "^[a-zA-Z0-9\\s\\-_()]+$", message = "英文名称只能包含英文字母、数字、空格、下划线、连字符和括号")
private String nameEn;
```

**4. 添加 Topic 后端正则**

建议在 `EventCreateRequest.java` 添加：
```java
@NotBlank(message = "Topic 不能为空")
@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9]*\\.[a-zA-Z][a-zA-Z0-9.]*$", 
         message = "Topic 格式错误，正确格式：模块.事件，如 user.status")
private String topic;
```

---

## 7. 完整文件清单

### 后端校验相关文件

| 文件 | 校验类型 |
|------|---------|
| [ApiService.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/service/ApiService.java) | SCOPE_PATTERN 正则 |
| [EventService.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/service/EventService.java) | SCOPE_PATTERN 正则 |
| [CallbackService.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/callback/service/CallbackService.java) | SCOPE_PATTERN 正则 |
| [ApprovalService.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/service/ApprovalService.java) | code 唯一性检查 |
| [ApiCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/dto/ApiCreateRequest.java) | @NotBlank 注解 |
| [EventCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/dto/EventCreateRequest.java) | @NotBlank 注解 |
| [CallbackCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/callback/dto/CallbackCreateRequest.java) | @NotBlank 注解 |
| [PermissionCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/api/dto/PermissionCreateRequest.java) | @Pattern 注解 |
| [PermissionDto.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/event/dto/PermissionDto.java) | @Pattern 注解 |
| [CategoryCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/category/dto/CategoryCreateRequest.java) | @NotBlank 注解 |
| [CategoryOwnerRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/category/dto/CategoryOwnerRequest.java) | @NotBlank 注解 |
| [ApprovalFlowCreateRequest.java](file:///d:/myProject/open-app/open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/dto/ApprovalFlowCreateRequest.java) | 无校验 ⚠️ |

### 前端校验相关文件

| 文件 | 校验类型 |
|------|---------|
| [constants.js](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/constants.js) | Scope, Topic, Path 正则 |
| [ResourceRegister.jsx](file:///d:/myProject/open-app/wecodesite/src/components/ResourceRegister/ResourceRegister.jsx) | nameEn, scope 正则 |
| [ApiRegister.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Admin/Api/ApiRegister.jsx) | nameEn, scope 正则 |
| [CategoryFormModal.jsx](file:///d:/myProject/open-app/wecodesite/src/components/CategoryFormModal/CategoryFormModal.jsx) | required 校验 |
| [CategoryOwnerModal.jsx](file:///d:/myProject/open-app/wecodesite/src/components/CategoryOwnerModal/CategoryOwnerModal.jsx) | required 校验 |
| [ApprovalFlowFormModal.jsx](file:///d:/myProject/open-app/wecodesite/src/components/ApprovalFlowFormModal/ApprovalFlowFormModal.jsx) | code 正则 |
| [CallbackConfigDrawer.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Callbacks/CallbackConfigDrawer.jsx) | URL 正则 |
| [EventSubscriptionDrawer.jsx](file:///d:/myProject/open-app/wecodesite/src/pages/Events/EventSubscriptionDrawer.jsx) | URL 正则 |

---

*文档生成时间：2026-05-18*
*分析版本：v1.1（完整版）*