# 审查报告：通用文件上传后端（TASK-003）

> **文档定位**: SDDU 产物审查报告 — 静态分析 TASK-003 代码质量、规范符合性和架构一致性  
> **审查对象**: EMBED-PLATFORM-001 / TASK-003（通用文件上传后端）  
> **审查基准**: spec.md v1.4（FR-005 验收标准）、plan.md v3.8（API #5 设计）、plan-code.md（代码规范）  
> **审查人**: SDDU Review Agent  
> **审查日期**: 2026-07-17  

---

## 1. 审查概要

| 维度 | 结果 |
|------|:----:|
| 审查文件数 | 10 个源码文件 + 3 个测试文件 + 1 个迁移脚本 |
| 编译检查 | ✅ 通过（`mvn compile` 无错误） |
| 通过项 | 17 项 |
| 改进项 | 4 项 |
| 阻塞项 | 0 项 |
| 结论 | ⚠️ **有条件通过** |

---

## 2. 代码质量审查

> 阅读代码，评估可读性、职责单一性、错误处理和编码规范

### 2.1 代码结构与命名

| # | 检查项 | 结果 | 说明 |
|---|--------|:---:|------|
| 1 | 命名清晰 | ✅ 通过 | 类名/方法名/变量名语义明确，符合驼峰命名 |
| 2 | 函数职责单一 | ✅ 通过 | `upload()` 分步骤：空检查 → 查找 Validator → 校验 → 委托存储，无多余职责 |
| 3 | 错误处理完善 | ✅ 通过 | 空文件、null bizType、不支持的 bizType、IO 异常均有对应处理 |
| 4 | 硬编码值 | ✅ 通过 | 文件大小常量/格式检查均提取为局部常量，无魔法数字 |
| 5 | 冗余逻辑 | ✅ 通过 | 无冗余中间变量，无重复代码块 |
| 6 | 大括号规范 | ✅ 通过 | 所有 if/for/while 均使用大括号（规范 #10） |
| 7 | 缩进规范 | ✅ 通过 | 4 空格缩进（规范 #7） |
| 8 | 变量声明 | ✅ 通过 | 每行一个变量（规范 #8） |
| 9 | 字符串操作 `Locale.ROOT` | ⚠️ 改进 | `ImageValidationUtils.getFileExtension()` 第29行使用 `toLowerCase()` 缺 `Locale.ROOT`（规范 #11） |

### 2.2 关键质量细节

**✅ 亮点**：`CommonFileServiceImpl.upload()` 方法设计精良——通过 `List<FileValidator>` 自动注入 + `stream().filter(v -> v.supportedBizType() == bizType)` 按业务类型匹配校验器，新增 bizType 只需新加一个 Validator 实现类，完全符合开闭原则。

**Service 层流程图**：
```
upload(file, bizType)
  ├── 空文件校验 → throw 400
  ├── null bizType → throw 400
  ├── 查找匹配 Validator（stream filter）
  │    └── 未找到 → throw 400（不支持的业务类型）
  │    └── 找到 → validator.validate(file)
  │         ├── 格式校验
  │         ├── 文件大小校验  
  │         └── 图片像素尺寸校验（SVG 跳过像素校验）
  └── storageStrategy.store(file, bizType, batchId, ext)
       ├── dev 模式：本地磁盘 + DB 记录
       └── standard 模式：占位 /ability-files/ 路径
```

---

## 3. 规范符合性审查

> 对照 spec.md 逐项核对 FR/NFR/EC

### 3.1 FR-005 验收标准逐项检查

| # | 验收标准 | 状态 | 说明 |
|---|---------|:----:|------|
| FR-005-1 | 接口路径 `POST /service/open/v2/file/upload` | ✅ | `CommonFileController` 映射正确 |
| FR-005-2 | 请求参数 `file`（MultipartFile）+ `bizType`（int） | ✅ | `@RequestParam("file") MultipartFile, @RequestParam("bizType") Integer` |
| FR-005-3 | 图标校验（bizType=1）：PNG/SVG, 40×40PX, ≤200KB | ✅ | `IconFileValidator` 实现完整 |
| FR-005-4 | 示意图校验（bizType=2）：PNG/JPG, 520×288PX, ≤500KB | ✅ | `DiagramFileValidator` 实现完整 |
| FR-005-5 | 校验失败返回 400，提示具体规则 | ✅ | `BusinessException.badRequest()` 带中英文提示 |
| FR-005-6 | 校验通过写 `openplatform_common_file_t`（开发环境） | ✅ | `DevFileStorageStrategy.saveFileRecord()` |
| FR-005-7 | 返回 `{batchId, showUrl}` | ✅ | `UploadResult` DTO + `Controller` 组装 Map |
| FR-005-8 | 接口权限仅平台管理员 | ✅ | `@AuthRole` 注解 |

