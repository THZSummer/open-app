# 应用上下文解析器设计方案

> **版本**: 1.0.0  
> **创建时间**: 2026-04-27  
> **状态**: 设计中

---

## 📋 目录

- [1. 概述](#1-概述)
- [2. 核心设计](#2-核心设计)
- [3. 接口定义](#3-接口定义)
- [4. 实现方案](#4-实现方案)
- [5. 使用示例](#5-使用示例)
- [6. 异常处理](#6-异常处理)
- [7. 相关文档](#7-相关文档)

---

## 1. 概述

### 1.1 背景说明

应用管理模块已有双 ID 设计：

| ID 类型 | 数据类型 | 说明 | 使用场景 |
|---------|----------|------|----------|
| **业务 ID** | `String` | 长ID，如 `app_20231201_abc123xyz` | 对外接口入参/出参、前端展示 |
| **主键 ID** | `Long` | 短ID，如 `1001` | 数据库存储、内部查询 |

### 1.2 问题分析

当前 open-server 直接使用 `appId` 参数，存在以下问题：

1. **类型不匹配**：接口入参是 String 类型长ID，数据库存储是 Long 类型短ID
2. **缺少权限校验**：未校验当前用户是否有权限访问该应用
3. **转换逻辑分散**：若在每个接口单独处理，代码冗余且难以维护

### 1.3 解决思路

在 Service 层统一处理：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          appId 处理流程                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   【Controller 层】                                                     │
│   ┌─────────────────────────────────────────┐                           │
│   │  入参: String appId (长业务ID)           │                          │
│   │  示例: "app_20231201_abc123xyz"         │                          │
│   └─────────────────────────────────────────┘                           │
│                        │                                                │
│                        ▼                                                │
│   【Service 层】                                                         │
│   ┌─────────────────────────────────────────┐                           │
│   │  AppContextResolver.resolveAndValidate │                           │
│  ┌└─────────────────────────────────────────┘                          ┌┐ │
│  │                                                                       ││
│  │  ① 长ID → 短ID 转换                                                   ││
│  │  ② 校验用户对该应用的访问权限                                          ││
│  │  ③ 返回应用上下文信息                                                 ││
│  │                                                                       ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                        │                                                │
│                        ▼                                                │
│   ┌─────────────────────────────────────────┐                           │
│   │  AppContext                              │                          │
│   │  - internalId: Long (短主键ID)          │                          │
│   │  - externalId: String (长业务ID)        │                          │
│   │  - 其他上下文信息...                     │                          │
│   └─────────────────────────────────────────┘                           │
│                        │                                                │
│                        ▼                                                │
│   【Mapper/数据库层】                                                    │
│   ┌─────────────────────────────────────────┐                           │
│   │  查询条件: app_id = #{internalId}        │                          │
│   │  示例: 1001                             │                          │
│   └─────────────────────────────────────────┘                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 核心设计

### 2.1 设计原则

| 原则 | 说明 |
|------|------|
| **Service 层处理** | 在 Service 层入口统一转换和校验，Controller 层保持无感知 |
| **策略模式** | 提供默认实现（原样返回/简单校验），标准环境注入真实实现 |
| **一次调用** | 解析和校验合并为一次调用，避免多次查询 |
| **异常快速失败** | 无效 ID 或无权限时抛出业务异常

### 2.2 核心接口职责

`AppContextResolver` 负责两件事：

1. **ID 转换**：外部长ID → 内部短ID
2. **权限校验**：当前用户是否有权访问该应用

### 2.3 返回数据结构

```java
public class AppContext {
    /**
     * 内部主键ID（用于数据库查询）
     */
    private Long internalId;
    
    /**
     * 外部业务ID（用于响应返回）
     */
    private String externalId;
    
    // 可扩展其他上下文信息
}
```

---

## 3. 接口定义

### 3.1 核心接口

```java
package com.xxx.it.works.wecode.v2.modules.app.resolver;

/**
 * 应用上下文解析器
 * 
 * <p>职责：</p>
 * <ul>
 *   <li>1. 外部长ID转换为内部短ID</li>
 *   <li>2. 校验当前用户对该应用的访问权限</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface AppContextResolver {

    /**
     * 解析并校验应用访问权限
     * 
     * @param externalAppId 外部业务ID（长ID）
     * @return 应用上下文信息
     * @throws BusinessException 当ID无效或无访问权限时抛出
     */
    AppContext resolveAndValidate(String externalAppId);
    
    /**
     * 将内部ID转换为外部ID
     * 
     * @param internalId 内部主键ID（短ID）
     * @return 外部业务ID（长ID）
     */
    String toExternalId(Long internalId);
}
```

### 3.2 上下文对象

```java
package com.xxx.it.works.wecode.v2.modules.app.resolver;

import lombok.Builder;
import lombok.Data;

/**
 * 应用上下文
 * 
 * <p>包含解析后的应用信息，用于业务逻辑处理</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
public class AppContext {
    
    /**
     * 内部主键ID（用于数据库查询）
     */
    private Long internalId;
    
    /**
     * 外部业务ID（用于响应返回）
     */
    private String externalId;
    
    // 未来可扩展：
    // - 应用名称
    // - 应用类型
    // - 其他业务属性
}
```

### 3.3 异常定义

```java
package com.xxx.it.works.wecode.v2.common.exception;

/**
 * 应用访问异常
 */
public class AppAccessException extends BusinessException {
    
    /**
     * 应用不存在
     */
    public static AppAccessException notFound(String appId) {
        return new AppAccessException("APP_NOT_FOUND", 
            String.format("应用不存在: %s", appId));
    }
    
    /**
     * 无访问权限
     */
    public static AppAccessException noPermission(String appId) {
        return new AppAccessException("APP_NO_PERMISSION", 
            String.format("无权访问应用: %s", appId));
    }
}
```

---

## 4. 实现方案

### 4.1 开发环境实现（默认）

```java
package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import org.springframework.stereotype.Component;

/**
 * 开发环境应用上下文解析器
 * 
 * <p>默认实现，用于开发和测试环境：</p>
 * <ul>
 *   <li>ID 转换：直接将 String 解析为 Long（或原样返回兼容测试）</li>
 *   <li>权限校验：跳过校验，允许所有访问</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Component
public class DevAppContextResolver implements AppContextResolver {

    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        // 开发环境：简单解析，不做权限校验
        Long internalId = parseInternalId(externalAppId);
        
        return AppContext.builder()
            .internalId(internalId)
            .externalId(externalAppId)
            .build();
    }

    @Override
    public String toExternalId(Long internalId) {
        // 开发环境：直接返回 String
        return String.valueOf(internalId);
    }
    
    /**
     * 解析内部ID
     * 支持两种格式：
     * 1. 纯数字字符串 "1001" → 1001L
     * 2. 长业务ID，提取末尾数字或使用默认值
     */
    private Long parseInternalId(String externalAppId) {
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }
        
        // 尝试直接解析为数字
        try {
            return Long.parseLong(externalAppId);
        } catch (NumberFormatException e) {
            // 开发环境：非数字ID时返回默认值 1L，方便测试
            return 1L;
        }
    }
}
```

### 4.2 标准环境实现（预留）

```java
package com.xxx.it.works.wecode.v2.modules.app.resolver.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 标准环境应用上下文解析器
 * 
 * <p>生产环境实现：</p>
 * <ul>
 *   <li>ID 转换：调用应用管理服务获取映射关系</li>
 *   <li>权限校验：校验当前用户对该应用的访问权限</li>
 * </ul>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
// @Component  // 标准环境启用，开发环境注释
@ConditionalOnProperty(name = "app.resolver.type", havingValue = "standard")
public class StandardAppContextResolver implements AppContextResolver {

    // @Autowired
    // private AppManageService appManageService;  // 已有的应用管理服务
    
    @Override
    public AppContext resolveAndValidate(String externalAppId) {
        // 1. 校验参数
        if (externalAppId == null || externalAppId.isEmpty()) {
            throw AppAccessException.notFound(externalAppId);
        }
        
        // 2. 调用应用管理服务获取内部ID
        // Long internalId = appManageService.getInternalIdByExternalId(externalAppId);
        // if (internalId == null) {
        //     throw AppAccessException.notFound(externalAppId);
        // }
        
        // 3. 校验当前用户对该应用的访问权限
        // String currentUserId = UserContextHolder.getUserId();
        // boolean hasPermission = appManageService.checkUserAppPermission(
        //     currentUserId, internalId);
        // if (!hasPermission) {
        //     throw AppAccessException.noPermission(externalAppId);
        // }
        
        // TODO: 标准环境实现，对接应用管理服务
        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet");
    }

    @Override
    public String toExternalId(Long internalId) {
        // TODO: 标准环境实现
        // return appManageService.getExternalIdByInternalId(internalId);
        throw new UnsupportedOperationException(
            "StandardAppContextResolver not implemented yet");
    }
}
```

### 4.3 配置切换

```yaml
# application-dev.yml (开发环境)
app:
  resolver:
    type: dev  # 使用 DevAppContextResolver

# application-prod.yml (标准环境)
app:
  resolver:
    type: standard  # 使用 StandardAppContextResolver
```

---

## 5. 使用示例

### 5.1 Service 层使用

```java
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AppContextResolver appContextResolver;
    private final SubscriptionMapper subscriptionMapper;
    
    /**
     * 获取应用 API 权限列表
     */
    public ApiResponse<List<ApiSubscriptionListResponse>> getApiSubscriptionList(
            String appId, Integer status, String keyword, 
            Integer curPage, Integer pageSize) {
        
        // 1. 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        Long internalId = appContext.getInternalId();
        
        // 2. 使用内部ID查询数据库
        List<Subscription> subscriptions = subscriptionMapper
            .selectApiSubscriptionsByAppId(internalId, status, keyword, offset, size);
        
        // 3. 构建响应（使用外部ID）
        return buildResponse(subscriptions, appContext.getExternalId());
    }
    
    /**
     * 申请 API 权限
     */
    public PermissionSubscribeResponse subscribeApiPermissions(
            String appId, PermissionSubscribeRequest request) {
        
        // 1. 解析并校验应用访问权限
        AppContext appContext = appContextResolver.resolveAndValidate(appId);
        
        // 2. 构建订阅记录
        Subscription subscription = new Subscription();
        subscription.setAppId(appContext.getInternalId());  // 存储内部ID
        // ... 其他字段
        
        // 3. 保存
        subscriptionMapper.insert(subscription);
        
        // 4. 构建响应
        return PermissionSubscribeResponse.builder()
            .appId(appContext.getExternalId())  // 返回外部ID
            .build();
    }
}
```

### 5.2 涉及修改的 Service 方法

| 方法 | 当前代码 | 修改内容 |
|------|----------|----------|
| `getApiSubscriptionList` | `parseId(appId)` | 改用 `appContextResolver.resolveAndValidate(appId).getInternalId()` |
| `getCategoryApiPermissions` | `parseId(appId)` | 同上 |
| `subscribeApiPermissions` | `parseId(appId)` | 同上 |
| `withdrawApiSubscription` | `parseId(appId)` | 同上 |
| `getEventSubscriptionList` | `parseId(appId)` | 同上 |
| `getCategoryEventPermissions` | `parseId(appId)` | 同上 |
| `subscribeEventPermissions` | `parseId(appId)` | 同上 |
| `configEventSubscription` | `parseId(appId)` | 同上 |
| `withdrawEventSubscription` | `parseId(appId)` | 同上 |
| `getCallbackSubscriptionList` | `parseId(appId)` | 同上 |
| `getCategoryCallbackPermissions` | `parseId(appId)` | 同上 |
| `subscribeCallbackPermissions` | `parseId(appId)` | 同上 |
| `configCallbackSubscription` | `parseId(appId)` | 同上 |
| `withdrawCallbackSubscription` | `parseId(appId)` | 同上 |

### 5.3 响应构建修改

```java
// 原代码
private ApiSubscriptionListResponse buildResponse(Subscription subscription) {
    return ApiSubscriptionListResponse.builder()
        .appId(String.valueOf(subscription.getAppId()))  // 直接转 String
        .build();
}

// 新代码
private ApiSubscriptionListResponse buildResponse(
        Subscription subscription, AppContext appContext) {
    return ApiSubscriptionListResponse.builder()
        .appId(appContext.getExternalId())  // 使用转换器
        .build();
}
```

---

## 6. 异常处理

### 6.1 异常场景

| 场景 | 异常类型 | 错误码 | 错误信息 |
|------|----------|--------|----------|
| appId 为空 | `AppAccessException` | `APP_NOT_FOUND` | 应用不存在: null |
| appId 无效（不存在） | `AppAccessException` | `APP_NOT_FOUND` | 应用不存在: xxx |
| 用户无访问权限 | `AppAccessException` | `APP_NO_PERMISSION` | 无权访问应用: xxx |

### 6.2 全局异常处理

```java
@ExceptionHandler(AppAccessException.class)
public ResponseEntity<ApiResponse<Void>> handleAppAccessException(
        AppAccessException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
}
```

---

## 7. 相关文档

- [plan-api.md](./plan-api.md) - 接口设计
- [plan-flow.md](./plan-flow.md) - 审批流程设计
- [plan-config-dev.md](./plan-config-dev.md) - 开发环境配置

---

## 📝 变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0.0 | 2026-04-27 | SDDU | 初始版本 |
