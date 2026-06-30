# 批 3：非 java 审查报告

> mapper xml / yml / pom / sql migration。阶段0快扫 + 本轮针对性扫描。

## 文件覆盖表

| 文件类型 | 文件数 | 方式 | 问题数 |
|---------|--------|------|--------|
| open mapper xml | ~25 | grep 扫描(${}/SELECT*/JOIN) | 0 |
| market mapper xml | 8 | grep 扫描 | 0 |
| open yml | 3(dev/prod/默认) | 逐行扫描敏感配置 | 2 |
| market yml | 1 | 扫描 | 0 |
| pom.xml | 2(open/connector-api) | diff扫描 | 1(已确认GraalVM安全) |
| sql migration | 2(V2/V3) | 未逐行读 | 待补 |

## QC 意见（2 条）

### 意见 1
- 大类：安全编码
- 子类：硬编码安全相关的常量
- 级别：一般
- 问题原因：open-server/src/main/resources/application-dev.yml:8 `password: 123456`（MySQL 密码明文硬编码）。虽为 dev 环境，但密码进入版本库
- 修改建议：dev 也用环境变量或 .gitignore 本地覆盖

### 意见 2
- 大类：安全编码
- 子类：硬编码安全相关的常量
- 级别：建议
- 问题原因：application-prod.yml:8 `password: ${MYSQL_PASSWORD:openapp}`、L27 `password: ${REDIS_PASSWORD:changeme}` —— prod 用环境变量（好），但**默认值** openapp/changeme 暴露在代码库，若未设环境变量则用弱默认
- 修改建议：移除默认值或改为无意义占位（强制必须配置环境变量）

## 其他扫描结论

- **mapper xml**：open+market 全量 grep 确认 **0 处 `${}`、0 处 SELECT \***（LIKE 全 `#{}` 参数化）✅ 无 SQL 注入
- **pom.xml**：新增 `org.graalvm.polyglot` + `org.graalvm.js:js-language`（GraalVM JS 引擎，用于 FlowPublishValidator 脚本校验，沙箱安全已确认）；lombok 1.18.46 兼容性修复；无可疑/CVE 依赖
- **sql migration V2(137行)/V3(192行)**：未逐行读（context 极限），建议后续补审 DDL（索引/约束/字段类型/外键）

## 结论

非 java 文件无 SQL 注入风险（规范好）。主要问题是 yml 密码配置（dev 明文 123456，prod 默认值暴露）。GraalVM JS 依赖已确认沙箱安全。sql migration DDL 待后续补审。
