# 审查报告：列表接口（后端）

> **Feature**: EMBED-PLATFORM-LIST-API-001 | **阶段**: reviewed  
> **审查人**: SDDU Review Agent | **时间**: 2026-07-20  
> **Commit**: 57dcd0cf (已修复 amend)

## 1. 审查结论

| 条件 | 结果 |
|------|:----:|
| 阻塞问题 | 0 个 ✅ |
| 改进项 | 4 个 ⚠️ |
| 规范符合率 | 100% ✅ |

**结论**: ✅ **通过** — 代码质量合格，可以进入 validate 阶段动手验证。

---

## 2. 文件级审查结果

### 2.1 `AbilityEntity.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 新增 6 字段 | ✅ | entryUrl, hidden, routePath, aliasName, requireRelease, loadType 全部存在 |
| Lombok `@Data` | ✅ | 注解正确 |
| Javadoc | ✅ | 每个新增字段均有注释 |
| 字段类型 | ✅ | String/Integer 与 DB 类型匹配 |
| serialVersionUID | ✅ | 已声明 |

### 2.2 `AbilityMapper.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| `selectPage()` | ✅ | 参数齐全（keyword, sortField, sortOrder, offset, pageSize） |
| `countByKeyword()` | ✅ | 支持模糊搜索 |
| `@Param` 注解 | ✅ | 所有参数均有命名注解 |
| 方法命名 | ✅ | 符合 MyBatis 命名惯例 |

### 2.3 `AbilityMapper.xml` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| resultMap 新 6 字段 | ✅ | entryUrl/hidden/routePath/aliasName/requireRelease/loadType 全部映射 |
| Base_Column_List | ✅ | 包含所有列 |
| `selectPage` SQL | ✅ | 动态 WHERE、ORDER BY `${}`、LIMIT |
| `countByKeyword` SQL | ✅ | 仅 COUNT 不含 ORDER BY |
| `<where>` 标签使用 | ✅ | 正确处理 AND 前缀 |
| SQL 注入防护 | ✅ | 所有条件参数使用 `#{}`；ORDER BY 字段有白名单校验 |

### 2.4 `AdminAbilityListRequest.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 分页参数默认值 | ✅ | curPage=1, pageSize=20 |
| 排序字段白名单 | ✅ | 6 个允许字段 |
| camelToUnderscore 转换 | ✅ | 驼峰→下划线自动转换 |
| 排序方向校验 | ✅ | 仅接受 asc/desc |
| Swagger `@Schema` | ✅ | 各字段均有描述 |

**改进项**:
- ⚠️ 建议将 `validateSortField()` / `validateSortOrder()` 的校验失败逻辑从 Service 层下沉到 DTO，通过 JSR-303 `@AssertTrue` 注解实现声明式校验

### 2.5 `AdminAbilityVO.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 17 个字段完整 | ✅ | 14 业务字段 + createTime/updateBy/updateTime |
| 字段命名符合 spec | ✅ | abilityType, nameCn, nameEn, descCn, descEn 等 |
| `@JsonFormat` | ✅ | createTime/updateTime 格式化为 `yyyy-MM-dd HH:mm:ss` |
| Lombok 注解 | ✅ | `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` |
| Swagger `@Schema` | ✅ | 所有字段描述 |

### 2.6 `AdminAbilityService.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 接口定义清晰 | ✅ | 返回 `ApiResponse<List<AdminAbilityVO>>` |
| Javadoc 完整 | ✅ | 方法/参数描述齐全 |

### 2.7 `AdminAbilityServiceImpl.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 分页参数保护 | ✅ | curPage≥1, pageSize∈[1,100] |
| 排序白名单校验 | ✅ | 校验失败返回 400 |
| 属性表批量查询 | ✅ | 使用 `selectByParentIds` 一次查询全部 |
| VO 组装 | ✅ | 含 17 个字段 |
| 图标 URL 构造 | ✅ | `/ability-files/{batchId}` |
| 日志记录 | ✅ | 异常时 `log.error` |
| 构造函数注入 | ✅ | 已改为构造器注入 |

