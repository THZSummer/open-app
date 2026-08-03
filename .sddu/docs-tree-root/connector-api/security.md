# connector-api — 安全模型

> **文档定位**: sddu-docs-security — 安全策略文档 — 认证流程、授权矩阵、安全边界  
> **输出文件名**: connector-api-security.md  
> **数据来源**: 代码扫描生成 — connector-api + specs-tree-connector-platform-v3 基线  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 数据面认证

| 方式 | 说明 |
|------|------|
| AKSK 凭证 | 触发方携带 AK/SK Token 校验 |
| OAuth | 数据面授权（设计基线） |
| CI Headers | 消费凭证头传递 |

## 2. 脚本节点安全（GraalJS 沙箱，五层纵深防御）

| 层 | 防御措施 |
|:--:|---------|
| 1. 引擎 | GraalJS ES2022 严格模式 |
| 2. 线程 | boundedElastic 线程隔离 |
| 3. 能力 | IO / 线程 / 进程 / Native / 环境变量全部关闭 |
| 4. 执行 | statementLimit=10000（防死循环） |
| 5. 超时 | 超时强制终止（默认 5s，范围 1~30s） |

## 3. 入站安全

| 机制 | 说明 |
|------|------|
| 入站限流 | 连接流级限流（Redis 令牌桶），触发 429 |
| 缓存 | 全流/节点级缓存（cache_status），防重放 |
| 版本指针 | 仅执行 deployed_version_id 指向的已发布版本 |
| 引用校验 | connector_version_ref_t 前置「被引用」校验（失效/删除保护） |

## 4. 敏感数据

- 连接配置 `connection_config` 仅声明认证类型 Schema（含 sensitive 标记），**不存储凭证值**
- 执行日志中敏感信息（凭证、Token）自动脱敏

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
