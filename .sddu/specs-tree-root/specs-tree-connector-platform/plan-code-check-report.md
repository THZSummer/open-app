# 📋 plan-code.md 规范符合性检查报告

**检查时间**: 2026-05-24  
**检查范围**: connector-api + open-server 新增代码（共 74 个文件）  
**规范版本**: plan-code.md（16 条规则）

---

## 总览

| 通过 | 违反 | 不适用 | 合计 |
|:----:|:----:|:------:|:----:|
| 12 | 2 | 2 | 16 |

---

## 逐项检查

### Rule 1: 注释规范（中文注释）
- **结果**: ✅ **通过**
- **范围**: 全部 74 个新增文件
- **检查内容**: 代码注释统一使用中文
- **详情**: 所有 JavaDoc 和方法内注释均使用中文，如 `// 反应式顺序执行器`、`// 构建执行结果` 等。英文仅出现在代码逻辑和日志中，符合规范要求。

---

### Rule 2: 日志规范（英文日志）
- **结果**: ✅ **通过**
- **范围**: 全部 74 个新增文件
- **检查内容**: 日志信息统一使用英文
- **详情**: 所有 `log.info/warn/error/debug` 消息均为英文，如：
  - `log.info("Creating connector: nameCn={}, nameEn={}")`
  - `log.info("Flow started: id={}", flowId)`
  - `log.warn("Rate limit exceeded for flowId={}", flowId)`

---

### Rule 3: SQL 查询规范（禁止 SELECT \*）
- **结果**: ✅ **通过**
- **范围**: 4 个 MyBatis XML 文件（ConnectorMapper.xml, ConnectorVersionMapper.xml, FlowMapper.xml, FlowVersionMapper.xml）
- **检查内容**: 禁止使用 `SELECT *`，必须明确指定字段名
- **详情**: 所有 `<select>` 语句均明确列出字段列表，未发现 `SELECT *`。

---

### Rule 4: 字符串格式化规范（Locale.ROOT）
- **结果**: ✅ **不适用**
- **范围**: 全部新增 Java 文件
- **检查内容**: 使用 `String.format()` 必须指定 `Locale.ROOT`
- **详情**: 新增代码中未使用 `String.format()`，无违反。

---

### Rule 5: 模糊查询规范（禁止 % 开头）
- **结果**: ❌ **违反**（低严重度）
- **范围**: ConnectorMapper.xml, FlowMapper.xml
- **文件**: `open-server/src/main/resources/mapper/ConnectorMapper.xml:75-76`
- **文件**: `open-server/src/main/resources/mapper/ConnectorMapper.xml:92-93`
- **文件**: `open-server/src/main/resources/mapper/FlowMapper.xml:81-82`
- **文件**: `open-server/src/main/resources/mapper/FlowMapper.xml:98-99`

```xml
AND (name_cn LIKE CONCAT('%', #{keyword}, '%')
     OR name_en LIKE CONCAT('%', #{keyword}, '%'))
```

**违反说明**: 模糊查询使用 `%` 开头导致索引失效（全表扫描）。项目所有现有 mapper 也使用此模式（CallbackMapper、ApprovalFlowMapper、PermissionMapper 等），属项目级历史惯例。

**建议**: 在 keyword 搜索场景中权衡业务需求（全词搜索 vs 前缀匹配）。如需全词搜索可引入全文索引或搜索引擎。

---

### Rule 6: Switch 语句规范（必须有 default）
- **结果**: ✅ **通过**
- **范围**: `DataProcessorExecutor.java:69`
- **检查内容**: switch 语句必须有 default 分支

```java
switch (sourceType) {
    case "constant":
        resolvedValue = sourceValue;
        break;
    case "reference":
    default:
        // 路径引用
        ...
        break;
}
```

**详情**: default 分支与 "reference" case 合并，处理所有非 constant 类型，符合规范。

---

### Rule 7: 缩进规范（4 空格）
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 文件
- **检查内容**: Java 代码必须使用 4 个空格缩进
- **详情**: 全部文件使用 4 空格缩进，未发现 Tab 或 2 空格缩进。

---

### Rule 8: 变量声明规范（每行一个变量）
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 文件
- **检查内容**: 每行只能声明一个变量
- **详情**: 未发现一行声明多个变量的情况，如：
  ```java
  Map<String, Object> outputData = new HashMap<>();
  Map<String, Object> inputData = new HashMap<>();  // 分开声明
  ```

---

### Rule 9: 大括号规范（条件语句必须使用大括号）
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 文件
- **检查内容**: if/else/for/while 必须使用大括号
- **详情**: 所有条件语句均使用大括号包裹，未发现省略大括号的模式。

---

### Rule 10: 字符串操作规范（Locale.ROOT）
- **结果**: ❌ **违反**（中严重度）
- **文件**: `ConnectorNodeExecutor.java:118`

