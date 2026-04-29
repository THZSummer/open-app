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

**格式要求**：注释前面如果有代码，必须有空行分隔。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 代码与注释之间有空行 | 代码与注释之间无空行 |

```java
// ✅ 正确示例
public void process() {
    int status = getStatus();
    
    // 处理状态逻辑
    if (status == 1) {
        doSomething();
    }
}

// ❌ 错误示例
public void process() {
    int status = getStatus();
    // 处理状态逻辑
    if (status == 1) {
        doSomething();
    }
}
```

**原因**：
- 提高代码可读性
- 清晰区分代码块和注释
- 符合主流编码规范（Google、阿里巴巴等）

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

## 8. 变量声明规范

**规则**：每行只能声明一个变量，禁止在一行声明多个变量。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 每行一个变量声明 | 一行声明多个变量 |

```java
// ✅ 正确示例 - 每行一个变量
int success = 0;
int failed = 0;
int skipped = 0;

String name = "test";
String scope = "api:test";

// ❌ 错误示例 - 一行声明多个变量
int success = 0, failed = 0, skipped = 0;

String name = "test", scope = "api:test";
```

**原因**：
- 提高代码可读性，每个变量独立一行
- 方便添加注释说明每个变量的用途
- 便于代码审查和调试
- 符合 Java 编码最佳实践

---

## 9. 大括号规范

**规则**：条件语句（if/else）和循环语句（for/while）必须使用大括号，即使只有一行代码。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用大括号包裹代码块 | 省略大括号（即使只有一行） |

```java
// ✅ 正确示例 - 使用大括号
if (status == null) {
    return 0;
}

if (count > 0) {
    log.info("Found {} records", count);
}

for (String item : items) {
    processItem(item);
}

while (hasNext()) {
    fetchNext();
}

// ❌ 错误示例 - 省略大括号
if (status == null) return 0;

if (count > 0) log.info("Found {} records", count);

for (String item : items) processItem(item);

while (hasNext()) fetchNext();
```

**原因**：
- 避免因添加代码时忘记加括号导致的逻辑错误
- 提高代码可读性和一致性
- 防止"苹果公司 SSL/TLS 重大漏洞"类问题（因缺少括号导致的逻辑错误）
- 符合主流 Java 编码规范（Google、Oracle、阿里巴巴等）

---

## 10. 字符串操作规范

**规则**：使用 `toLowerCase()` 和 `toUpperCase()` 必须指定 `Locale.ROOT`，避免不同地区的转换差异。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 指定 Locale.ROOT | 不指定 Locale |

```java
// ✅ 正确示例
String normalized = input.toLowerCase(Locale.ROOT);
String upperValue = code.toUpperCase(Locale.ROOT);

if (type.toLowerCase(Locale.ROOT).equals("api")) {
    // 处理 API 类型
}

// ❌ 错误示例
String normalized = input.toLowerCase();  // 使用默认 Locale，可能导致不一致
String upperValue = code.toUpperCase();   // 在土耳其环境下 'i' → 'İ' 而非 'I'

if (type.toLowerCase().equals("api")) {
    // 在某些 Locale 下可能不匹配
}
```

**原因**：
- 不同地区的字母转换规则不同（如土耳其语 'i' → 'İ'）
- 不指定 Locale 可能导致字符串匹配失败
- 确保在不同服务器环境下转换结果一致
- 与规则 #4（String.format）同理，避免国际化问题

**典型案例**：
- 某系统在土耳其服务器上，`"title".toLowerCase()` → `"tıtle"`（非 `"title"`）
- 导致字符串匹配失败，功能异常

---

## 11. 空代码块规范

**规则**：禁止空代码块，所有代码块（if/else/for/while/try/catch 等）必须有实际代码。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 代码块内有实际逻辑或日志 | 只有注释的空代码块 |

```java
// ✅ 正确示例
if (status == null) {
    log.warn("Status is null, using default value");
    status = 0;
}

try {
    processItem(item);
} catch (Exception e) {
    log.error("Failed to process item: {}", item, e);
}

// ❌ 错误示例
if (status == null) {
    // 忽略
}

try {
    processItem(item);
} catch (Exception e) {
    // 忽略异常
}

else if (type.equals("event")) {
    // 类似逻辑
}
```

**原因**：
- 空代码块通常表示未完成的功能
- 空的 catch 块会吞掉异常，导致问题难以排查
- 降低代码可维护性

**建议处理方式**：
- 空 if/else：添加日志记录或抛出异常
- 空 catch：至少添加日志记录异常信息
- 未实现功能：添加 `throw new UnsupportedOperationException("TODO: implement")`

---

## 12. 行尾空格规范

**规则**：禁止代码行尾有多余空格（Trailing Whitespace）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 行尾无多余空格 | 行尾有 1 个或多个空格 |

```java
// ✅ 正确示例
String name = "test";
log.info("Processing item: {}", itemId);

// ❌ 错误示例（注意行尾空格）
String name = "test";    
log.info("Processing item: {}", itemId);   
```

**原因**：
- 多余空格增加代码体积，无实际意义
- 在 Git diff 中显示为无意义的修改
- 不同编辑器可能显示不一致
- 影响代码整洁度

**IDE 配置**：
- IntelliJ IDEA: Settings → Editor → General → On Save → Remove trailing spaces
- VS Code: Settings → Files: Trim Trailing Whitespace
- 可配置保存时自动删除行尾空格

---

## 13. Shell 脚本规范

**规则**：Shell 脚本必须以 `#!/bin/bash` 和 `set -ex` 开头，确保异常退出和调试输出。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 `set -ex` | 缺少 `set -ex` |

```bash
#!/bin/bash
set -ex

# 脚本内容
echo "Starting deployment"
./deploy.sh

# ❌ 错误示例
#!/bin/bash

# 缺少 set -ex，脚本出错时会继续执行
echo "Starting deployment"
./deploy.sh
```

**`set -ex` 参数说明**：
- `set -e`：脚本中任何命令返回非零状态码时立即退出，防止错误累积
- `set -x`：在执行命令前打印命令，便于调试和日志追踪

**原因**：
- 避免脚本在错误状态下继续执行
- 提供调试信息，便于排查问题
- 符合 Shell 脚本最佳实践
- 提高脚本可靠性

**完整示例**：
```bash
#!/bin/bash
set -ex

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# 执行主要逻辑
main() {
    echo "Starting process..."
    # 业务逻辑
}

main "$@"
```

---

## 总结

以上 13 条规范为能力开放平台项目的**强制要求**，所有代码提交前必须确保符合规范。

请在 IDE 中配置相应的代码格式化和检查规则，确保代码风格统一。