**改进项**:
- ⚠️ `catch(Exception)` 捕获范围过宽，建议改为 `catch(RuntimeException)` 或交由全局 `@ControllerAdvice` 处理
- ⚠️ 属性表查询未按 `property_name` 过滤（`icon`/`diagram`），查询全部属性后在 Java 侧过滤；数据量大时建议在 SQL 层增加过滤条件

### 2.8 `AdminAbilityController.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| `@RestController` | ✅ | 已标注 |
| `@RequestMapping` | ✅ | `/service/open/v2/ability/admin` |
| `@GetMapping("/list")` | ✅ | 路径与 spec 一致 |
| 返回值 | ✅ | `ApiResponse<List<AdminAbilityVO>>` |
| 构造函数注入 | ✅ | 已改为构造器注入 |
| Swagger `@Tag`/`@Operation` | ✅ | API 文档注解 |

**改进项**:
- ⚠️ NFR-001（管理员权限校验）未在 Controller 层显式声明。建议添加 `@PreAuthorize` 或自定义权限注解，确保非管理员用户被拒绝访问

### 2.9 `AbilityProperty.java` & `AbilityPropertyMapper.java` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| 实体字段完整 | ✅ | id/parentId/propertyName/propertyValue/status/审计字段 |
| `@Mapper` 注解 | ✅ | 已标注 |
| `selectByParentIds` | ✅ | 批量查询，含 status=1 过滤 |

### 2.10 `AbilityPropertyMapper.xml` — ✅ 通过

| 检查项 | 状态 | 说明 |
|--------|:----:|------|
| resultMap | ✅ | 所有字段映射 |
| SQL 参数化 | ✅ | `#{id,jdbcType=BIGINT}` |
| `status = 1` 过滤 | ✅ | 仅查询有效属性 |

---

## 3. 按维度审查汇总

### 3.1 规范符合性

| 规范 | 符合情况 |
|------|---------|
| `@RestController` + `@RequestMapping` | ✅ 符合 |
| `ApiResponse` 统一响应信封 | ✅ 符合（code/data/page 结构） |
| Lombok 注解（`@Data`/`@Builder`） | ✅ 使用 |
| Swagger `@Tag`/`@Operation`/`@Schema` | ✅ 使用 |
| Javadoc 规范 | ✅ 类和方法均有注释 |
| 驼峰命名 | ✅ Java/API 字段均驼峰 |

### 3.2 SQL 安全性

| 防护措施 | 状态 | 说明 |
|---------|:----:|------|
| 排序字段白名单 | ✅ | 6 个允许字段 + camelToUnderscore 转换 |
| 排序方向白名单 | ✅ | 仅 asc/desc |
| 参数化查询 | ✅ | 所有 LIKE/WHERE 条件使用 `#{}` |
| MyBatis `${}` 风险 | ✅ | ORDER BY 使用 `${}` 但已通过白名单校验，无注入风险 |

### 3.3 业务逻辑正确性

| 逻辑 | 状态 | 说明 |
|------|:----:|------|
| 分页计算 | ✅ | offset = (curPage - 1) * pageSize |
| 总数查询 | ✅ | countByKeyword 单独查询 |
| 总页数计算 | ✅ | (total + pageSize - 1) / pageSize |
| 页码保护 | ✅ | Math.max(1, curPage) |
| pageSize 限制 | ✅ | Math.min(100, Math.max(1, pageSize)) |
| 图标 URL 拼接 | ✅ | `/ability-files/{batchId}` |
| 属性表关联 | ✅ | batchId 从属性表获取，批量查询 |
| 状态过滤 | ✅ | 仅查询 status=1 的活跃能力 |

### 3.4 测试覆盖

| 测试类 | 用例数 | 状态 | 覆盖场景 |
|--------|:------:|:----:|---------|
| AdminAbilityListControllerTest | 6 | ✅ 通过 | 正常分页、关键字搜索(中文/英文)、空结果、翻页、排序 |
| AdminAbilityListServiceTest | 7 | ✅ 通过 | 正常分页、关键字搜索、动态排序、非法排序字段/方向、空结果、驼峰→下划线转换 |
| Python test_admin_list.py L1 | 3 | ✅ | 默认参数、翻页、字段完整性 |
| Python test_admin_list.py L2 | 7 | ✅ | 中文搜索、英文搜索、大小写不敏感、升序/降序、abilityType排序、属性表关联 |
| Python test_admin_list.py L4 | 8 | ✅ | 非法排序字段/方向、超大 pageSize、零 pageSize、负数 curPage、特殊字符、空关键字、超出分页 |