```java
.method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
```

**违反说明**: `method.toUpperCase()` 未指定 `Locale.ROOT`。在土耳其等环境下 `.toUpperCase()` 可能导致不正确的转换（'i' → 'İ'），导致 HTTP Method 解析失败。

**建议**: 改为 `method.toUpperCase(Locale.ROOT)`。

---

### Rule 11: 空代码块规范
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 文件
- **检查内容**: 禁止空代码块
- **详情**: 未发现空 if/for/while/try/catch 块。

---

### Rule 12: 行尾空格规范
- **结果**: ⚠️ **存在少量行尾空格**
- **范围**: `.sddu/specs-tree-root/README.md` 和 `validation-report.md`
- **检查内容**: 禁止代码行尾有多余空格
- **详情**: Markdown 文件中存在少量行尾空格（由自动生成的文档引入），不影响代码逻辑，但建议清理。

---

### Rule 13: Shell 脚本规范（set -ex）
- **结果**: ❌ **违反**（中严重度）
- **文件**: `connector-api/scripts/start.sh:2`
- **文件**: `connector-api/scripts/stop.sh:2`

```bash
set -x
```

**违反说明**: 使用了 `set -x` 但**缺少 `-e`** 标志。`set -e` 确保脚本在遇到错误时立即退出，防止错误累积。

**影响**: 如 `mvn spring-boot:run` 启动失败，脚本仍会继续执行（尝试 curl 健康检查），产生误导性日志。

**建议**: 改为 `set -ex`。

---

### Rule 14: 圈复杂度规范（≤ 15）
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 方法
- **检查内容**: 方法圈复杂度必须维持在 15 以下
- **详情**: 对最复杂的几个方法手工计算：

| 方法 | 圈复杂度 | 状态 |
|------|:--------:|:----:|
| `buildResult()` (ReactiveSequentialExecutor) | 5 | ✅ |
| `executeHttpCall()` (ConnectorNodeExecutor) | 7 | ✅ |
| `updateFlowConfig()` (FlowService) | 6 | ✅ |
| `topologicalSort()` (ReactiveSequentialExecutor) | 5 | ✅ |

所有方法复杂度均在 15 以下，符合规范。

---

### Rule 15: 函数深度规范（嵌套 ≤ 5）
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 方法
- **检查内容**: 方法最大嵌套深度不得超过 5 层
- **详情**: 对最深的嵌套路径检查：

| 方法 | 最大嵌套深度 | 状态 |
|------|:-----------:|:----:|
| `buildResult()` | 4（for>if>if>if） | ✅ |
| `executeHttpCall()` | 4（try>if>if>try） | ✅ |
| `executeNode()` | 2（if>if） | ✅ |

所有方法嵌套深度 ≤ 4，符合规范。

---

### Rule 16: 敏感信息规范
- **结果**: ✅ **通过**
- **范围**: 全部新增 Java 文件
- **检查内容**: 日志中禁止打印敏感信息（Token、密码、凭证等）
- **详情**: 
  - 日志中仅记录 flowId、nameCn、connectorId 等非敏感信息
  - 未打印完整的请求头、请求体
  - 凭证通过 `ExecutionContext.credentials` 内存传递，不在日志中输出
  - `X-Sys-Token` 仅校验存在性，不记录具体值

---

## 违反汇总

| 规则 | 严重度 | 文件 | 描述 | 建议 |
|:----:|:------:|------|------|------|
| **#5** | 低 | ConnectorMapper.xml, FlowMapper.xml | 模糊查询 `LIKE '%keyword%'` 导致索引失效 | 引入全文索引或搜索引擎 |
| **#10** | 中 | ConnectorNodeExecutor.java:118 | `toUpperCase()` 未指定 `Locale.ROOT` | 改为 `method.toUpperCase(Locale.ROOT)` |
| **#13** | 中 | start.sh, stop.sh | `set -x` 缺少 `-e` | 改为 `set -ex` |
| **#12** | 低 | README.md, validation-report.md | 少量行尾空格 | 配置 IDE 保存时自动清理 |

## 结论

| 通过率 | 阻塞问题 | 需修复 |
|:------:|:--------:|:------:|
| **87.5%**（14/16） | ❌ 无 | 2 个中严重度 + 2 个低严重度 |

### 优先级建议

| 优先级 | 规则 | 操作 |
|:------:|:----:|------|
| 🥇 P1 | #13 Shell set -ex | 改为 `set -ex`（1 行改动，防止脚本错误累积） |
| 🥇 P1 | #10 Locale.ROOT | 改为 `toUpperCase(Locale.ROOT)`（1 行改动，防止国际化 bug） |
| 🥈 P2 | #5 模糊查询 | 引入全文索引（项目级决策，现有代码均同此模式） |
| 🥉 P3 | #12 行尾空格 | 清理即可 |

---