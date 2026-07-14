# 代码规范

> 本文档为 `plan.md` 的子文档，定义连接器平台的核心代码规范。V3 沿用 V1 全部规范。
>
> 以下规范**沿用**能力开放平台（`specs-tree-capability-open-platform/plan-code.md`）已确立的技术标准，确保全项目代码风格统一。

---

> 💡 **说明**：本规范及以下 16 条规则来源于能力开放平台（CAP-OPEN-001）已确立的技术标准，连接器平台作为开放平台体系的一部分，保持一致的技术规范。已在能力开放平台项目中验证过的规范直接沿用，不做重复决策。

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
public class ConnectorService {
    public void createConnector(Connector connector) {
        log.info("Creating connector, name={}", connector.getName());
        connectorRepository.save(connector);
        log.info("Connector created successfully, id={}", connector.getConnectorId());
    }
}

// ❌ 错误示例
@Slf4j
@Service
public class ConnectorService {
    public void createConnector(Connector connector) {
        log.info("创建连接器, name={}", connector.getName());
        connectorRepository.save(connector);
        log.info("连接器创建成功, id={}", connector.getConnectorId());
    }
}
```

---

## 3. SQL 查询规范

**规则**：禁止使用 `SELECT *`，必须明确指定字段名。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `SELECT id, name, type FROM cp_connector` | `SELECT * FROM cp_connector` |
| `SELECT execution_id, status FROM cp_execution_record` | `SELECT * FROM cp_execution_record` |

```xml
<!-- ✅ 正确示例 - MyBatis Mapper -->
<select id="findByAppId" resultType="Connector">
    SELECT connector_id, name, connector_type, visibility, status
    FROM cp_connector
    WHERE creator_app_id = #{appId}
</select>

<!-- ❌ 错误示例 -->
<select id="findByAppId" resultType="Connector">
    SELECT *
    FROM cp_connector
    WHERE creator_app_id = #{appId}
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
public String buildCacheKey(String connectorId, String versionNo) {
    return String.format(Locale.ROOT, "connector:%s:%s", connectorId, versionNo);
}

public String formatExecutionKey(String executionId) {
    return String.format(Locale.ROOT, "execution:%s", executionId);
}