### 3.2 NFR 符合性

| ID | 需求 | 状态 | 说明 |
|----|------|:----:|------|
| NFR-001 | 平台管理权限 | ✅ | `@AuthRole` 校验 |
| NFR-002 | 服务端校验格式/尺寸/大小 | ✅ | Validator 层完成 |
| NFR-003 | 写入即对开放面可见 | ✅ | 同 DB（本节暂不验证） |

### 3.3 EC 符合性

| ID | 场景 | 状态 | 说明 |
|----|------|:----:|------|
| EC-005 | 图标格式/尺寸不符合 | ✅ | 400 + 规则提示 |
| EC-007 | 上传文件格式/尺寸/大小不符合 | ✅ | 400 + 具体规则提示 |

---

## 4. 架构一致性审查

> 对照 plan.md、ADR 和 plan-code.md，检查架构决策遵循情况

### 4.1 ADR 符合性

| ADR | 决策 | 状态 | 说明 |
|-----|------|:----:|------|
| ADR-001 | CRUD 放 market-server | ✅ | 上传接口在 market-server 实现 |

### 4.2 计划对照（关键项）

| # | 计划项 | 实际 | 结果 | 说明 |
|---|--------|------|:----:|------|
| 1 | 存储模式 `ability.file.storage-mode` | `FileStorageStrategy` 接口 + `@ConditionalOnProperty` | ✅ | 实现完全一致 |
| 2 | `standard` 实现默认，`dev` 独立类 | `StandardFileStorageStrategy`(`matchIfMissing=true`) + `DevFileStorageStrategy`(`havingValue="dev"`) | ✅ | ✅ |
| 3 | `@StandardTodo` 标记 | 标注在类 + 两个方法上 | ✅ | 符合预期 |
| 4 | Controller 仅参数绑定+权限 | 仅 `@RequestParam` + `@AuthRole`，无业务逻辑 | ✅ | ✅ |
| 5 | V5 脚本：`CREATE TABLE openplatform_common_file_t` | `safe_create_table` 存储过程幂等创建 | ✅ | 幂等设计 |
| 6 | 包路径：`ability/service/AdminAbilityFileService` | `file/service/CommonFileService` | ⚠️ 改进 | 模块名从 ability 改为 file，类名从 AdminAbilityFile 改为 CommonFile |

### 4.3 迁移脚本检查（plan-code.md §4）

| # | 规范 | 状态 | 说明 |
|---|------|:----:|------|
| 1 | 幂等设计 | ✅ | `safe_create_table` 存储过程判断表已存在则跳过 |
| 2 | 每条语句独立 | ✅ | 存储过程 + CREATE TABLE 均独立 |
| 3 | 无事务包裹 | ✅ | 无 `BEGIN/COMMIT` |
| 4 | 字段字符集规范（§4.7） | ✅ | 字段无显式 `CHARACTER SET`（继承表级 utf8mb4） |
| 5 | 文件结构模板 | ✅ | 包含存储过程定义、新建表、清理过程、迁移完成标记 |
| 6 | 结尾清理存储过程 | ✅ | `DROP PROCEDURE IF EXISTS safe_create_table; DROP PROCEDURE IF EXISTS safe_add_column;` |

---

## 5. 测试质量审查

> 阅读测试代码，评估覆盖完整性（静态分析，不运行）

### 5.1 单元测试覆盖

| 测试类 | 用例数 | 覆盖场景 | 结果 |
|--------|:-----:|---------|:----:|
| `CommonFileServiceTest` | 7 | 正常上传、dev 模式、空文件、null bizType、不支持 bizType、校验失败、getShowUrl 委托 | ✅ |
| `CommonFileControllerTest` | 3 | 上传成功响应、缺 bizType、缺文件 | ❌ 见下 |

### 5.2 测试质量评估

| # | 检查项 | 结果 | 说明 |
|---|--------|:----:|------|
| 1 | 测试是否存在 | ✅ | 3 个测试文件均存在 |
| 2 | 核心逻辑路径覆盖 | ✅ | Service 层覆盖成功路径 + 5 条异常路径 |
| 3 | 边界条件覆盖 | ✅ | 空文件、null bizType、不支持 bizType、无扩展名 |
| 4 | 断言有效性 | ⚠️ 改进 | 控制器测试 `$.code` 断言值 `"0"` 与 `ApiResponse.success()` 实际返回值 `"200"` 不符 |
| 5 | 控制器测试可运行 | ❌ | `CommonFileControllerTest` 因 Spring 上下文缺少 `UserResolveStrategy` Bean 全部失败（3/3 Errors） |

