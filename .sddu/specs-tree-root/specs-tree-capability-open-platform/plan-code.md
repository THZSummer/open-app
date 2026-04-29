# 代码规范

> 本文档为 `plan.md` 的子文档，定义能力开放平台的核心代码规范。

---

## 1. 注释规范

**规则**：代码注释统一使用中文。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `// 获取用户权限列表` | `// Get user permission list` |
| `// 校验应用是否有权限` | `// Validate app permission` |
| `// 创建审批流程` | `// Create approval flow` |

```java
// ✅ 正确示例
/**
 * 查询应用订阅的权限列表
 * 
 * @param appId 应用ID
 * @return 权限列表
 */
public List<Permission> getSubscribedPermissions(String appId) {
    // 查询应用已订阅的权限
    return permissionRepository.findByAppId(appId);
}

// ❌ 错误示例
/**
 * Get subscribed permission list by app ID
 */
public List<Permission> getSubscribedPermissions(String appId) {
    // Query permissions subscribed by app
    return permissionRepository.findByAppId(appId);
}
```

---

## 2. 日志规范

**规则**：日志信息统一使用英文。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `log.info("Permission created, id={}", id)` | `log.info("权限创建成功, id={}", id)` |
| `log.error("Failed to subscribe permission")` | `log.error("订阅权限失败")` |
| `log.warn("User not found, userId={}", userId)` | `log.warn("用户不存在, userId={}", userId)` |

```java
// ✅ 正确示例
@Slf4j
@Service
public class PermissionService {
    public void createPermission(Permission permission) {
        log.info("Creating permission, scope={}", permission.getScope());
        permissionRepository.save(permission);
        log.info("Permission created successfully, id={}", permission.getId());
    }
}

// ❌ 错误示例
@Slf4j
@Service
public class PermissionService {
    public void createPermission(Permission permission) {
        log.info("创建权限, scope={}", permission.getScope());
        permissionRepository.save(permission);
        log.info("权限创建成功, id={}", permission.getId());
    }
}
```

---

## 3. SQL 查询规范

**规则**：禁止使用 `SELECT *`，必须明确指定字段名。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `SELECT id, name, scope FROM permission` | `SELECT * FROM permission` |
| `SELECT id, app_id, status FROM subscription` | `SELECT * FROM subscription` |

```xml
<!-- ✅ 正确示例 - MyBatis Mapper -->
<select id="findByCategoryId" resultType="Permission">
    SELECT id, name_cn, name_en, scope, status, create_time
    FROM openplatform_permission_t
    WHERE category_id = #{categoryId}
</select>

<!-- ❌ 错误示例 -->
<select id="findByCategoryId" resultType="Permission">
    SELECT *
    FROM openplatform_permission_t
    WHERE category_id = #{categoryId}
</select>
```

**原因**：
- 避免查询不需要的字段，影响性能
- 表结构变更时不会影响现有查询
- 明确字段列表，提高代码可读性

---

## 4. 字符串格式化规范

**规则**：使用 `String.format()` 必须指定 `Locale.ROOT`，避免不同环境下的格式化差异。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `String.format(Locale.ROOT, "id=%d", id)` | `String.format("id=%d", id)` |
| `String.format(Locale.ROOT, "%.2f", value)` | `String.format("%.2f", value)` |

```java
// ✅ 正确示例
public String buildCacheKey(String appId, Long permissionId) {
    return String.format(Locale.ROOT, "permission:%s:%d", appId, permissionId);
}

public String formatScope(String type, String module, String resource) {
    return String.format(Locale.ROOT, "%s:%s:%s", type, module, resource);
}

// ❌ 错误示例
public String buildCacheKey(String appId, Long permissionId) {
    return String.format("permission:%s:%d", appId, permissionId);
}
```

**原因**：
- 不同地区的数字格式不同（如千分位分隔符）
- 指定 `Locale.ROOT` 确保格式化结果一致性
- 避免在不同服务器环境下产生不同结果

---

## 5. 模糊查询规范

**规则**：模糊查询禁止使用 `%` 开头，避免索引失效。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `WHERE name LIKE 'api%'` | `WHERE name LIKE '%api%'` |
| `WHERE scope LIKE 'permission:%'` | `WHERE scope LIKE '%permission%'` |

```xml
<!-- ✅ 正确示例 - 前缀匹配 -->
<select id="searchByName" resultType="Permission">
    SELECT id, name_cn, scope
    FROM openplatform_permission_t
    WHERE name_cn LIKE CONCAT(#{keyword}, '%')
</select>

<!-- ❌ 错误示例 - 全模糊匹配 -->
<select id="searchByName" resultType="Permission">
    SELECT id, name_cn, scope
    FROM openplatform_permission_t
    WHERE name_cn LIKE CONCAT('%', #{keyword}, '%')
</select>
```

**原因**：
- `LIKE '%keyword%'` 无法使用索引，导致全表扫描
- `LIKE 'keyword%'` 可以使用索引（前缀匹配）
- 如需全模糊查询，应使用全文索引或搜索引擎

---

## 6. Switch 语句规范

**规则**：`switch` 语句必须有 `default` 分支，处理未知情况。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 default 分支 | 缺少 default 分支 |

```java
// ✅ 正确示例
public String getStatusText(Integer status) {
    switch (status) {
        case 0:
            return "待审批";
        case 1:
            return "已通过";
        case 2:
            return "已驳回";
        case 3:
            return "已撤销";
        default:
            log.warn("Unknown status: {}", status);
            return "未知状态";
    }
}

// ❌ 错误示例
public String getStatusText(Integer status) {
    switch (status) {
        case 0:
            return "待审批";
        case 1:
            return "已通过";
        case 2:
            return "已驳回";
        case 3:
            return "已撤销";
    }
    return "未知状态";  // 应该在 default 中处理
}
```

**原因**：
- 处理新增的枚举值或状态值
- 提供兜底逻辑，增强代码健壮性
- 明确表达"未知情况"的处理方式

---

## 7. 缩进规范

**规则**：Java 代码必须使用 **4 个空格**缩进，禁止使用 Tab。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 4 个空格缩进 | 2 个空格或 Tab 缩进 |

```java
// ✅ 正确示例 - 4 个空格
public class PermissionService {
    public void createPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("权限不能为空");
        }
        
        // 设置默认值
        if (permission.getStatus() == null) {
            permission.setStatus(0);
        }
        
        permissionRepository.save(permission);
    }
}

// ❌ 错误示例 - 2 个空格或 Tab
public class PermissionService {
  public void createPermission(Permission permission) {
    if (permission == null) {
      throw new IllegalArgumentException("权限不能为空");
    }
  }
}
```

**配置 IDE**：
- IntelliJ IDEA: Settings → Editor → Code Style → Java → Tab size: 4
- Eclipse: Preferences → Java → Code Style → Formatter → Indentation
- VS Code: Settings → Editor: Tab Size: 4

---

## 总结

以上 7 条规范为能力开放平台项目的**强制要求**，所有代码提交前必须确保符合规范。

请在 IDE 中配置相应的代码格式化和检查规则，确保代码风格统一。