// ❌ 错误示例
public String buildCacheKey(String connectorId, String versionNo) {
    return String.format("connector:%s:%s", connectorId, versionNo);
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
| `WHERE name LIKE 'IM%'` | `WHERE name LIKE '%IM%'` |
| `WHERE flow_id LIKE 'flow:%'` | `WHERE flow_id LIKE '%flow%'` |

```xml
<!-- ✅ 正确示例 - 前缀匹配 -->
<select id="searchByName" resultType="Connector">
    SELECT connector_id, name, connector_type
    FROM cp_connector
    WHERE name LIKE CONCAT(#{keyword}, '%')
</select>

<!-- ❌ 错误示例 - 全模糊匹配 -->
<select id="searchByName" resultType="Connector">
    SELECT connector_id, name, connector_type
    FROM cp_connector
    WHERE name LIKE CONCAT('%', #{keyword}, '%')
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
public String getExecutionStatusText(String status) {
    switch (status) {
        case "pending":
            return "待执行";
        case "running":
            return "执行中";
        case "success":
            return "执行成功";
        case "failed":
            return "执行失败";
        case "timeout":
            return "已超时";
        default:
            log.warn("Unknown execution status: {}", status);
            return "未知状态";
    }
}

// ❌ 错误示例
public String getExecutionStatusText(String status) {
    switch (status) {
        case "pending":
            return "待执行";
        case "running":
            return "执行中";
        case "success":
            return "执行成功";
        case "failed":
            return "执行失败";
        case "timeout":
            return "已超时";
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
public class ConnectorService {
    public void createConnector(Connector connector) {
        if (connector == null) {
            throw new IllegalArgumentException("连接器不能为空");
        }
        
        // 设置默认值
        if (connector.getStatus() == null) {
            connector.setStatus("active");
        }
        
        connectorRepository.save(connector);
    }
}

// ❌ 错误示例 - 2 个空格或 Tab
public class ConnectorService {
  public void createConnector(Connector connector) {
    if (connector == null) {
      throw new IllegalArgumentException("连接器不能为空");
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
int successCount = 0;
int failedCount = 0;
int timeoutCount = 0;

String connectorName = "IM 发送消息";
String connectorType = "HTTP";

// ❌ 错误示例 - 一行声明多个变量
int successCount = 0, failedCount = 0, timeoutCount = 0;

String connectorName = "IM 发送消息", connectorType = "HTTP";
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
if (execution == null) {
    return null;
}

if (retryCount > 0) {
    log.info("Retrying execution, attempt={}", retryCount);
}

for (ExecutionStep step : steps) {
    processStep(step);
}

while (hasNextNode()) {
    executeNext();
}

// ❌ 错误示例 - 省略大括号
if (execution == null) return null;

if (retryCount > 0) log.info("Retrying execution, attempt={}", retryCount);

for (ExecutionStep step : steps) processStep(step);

while (hasNextNode()) executeNext();
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
String upperValue = connectorType.toUpperCase(Locale.ROOT);

if (nodeType.toLowerCase(Locale.ROOT).equals("connector")) {
    // 处理连接器节点
}

// ❌ 错误示例
String normalized = input.toLowerCase();  // 使用默认 Locale，可能导致不一致
String upperValue = connectorType.toUpperCase();   // 在土耳其环境下 'i' → 'İ' 而非 'I'

if (nodeType.toLowerCase().equals("connector")) {
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

## 11. 包装类型值比较规范

**规则**：`Integer`、`Long`、`Boolean` 等包装类型**禁止使用 `==` / `!=` 比较**，必须使用 `.equals()` 或 `Objects.equals()` 进行值比较。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `.equals()` 值比较 | `==` / `!=` 引用比较 |

```java
// ✅ 正确示例 — 用 .equals() 做值比较
if (cv.getStatus() == null
        || !cv.getStatus().equals(ConnectorVersionStatus.PUBLISHED.getCode())) {
    return ApiResponse.error("422", "引用的连接器版本已失效", "...");
}

if (v.getStatus() != null
        && v.getStatus().equals(ConnectorVersionStatus.DRAFT.getCode())) {
    // 草稿版本
}

// ✅ 也可用 Objects.equals() 处理 null 安全
if (!Objects.equals(cv.getStatus(), ConnectorVersionStatus.PUBLISHED.getCode())) {
    return ApiResponse.error("422", "引用的连接器版本已失效", "...");
}

// ❌ 错误示例 — ==/!= 是引用比较，值 > 127 时永远出错
if (cv.getStatus() != ConnectorVersionStatus.PUBLISHED.getCode()) {
    return ApiResponse.error("422", "...", "...");
}

if (v.getStatus() == ConnectorVersionStatus.DRAFT.getCode()) {
    // 值 > 127 时永远为 false！
}
```

**原因**：
- Java 的 `Integer`/`Long` 是对象类型，`==`/`!=` 比较的是**引用地址**而非值
- JVM 对 -128~127 的 `Integer` 有缓存池，该范围内 `==` 碰巧能工作；超出此范围（如状态码 > 127）则必然失败
- 项目中 `status` 等字段定义为 `private Integer`，`getCode()` 也返回 `Integer`，必须用 `.equals()` 做值比较
- 经典案例：某系统状态码定义为 200，本地测试 `==` 正常；线上某个实例 Integer 缓存失效后状态永远不匹配，故障定位数小时

**与规则 10（Locale.ROOT）的关系**：
- 两者都是 Java 常见陷阱 — 规则 10 防范国际化 Locale 差异，本条防范装箱类型比较陷阱
- 共同原则：永远不依赖 JVM 的隐式优化（字符串大小写转换的默认 Locale、Integer 缓存池）

---

## 12. 空代码块规范

**规则**：禁止空代码块，所有代码块（if/else/for/while/try/catch 等）必须有实际代码。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 代码块内有实际逻辑或日志 | 只有注释的空代码块 |

```java
// ✅ 正确示例
if (executionStatus == null) {
    log.warn("Execution status is null, using default");
    executionStatus = "pending";
}

try {
    executeNode(node);
} catch (Exception e) {
    log.error("Failed to execute node: {}", node.getNodeId(), e);
    throw e;
}

// ❌ 错误示例
if (executionStatus == null) {
    // 忽略
}

try {
    executeNode(node);
} catch (Exception e) {
    // 忽略异常
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

## 13. 注释块换行规范

**规则**：代码块内的注释必须独立成行并缩进，禁止注释与花括号挤在同一行。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 注释独立缩进，花括号换行 | 注释与花括号同行 |

```java
// ✅ 正确示例 — switch case 显式允许分支
case "script" -> {
    // 串行模式允许脚本节点
}

case "connector", "script" -> {
    // 并行模式允许连接器和脚本节点
}

if (nodes == null || !nodes.isArray()) {
    // 无节点时跳过校验
    return;
}

// ❌ 错误示例 — 注释与花括号同行
case "script" -> { /* 允许 */ }

case "connector", "script" -> { /* 允许 */ }

if (nodes == null || !nodes.isArray()) { /* 空节点跳过 */ return; }
```

**原因**：
- 注释挤在花括号之间可读性差，在 diff 和代码审查中容易被忽略
- 独立成行的注释在 git diff 中能清晰展示意图变更
- 保持代码块格式一致性 — 花括号各占一行是基本代码风格

**与规则 11（空代码块）的关系**：
- 规则 11 禁止"只有注释的空代码块" — switch case 中显式允许某类型通过时，应至少包含一条语义明确的语句
- 本条进一步要求：当注释与语句共同存在于代码块中时，注释也必须独立成行

---

## 14. 行尾空格规范

**规则**：禁止代码行尾有多余空格（Trailing Whitespace）。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 行尾无多余空格 | 行尾有 1 个或多个空格 |

```java
// ✅ 正确示例
String connectorId = "con_xxxxx";
log.info("Creating connector: {}", connectorId);

// ❌ 错误示例（注意行尾空格）
String connectorId = "con_xxxxx";    
log.info("Creating connector: {}", connectorId);   
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

## 15. Shell 脚本规范

**规则**：Shell 脚本必须以 `#!/bin/bash` 和 `set -ex` 开头，确保异常退出和调试输出。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 `set -ex` | 缺少 `set -ex` |

```bash
#!/bin/bash
set -ex

# 脚本内容
echo "Starting connector platform deployment"
./deploy-connector.sh

# ❌ 错误示例
#!/bin/bash

# 缺少 set -ex，脚本出错时会继续执行
echo "Starting connector platform deployment"
./deploy-connector.sh
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

## 16. 圈复杂度规范

**规则**：方法圈复杂度必须维持在 15 以下，超过时必须重构拆分。

**圈复杂度计算**：
- 基础复杂度 = 1
- 每个 if/else 分支 +1
- 每个 case 分支 +1
- 每个 for/while 循环 +1
- 每个 catch 块 +1
- 每个 && 或 || 运算符 +1

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 圈复杂度 ≤ 15，逻辑清晰 | 圈复杂度 > 15，难以维护 |

```java
// ✅ 正确示例 - 圈复杂度 5
public void updateConnector(String id, ConnectorUpdateRequest request) {
    Connector connector = connectorMapper.selectByConnectorId(id);
    
    if (connector == null) {
        throw new BusinessException("连接器不存在", "Connector not found");
    }
    
    // 提取到独立方法，降低复杂度
    updateBasicFields(connector, request);
    updateVisibilityIfNeeded(connector, request);
    
    connectorMapper.update(connector);
}

private void updateBasicFields(Connector connector, ConnectorUpdateRequest request) {
    if (request.getName() != null) {
        connector.setName(request.getName());
    }
    if (request.getDescription() != null) {
        connector.setDescription(request.getDescription());
    }
}

// ❌ 错误示例 - 圈复杂度 20+
public void updateConnector(String id, ConnectorUpdateRequest request) {
    Connector connector = connectorMapper.selectByConnectorId(id);
    if (connector == null) { throw new BusinessException(...); }
    if (request.getName() != null && !request.getName().isEmpty()) {
        connector.setName(request.getName());
    }
    if (request.getDescription() != null && !request.getDescription().isEmpty()) {
        connector.setDescription(request.getDescription());
    }
    // ... 更多 if 检查，圈复杂度累积超过 15
}
```

**重构策略**：
1. **提取方法**：将复杂逻辑拆分为多个小方法
2. **早返回（Guard Clause）**：用早返回减少嵌套层级
3. **策略模式**：替代复杂的 switch-case 结构
4. **使用 Optional**：减少 null 检查的嵌套

**工具检测**：
- Maven: `mvn checkstyle:check` 配置 CyclomaticComplexity 规则
- IntelliJ IDEA: 安装 MetricsReloaded 插件查看方法复杂度
- PMD: 配置 `CyclomaticComplexity` 规则

**原因**：
- 圈复杂度过高导致代码难以理解和维护
- 测试覆盖难度增加，需要更多测试用例
- 代码审查和调试效率降低
- 符合业界最佳实践（Google、阿里巴巴规范建议 < 10）

---

## 17. 函数深度规范

**规则**：方法最大嵌套深度不得超过 5 层，超过时必须重构。

**嵌套深度计算**：
- 方法基础深度 = 0
- 每个 if/else 嵌套 +1
- 每个 for/while 循环嵌套 +1
- 每个 try-catch 嵌套 +1
- 每个 switch-case 嵌套 +1

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 嵌套深度 ≤ 5，层次清晰 | 嵌套深度 > 5，难以理解 |

```java
// ✅ 正确示例 - 最大深度 2
public void processFlowExecution(String executionId) {
    ExecutionRecord record = executionMapper.selectByExecutionId(executionId);
    
    if (record == null) {
        throw new BusinessException("执行记录不存在", "Execution not found");
    }
    
    // 提取方法，避免嵌套
    validateExecution(record);
    executeSteps(record);
    completeExecution(record);
}

private void executeSteps(ExecutionRecord record) {
    for (ExecutionStep step : record.getSteps()) {
        // 深度 = 1（for循环）
        if (step.getStatus().equals("pending")) {
            // 深度 = 2（if）
            executeStep(step);
        }
    }
}

// ❌ 错误示例 - 最大深度 6
public void processFlowExecution(String executionId) {
    ExecutionRecord record = executionMapper.selectByExecutionId(executionId);
    if (record != null) {                          // 深度 = 1
        if (record.getStatus().equals("running")) { // 深度 = 2
            for (ExecutionStep step : record.getSteps()) {  // 深度 = 3
                if (step.getStatus().equals("pending")) {   // 深度 = 4
                    if (step.getNodeType().equals("connector")) { // 深度 = 5
                        if (step.getRetryCount() < 3) {   // 深度 = 6 ❌
                            // 处理逻辑...
                        }
                    }
                }
            }
        }
    }
}
```

**重构策略**：
1. **早返回（Guard Clause）**：用 return 或 throw 提前退出，减少嵌套
2. **提取方法**：将嵌套逻辑拆分为独立方法
3. **使用 continue/break**：减少循环内的嵌套
4. **使用 Optional**：减少 null 检查的嵌套
5. **策略模式**：替代多层条件判断

```java
// 重构示例：使用早返回降低嵌套深度
public void processFlowExecution(String executionId) {
    ExecutionRecord record = executionMapper.selectByExecutionId(executionId);
    
    // ✅ 早返回，避免嵌套
    if (record == null) {
        throw new BusinessException("执行记录不存在", "Execution not found");
    }
    
    if (!record.getStatus().equals("running")) {
        log.warn("Execution not in running status, executionId={}", executionId);
        return;
    }
    
    // 主逻辑平铺，深度 = 0
    for (ExecutionStep step : record.getSteps()) {  // 深度 = 1
        processStep(step);  // 提取方法，内部深度独立计算
    }
}
```

**工具检测**：
- IntelliJ IDEA: 安装 MetricsReloaded 插件查看嵌套深度
- Checkstyle: 配置 `NPathComplexity` 规则
- PMD: 配置 `DeepNestedMethod` 规则

**原因**：
- 嵌套过深导致代码难以理解（需要记住多层上下文）
- 测试覆盖难度增加，需要更多测试组合
- 代码审查效率降低，难以追踪逻辑流程
- 符合业界最佳实践（Google、阿里巴巴规范建议 ≤ 4）

---

## 18. 敏感信息规范

**规则**：日志中禁止打印敏感信息，如直接打印请求头、Token、密码等。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 过滤敏感信息后打印日志 | 直接打印包含敏感信息的对象 |
| 打印脱敏后的参数 | 打印完整的请求头、请求体 |

```java
// ✅ 正确示例
@Slf4j
@RestController
public class WebhookController {
    
    public void handleWebhook(HttpServletRequest request) {
        // 只记录必要的非敏感信息
        log.info("Webhook received, uri={}, method={}", 
            request.getRequestURI(), request.getMethod());
        
        String signature = request.getHeader("X-Webhook-Signature");
        if (signature != null) {
            log.info("Webhook signature present, length={}", signature.length());
        }
    }
    
    public void configureAuth(String connectorId, AuthConfig authConfig) {
        // 脱敏处理后记录
        log.info("Auth configured, connectorId={}, authType={}", 
            connectorId, authConfig.getType());
        // 不记录 accessKey/secretKey
    }
}

// ❌ 错误示例
@Slf4j
@RestController
public class WebhookController {
    
    public void handleWebhook(HttpServletRequest request) {
        // ❌ 直接打印请求头，可能包含敏感信息
        log.info("Webhook headers: {}", request.getHeaderNames());
    }
    
    public void configureAuth(String connectorId, AuthConfig authConfig) {
        // ❌ 直接打印包含敏感信息的对象
        log.info("Auth config: {}", authConfig);
    }
}
```

**敏感信息类型**：
- 认证信息：Token、Session ID、Cookie、Webhook Signature
- 凭证信息：AccessKey、SecretKey、ClientID、ClientSecret
- 个人信息：密码、身份证号、手机号、邮箱
- 业务敏感信息：API Key、私钥、加密密钥

**工具检测**：
- Logback: 配置 `maskingConverter` 敏感信息脱敏
- 自定义 Logback Filter: 拦截敏感字段

**原因**：
- 安全合规要求（GDPR、网络安全法等）
- 防止敏感信息泄露到日志文件
- 避免日志被未授权人员访问导致信息泄露
- 降低安全审计风险

---

## 19. 日期格式化规范

**规则**：禁止使用 `SimpleDateFormat`，必须使用 `DateTimeFormatter`。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `DateTimeFormatter.ISO_LOCAL_DATE_TIME` | `new SimpleDateFormat("yyyy-MM-dd")` |
| `LocalDateTime.now().format(formatter)` | `new SimpleDateFormat("yyyy-MM-dd").format(new Date())` |

```java
// ✅ 正确示例
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

public String formatTime(LocalDateTime time) {
    return time.format(FORMATTER);
}

// ❌ 错误示例
import java.text.SimpleDateFormat;

private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

public String formatTime(Date date) {
    return SDF.format(date);
}
```

**原因**：
- `SimpleDateFormat` 不是线程安全的，多线程环境下可能产生错误结果
- `DateTimeFormatter` 是线程安全的，由 `java.time` 包提供
- Java 8+ 推荐使用 `java.time` API
- 避免并发环境下的日期格式化问题

---

## 总结

以上 19 条规范为连接器平台项目的**强制要求**，所有代码提交前必须确保符合规范。

请在 IDE 中配置相应的代码格式化和检查规则，确保代码风格统一。

---

*规范来源：能力开放平台（CAP-OPEN-001）`plan-code.md`，连接器平台沿用并适配*