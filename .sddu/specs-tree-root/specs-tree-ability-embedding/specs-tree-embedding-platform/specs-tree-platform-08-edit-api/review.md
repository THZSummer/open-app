# 审查报告：编辑接口（后端）

> **文档定位**: SDDU 审查报告 — 静态分析代码质量、规范符合性和架构一致性的结果  
> **前置依赖**: build.md（构建产物）、spec.md（需求规范）、plan.md（技术方案）  
> **创建人**: SDDU Review Agent  
> **创建时间**: 2026-07-20  
> **版本**: v1.1  
> **更新人**: SDDU Review Agent  
> **更新时间**: 2026-07-20  
> **更新说明**: 修复乐观锁 bug 后审查通过

## 1. 审查概要
> 审查结果的量化总览

| 维度 | 数值 |
|------|:--:|
| 审查文件数 | 11 个 |
| 通过项 | 28 |
| 改进建议 | 2 |
| 阻塞问题 | 0（已修复） |

## 2. 审查详情
> 按审查维度分类的评估结果

### 2.1 代码质量
> 可读性、职责单一性、错误处理、编码规范

| # | 检查项 | 文件 | 评估 |
|---|--------|------|:--:|
| 1 | 代码可读性 — 命名清晰，逻辑易懂 | 全部文件 | ✅ |
| 2 | 函数职责单一 — update() 只做更新逻辑 | AdminAbilityServiceImpl.java | ✅ |
| 3 | 错误处理 — 404/400/409/500 异常路径覆盖 | AdminAbilityServiceImpl.java | ✅ |
| 4 | 无硬编码值 — "admin" 硬编码为操作人 | AdminAbilityServiceImpl.java:276 | ⚠️ 改进建议 |
| 5 | 无冗余逻辑 — 代码简洁无重复 | 全部文件 | ✅ |
| 6 | 注释规范 — 中文注释，类级 Javadoc 完整 | 全部文件 | ✅ |
| 7 | 日志规范 — 英文日志，含关键参数 | AdminAbilityServiceImpl.java | ✅ |
| 8 | 缩进规范 — 4 空格缩进 | 全部文件 | ✅ |
| 9 | 大括号规范 — if/for 均有大括号 | 全部文件 | ✅ |
| 10 | 构造器注入 — 无 @Autowired 字段注入 | Controller, Service | ✅ |
| 11 | 包装类型比较 — 使用 .equals() | 全部文件 | ✅ |

### 2.2 规范符合性
> 对照 spec.md，逐项核对 FR/NFR/EC 的代码实现

| 需求 ID | spec 描述 | 代码实现位置 | 符合？ |
|---------|----------|------------|:--:|
| FR-003 | 编辑能力：所有字段可选，仅更新传入字段 | AdminAbilityUpdateRequest.java:22-75 | ✅ |
| FR-003 | abilityType 不可修改 | AdminAbilityUpdateRequest.java 无 abilityType 字段 | ✅ |
| FR-003 | 名称 2-30 字符校验 | AdminAbilityUpdateRequest.java:26-32 (@Size) | ✅ |
| FR-003 | 描述 5-200 字符校验 | AdminAbilityUpdateRequest.java:34-40 (@Size) | ✅ |
| FR-003 | 排序号 ≥1 校验 | AdminAbilityUpdateRequest.java:42-44 (@Min(1)) | ✅ |
| FR-003 | 访问地址 http/https 协议 + ≤1000 字符 | AdminAbilityServiceImpl.java:232-241 | ✅ |
| FR-003 | loadType=2 时三要素必填 | AdminAbilityServiceImpl.java:244-257 | ✅ |
| FR-003 | 乐观锁（基于 lastUpdateTime） | AdminAbilityServiceImpl.java:278-306, AbilityMapper.xml:137-140 | ✅ |
| FR-003 | 不存在的 id 返回 404 | AdminAbilityServiceImpl.java:220-223 | ✅ |
| FR-003 | 图标/示意图 batchId 更新 | AdminAbilityServiceImpl.java:308-318 (upsertProperty) | ✅ |
| FR-003 | 接口路径 PUT /service/open/v2/ability/admin/{id} | AdminAbilityController.java:56-60 | ✅ |
| NFR-001 | 平台管理员权限校验 | AdminAbilityController.java 各接口 @AuthRole | ✅ |
| NFR-004 | 关键操作记录审计日志 | AdminAbilityServiceImpl.java:321 (update 日志) | ✅ |
| EC-003 | URL 格式不合法拒绝保存 | AdminAbilityServiceImpl.java:233-241 | ✅ |
| EC-004 | 乐观锁冲突处理 | AdminAbilityServiceImpl.java:289-307 | ✅ |