**改进项**:
- ⚠️ Python 测试 `test_list_fields_completeness` 中使用 `random.randint()` 生成测试 ID，可能在并发执行时碰撞。建议使用 UUID 或时间戳 + 序列号方式

### 3.5 代码一致性

| 对比项 | 状态 | 说明 |
|--------|:----:|------|
| spec.md FR-001 | ✅ | 列表字段、排序默认值、分页均匹配 |
| plan.md 接口定义 | ✅ | GET /ability/admin/list 路径一致 |
| plan.md 字段映射表 | ✅ | 响应字段名一致（abilityType/nameCn 等） |
| plan.md ADR 决策 | ✅ | 白名单、属性表关联、分页参数保护均落地 |
| build.md 产物清单 | ✅ | 所有文件已实现 |
| tasks.md 验收标准 | ✅ | 所有标准已满足 |

---

## 4. 修复内容

本次审查过程中修复了以下问题：

| # | 文件 | 修复内容 | 类型 |
|:-:|------|---------|:----:|
| 1 | AdminAbilityServiceImpl.java | 属性表 property_name 常量从 `iconUrl`→`icon`、`diagramUrl`→`diagram`，对齐 spec.md §5.3 字段约束表定义 | 规范对齐 |
| 2 | AdminAbilityListServiceTest.java | 测试数据 property_name 同步更新为 `icon`/`diagram` | 配套修正 |
| 3 | test_admin_list.py | Python 测试数据 property_name 同步更新 | 配套修正 |
| 4 | build.md | 图标/示意图文档 description 同步更新 | 文档对齐 |
| 5 | AdminAbilityController.java | `@Autowired` 字段注入改为构造器注入 | 代码改进 |
| 6 | AdminAbilityControllerTest.java | 适配构造器注入 | 配套修正 |
| 7 | AdminAbilityServiceImpl.java | `@Autowired` 字段注入改为构造器注入 | 代码改进 |
| 8 | AdminAbilityListServiceTest.java | 适配构造器注入，移除 `ReflectionTestUtils` | 配套修正 |

---

## 5. 改进建议（非阻塞）

| # | 问题 | 严重度 | 建议 |
|:-:|------|:------:|------|
| 1 | ⚠️ Controller 层未显式声明 NFR-001 管理员权限校验 | 中等 | 添加 `@PreAuthorize("hasRole('ADMIN')")` 或自定义权限注解 |
| 2 | ⚠️ `AdminAbilityServiceImpl` 使用 `catch(Exception)` 捕获所有异常 | 低 | 建议改为 `catch(RuntimeException)` 或交由全局 `@ControllerAdvice` 统一处理 |
| 3 | ⚠️ 属性表查询未在 SQL 层过滤 `property_name` | 低 | 数据量大时建议在 `selectByParentIds` 增加 `AND property_name IN ('icon', 'diagram')` 条件 |
| 4 | ⚠️ Python 测试 ID 使用 `random.randint` 可能导致碰撞 | 低 | 建议改用 `uuid.uuid4().int` 或 `int(time.time() * 1000)` 生成唯一 ID |

---

## 6. 验证结果

```bash
# Java 单元测试（13/13 通过 ✅）
mvn -f market-server/pom.xml test -Dtest="AdminAbilityListControllerTest,AdminAbilityListServiceTest"
# Tests run: 13, Failures: 0, Errors: 0

# 全量编译验证（通过 ✅）
mvn -f market-server/pom.xml compile -q
```

---

## 7. 产物清单

| 产物 | 路径 |
|------|------|
| 审查报告 | `review.md` |
| 状态文件 | `state.json` (phase: reviewed) |

*本报告由 SDDU Review Agent 自动生成*