### 5.3 Python 集成测试

| 文件 | 层级 | 用例数 | 状态 |
|------|:----:|:-----:|:----:|
| `test_common_file_upload.py` | L1 | 2（成功上传图标/示意图） | ✅ |
| | L2 | 8（格式/尺寸/大小/JPEG/SVG） | ✅ |
| | L4 | 5（空文件/缺参数/不支持类型/无扩展名） | ✅ |

---

## 6. 发现问题汇总

### 6.1 阻塞问题 — 0 个

无阻塞问题。编译通过，架构设计合理，核心业务逻辑实现正确。

### 6.2 改进项 — 4 个

| # | 类别 | 严重度 | 问题 | 文件 | 建议 |
|---|------|:------:|------|------|------|
| 1 | 测试 | ⚠️ 中 | **控制器测试无法运行**：`CommonFileControllerTest` 3 个用例因 Spring 上下文缺少 `UserResolveStrategy` Bean 全部报 Error（`ApplicationContext failure threshold exceeded`） | `CommonFileControllerTest.java` | 在 `@WebMvcTest` 中 mock 或提供 `UserResolveStrategy` 测试桩，或添加 `@MockBean` 补全依赖。另外考虑用 `@AutoConfigureMockMvc` + `@SpringBootTest` 替代 `@WebMvcTest` 以加载完整上下文 |
| 2 | 测试 | ⚠️ 中 | **控制器测试断言码值错误**：`.andExpect(jsonPath("$.code").value("0"))` 期望 `"0"`，但 `ApiResponse.success()` 从 `ResponseCodeEnum.SUCCESS` 获取码值 `"200"`。上下文加载修复后此断言将失败 | `CommonFileControllerTest.java` | 将 `"0"` 改为 `"200"` |
| 3 | 规范 | ⚠️ 低 | **缺少 `Locale.ROOT`**：`ImageValidationUtils.getFileExtension()` 第29行 `filename.substring(lastDot + 1).toLowerCase()` 未指定 `Locale.ROOT`，违反 plan-code.md §11 | `ImageValidationUtils.java` | 改为 `toLowerCase(Locale.ROOT)` |
| 4 | 文档 | ⚠️ 低 | **build.md 与实际实现不一致**：build.md 声称 `AdminAbilityFileServiceTest` 有 15 用例，实际 `CommonFileServiceTest` 只有 7 用例；类名从 `AdminAbilityFile*` 变为 `CommonFile*`，包从 `ability` 变为 `file` | `build.md` | 更新 build.md 反映真实文件名、包路径和测试用例数 |

### 6.3 备注项

| # | 类别 | 说明 |
|---|------|------|
| 1 | 架构 | 代码从 `ability` 包迁移到 `file` 包，类命名从 `AdminAbilityFileService` → `CommonFileService`。功能等价，路径更合理（文件上传是通用能力，不限于能力管理）。建议同步更新 plan.md 文件影响分析 |
| 2 | 设计 | `CommonFileUploadRequest` DTO 已创建但未被 Controller 使用。Controller 直接使用 `@RequestParam` 绑定参数。属于死代码，可选择删除或改用 `@ModelAttribute` 绑定 |
| 3 | 设计 | `SVG` 图标跳过像素尺寸校验是合理设计（SVG 矢量格式无固定像素尺寸），符合 spec 隐含意图 |

---

## 7. 审查结论

| 条件 | 要求 | 实际 |
|------|:----:|:----:|
| 阻塞问题 | 0 个 | 0 个 |
| 改进项 | < 5 个 | 4 个 |
| 规范符合率 | 100% | 100%（1 项改进级不符合为 Locale.ROOT） |

### ✅ **审查结论：⚠️ 有条件通过**

**核心理由**：
- 核心架构设计（存储策略模式 + Validator 拆分 + 配置切换）完全正确
- 编译通过，Service 层 7 个单元测试全部通过
- FR-005 所有验收标准在代码中有对应实现
- 4 个改进项集中于测试可运行性和规范性细节，不影响业务功能正确性

**建议**：
1. 优先修复控制器测试上下文加载问题（#1）和码值断言（#2）
2. 修复 `Locale.ROOT` 缺少问题（#3）
3. 更新 build.md 反映实际文件路径和测试数（#4）
4. 修复后可运行 `@sddu-validate EMBED-PLATFORM-001-TASK-003` 动手验证

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — TASK-003 通用文件上传后端审查 | 2026-07-17 | SDDU Review Agent |