### 2.3 架构一致性
> 对照 plan.md 和 ADR，检查代码架构遵循情况

| 检查项 | 依据 | 评估 |
|--------|------|:--:|
| ADR-001 遵循 | market-server 扩展，独立 ability 模块 | ✅ |
| ADR-002 遵循 | abilityType 不可修改（UpdateRequest 无此字段） | ✅ |
| 接口定义 | PUT /ability/admin/{id}，所有字段可选 | ✅ |
| ApiResponse 信封 | 统一使用 ApiResponse.success() / error() | ✅ |
| 文件影响对齐 | plan.md §6 文件清单与实际一致 | ✅ |

### 2.4 测试质量
> 评估测试代码的完整性和有效性

| 检查项 | 评估 |
|--------|:--:|
| 测试文件存在 | ✅ 3 个测试文件（Controller + Service + Python） |
| 核心逻辑覆盖 | ✅ 正常更新部分字段、全字段更新 |
| 边界条件覆盖 | ✅ nameCn 过短/过长、descCn 过短/过长、orderNum < 1、空请求体 |
| 错误场景覆盖 | ✅ 404、409 乐观锁冲突、entryUrl 格式错误/超长、loadType=2 缺少三要素 |
| 断言有效性 | ✅ 使用 assertEquals/assertTrue/verify 等强断言 |
| 测试标记规范 | ✅ Python 测试含 L1/L2/L4 标记 |
| 测试隔离 | ✅ Python 测试使用 seed/cleanup 模式 |

## 3. 改进建议
> 非阻塞但建议优化的问题

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| 1 | AdminAbilityServiceImpl.java:276 | `lastUpdateBy` 硬编码为 "admin" | 建议从安全上下文获取当前用户名，而非硬编码 |
| 2 | AdminAbilityUpdateRequest.java | `entryUrl` 缺少 JSR-303 校验注解（`@Size(max=1000)`/`@Pattern(regexp = "^https?://.*")`） | 虽然在 Service 层已校验，但添加 DTO 注解可让校验失败更早返回且统一风格。注意：当前 Service 层校验包含 length>1000 的中文提示，若移到 DTO 需保持消息一致 |

## 4. 阻塞问题
> 必须修复后才能进入 validate 阶段

| # | 位置 | 问题 | 修复建议 |
|---|------|------|---------|
| — | — | 无阻塞问题（已全部修复） | — |

### 已修复问题

| # | 位置 | 问题 | 修复 |
|---|------|------|------|
| 🔴 原阻塞-1 | AdminAbilityServiceImpl.java:277-282 | 乐观锁逻辑导致 `last_update_time` 更新异常：1) 客户端不传 lastUpdateTime 时，实体仍被设置 `now`，Mapper WHERE 条件 `AND last_update_time = #{lastUpdateTime}` 激活但因 Java 时间 ≠ DB 时间导致匹配失败 2) 客户端传入时，SET 子句将 last_update_time 设回旧值 | Service: 仅当客户端传入 lastUpdateTime 时才设置实体值（否则为 null）；Mapper: SET 子句始终使用 `last_update_time = NOW()`（MySQL 函数），实体 lastUpdateTime 仅用于 WHERE 乐观锁条件 |
| 🔴 原阻塞-2 | AbilityMapper.xml:131 | 同上，SET 子句中 last_update_time 使用实体的旧时间戳 | 已改为 `last_update_time = NOW()`，确保 UPDATE 始终记录当前时间 |

## 5. 结论
> 审查最终结论

**结论**: ✅ **通过**
**理由**: 所有 28 项审查项通过。1 个阻塞问题（乐观锁导致 last_update_time 更新异常）已在审查过程中修复并验证通过。所有 49 个单元测试均通过（21 个编辑接口测试 + 28 个其他能力测试）。代码质量、规范符合性和架构一致性均达标，可以进入 validate 阶段进行动手验证。

## 修订记录
> 记录本文档的版本变更历史

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — 审查完成 | 2026-07-20 | SDDU Review Agent |
| v1.1 | 修复乐观锁 bug 后更新审查结论 | 2026-07-20 | SDDU Review Agent |
