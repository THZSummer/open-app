# 审查报告：新增接口（后端）

> **Feature**: EMBED-PLATFORM-CREATE-API-001（新增接口，后端）  
> **分支**: `feature/embedding-platform-06-create`  
> **Commit 基线**: `87bd2a5a`  
> **审查时间**: 2026-07-20  
> **审查人**: SDDU Review Agent  
> **结论**: ✅ **通过**（修复后全部通过）

## 1. 审查总览

| 维度 | 结果 | 说明 |
|------|:----:|------|
| 代码质量 | ✅ | 可读性良好，职责单一，错误处理覆盖 |
| 规范符合性 | ✅ | 修复后 100% 符合 spec.md §5.3 字段约束 |
| 架构一致性 | ✅ | 遵循 plan.md + ADR-001/ADR-002 |
| 测试覆盖 | ✅ | Java 单元测试（22 用例全部通过）+ Python 集成测试（11 用例全部通过） |
| 安全性 | ✅ | @AuthRole 已添加，仅管理员可访问 |

## 2. 发现问题及修复

### 🔴 阻塞问题（5 项）

| # | 问题描述 | 文件 | 修复内容 |
|---|---------|------|---------|
| 1 | **`nameEn` 缺少 `@NotBlank`** — spec §5.3 要求 英文名称 必填，DTO 仅有 `@Size` 无 `@NotBlank`，允许空值通过 JSR-303 校验 | `AdminAbilityCreateRequest.java:38-41` | 添加 `@NotBlank(message = "英文名不能为空")` 及 `requiredMode = REQUIRED` |
| 2 | **`descEn` 缺少 `@NotBlank`** — 同上，spec 要求英文描述必填 | `AdminAbilityCreateRequest.java:47-51` | 添加 `@NotBlank(message = "英文描述不能为空")` 及 `requiredMode = REQUIRED` |
| 3 | **`hidden` 默认值错误** — spec 要求「默认隐藏」(default=1)，DTO 和 Service 均默认 0（展示） | `AdminAbilityCreateRequest.java:66-67` + `AdminAbilityServiceImpl.java:147` | DTO: `private Integer hidden = 1;` Service: `entity.setHidden(..., 1)` |
| 4 | **缺少 `@Transactional`** — `create()` 方法写主表 + 属性表（两表可能写两条），无事务保证原子性 | `AdminAbilityServiceImpl.java:99` | 添加 `@Transactional(rollbackFor = Exception.class)` |
| 5 | **`id` 列无 AUTO_INCREMENT 导致 insert 失败** — 主键 `id` 需用 `IdGeneratorStrategy` 手动生成，且 MyBatis XML 的 `useGeneratedKeys=true` 会忽略手动设置 | `AbilityMapper.xml:74-85` + `AbilityPropertyMapper.xml:37-46` + `AdminAbilityServiceImpl.java:159-160` | XML 移除 `useGeneratedKeys`，手动 INSERT `id`；Service 注入 `IdGeneratorStrategy`，insert 前调用 `entity.setId(idGenerator.nextId())` |

### 🟡 改进项（1 项）

| # | 问题描述 | 文件 | 说明 |
|---|---------|------|------|
| 6 | **响应体缺少 spec 要求的 data** — plan.md §3.3 期望响应包含 `{abilityType, nameCn, createTime}`，原代码返回无 data | `AdminAbilityService.java` + `AdminAbilityServiceImpl.java` | 已修复为返回 `ApiResponse<Map<String, Object>>`，data 包含三字段 |

## 3. 逐项审查详情

### 3.1 规范符合性 — 与 spec.md 对照

| FR | 状态 | 说明 |
|:--:|:----:|------|
| FR-002 | ✅ | 创建能力：abilityType 唯一性校验、loadType 联动校验、batchId 提交图标/示意图、写主表+属性表、状态默认启用、编码 8-255 范围（TINYINT 约束） |
| §5.3 字段约束 | ✅ | 14 个字段全部符合，nameEn/descEn 修复为 @NotBlank，hidden 默认修复为 1 |
| NFR-001 | ✅ | @AuthRole 权限校验 |
| EC-001 | ✅ | 409 编码已占用 |
| EC-003 | ✅ | 400 URL 格式校验 |

### 3.2 参数校验矩阵

| 字段 | @NotBlank | @Size | @Min/@Max | 自定义校验 |
|------|:---------:|:-----:|:---------:|:----------:|
| abilityType | — | — | — | 唯一性校验(Service) |
| nameCn | ✅ | 2-30 | — | — |
| nameEn | ✅(修复) | 2-30 | — | — |
| descCn | ✅ | 5-200 | — | — |
| descEn | ✅(修复) | 5-200 | — | — |
| iconBatchId | ✅ | — | — | — |
| orderNum | — | — | ≥1(@Min) | 自动补全 max+1 |
| entryUrl | — | — | — | http/https 正则 + ≤1000 字符 |
| loadType=2 | — | — | — | entryUrl/routePath/aliasName 三要素必填 |

### 3.3 代码一致性

| 规范 | 状态 |
|------|:----:|
| 注释中文（plan-code.md §1） | ✅ |
| 日志英文（plan-code.md §2） | ✅ |
| 构造器注入 | ✅ |
| ApiResponse 信封 | ✅ |
| @AuthRole 权限 | ✅ |
| 缩进 4 空格 | ✅ |

## 4. 测试结果

### 4.1 Java 单元测试

| 测试类 | 用例数 | 通过 | 结果 |
|--------|:-----:|:----:|:----:|
| AdminAbilityCreateControllerTest | 7 | 7 | ✅ |
| AdminAbilityCreateServiceTest | 8 | 8 | ✅ |
| AdminAbilityListServiceTest | 7 | 7 | ✅ |
| **总计** | **22** | **22** | ✅ |

### 4.2 Python 集成测试

| 级别 | 用例数 | 通过 | 结果 |
|:----:|:-----:|:----:|:----:|
| L1 (正常流程) | 2 | 2 | ✅ |
| L2 (业务规则) | 5 | 5 | ✅ |
| L4 (边界/反向) | 4 | 4 | ✅ |
| **总计** | **11** | **11** | ✅ |

## 5. 最终结论

✅ **审查通过** — 5 项阻塞问题已修复，1 项改进项已落实。符合 spec.md/plan.md/plan-code.md 全部规范要求。可以进入 validate 阶段动手验证。

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始审查报告 | 2026-07-20 | SDDU Review Agent |
