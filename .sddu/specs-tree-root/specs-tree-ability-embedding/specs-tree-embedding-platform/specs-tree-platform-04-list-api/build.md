# 构建输出：列表接口（后端）

> **Feature**: EMBED-PLATFORM-LIST-API-001 | **阶段**: builded  
> **实现人**: SDDU Build Agent | **时间**: 2026-07-20

## 1. 构建概要

| 维度 | 明细 |
|------|------|
| TASK-ID | TASK-004 |
| FR | FR-001（能力目录列表） |
| 复杂度 | M |
| 前置依赖 | TASK-003 ✅ |
| 任务实现数 | 12 个文件（3 修改 + 9 新增） |
| Java 单元测试 | 2 个测试类，13 个测试用例，全部通过 |
| Python 集成测试 | 3 个测试类，18 个测试用例，全部通过 |

## 2. 文件变更

### 修改文件（3 个）

| 文件 | 变更内容 |
|------|---------|
| `AbilityEntity.java` | 新增 6 个字段：entryUrl, hidden, routePath, aliasName, requireRelease, loadType |
| `AbilityMapper.java` | 新增 selectPage() 分页查询方法和 countByKeyword() 计数方法 |
| `AbilityMapper.xml` | resultMap 新增 6 字段映射；Base_Column_List 新增 6 列；新增 selectPage/selectByKeyword SQL |

### 新增文件（9 个）

| 文件 | 说明 |
|------|------|
| `entity/AbilityProperty.java` | 能力属性实体（对应 ability_p_t 表） |
| `mapper/AbilityPropertyMapper.java` | 能力属性 Mapper（market-server 版，批量属性查询） |
| `dto/admin/AdminAbilityListRequest.java` | 列表请求 DTO：分页 + 关键字 + 排序（含字段白名单防 SQL 注入） |
| `vo/admin/AdminAbilityVO.java` | 列表响应 VO：14 个业务字段 + createTime/updateBy/updateTime |
| `service/AdminAbilityService.java` | 管理面能力服务接口 |
| `service/impl/AdminAbilityServiceImpl.java` | 列表查询实现：分页 → 属性表关联 → VO 组装 |
| `controller/AdminAbilityController.java` | GET /service/open/v2/ability/admin/list |
| `AdminAbilityListControllerTest.java` | Controller 层单元测试（MockMvc，6 个用例） |
| `AdminAbilityListServiceTest.java` | Service 层单元测试（Mockito，7 个用例） |

### 新增 Python 测试文件

| 文件 | 用例数 | 覆盖场景 |
|------|:------:|---------|
| `test_admin_list.py:TestAbilityAdminListL1` | 3 | 默认参数 / 翻页 / 字段完整性 |
| `test_admin_list.py:TestAbilityAdminListL2` | 7 | 中文搜索 / 英文搜索 / 排序升降序 / 属性表关联 |
| `test_admin_list.py:TestAbilityAdminListL4` | 8 | 非法排序 / 超长 pageSize / 特殊字符 / 超出分页 |

## 3. 测试覆盖

| 类型 | 测试类 | 用例数 | 状态 |
|------|--------|:------:|:----:|
| Java 单元测试（Controller） | AdminAbilityListControllerTest | 6 | ✅ |
| Java 单元测试（Service） | AdminAbilityListServiceTest | 7 | ✅ |
| Python 集成测试（L1 正常） | TestAbilityAdminListL1 | 3 | ✅ |
| Python 集成测试（L2 规则） | TestAbilityAdminListL2 | 7 | ✅ |
| Python 集成测试（L4 边界） | TestAbilityAdminListL4 | 8 | ✅ |
| **合计** | | **31** | **✅ 全部通过** |

## 4. 关键设计决策

### 4.1 排序字段白名单（防 SQL 注入）

在 `AdminAbilityListRequest.validateSortField()` 中使用白名单校验，允许的排序字段：
`order_num`, `ability_type`, `ability_name_cn`, `ability_name_en`, `create_time`, `last_update_time`

支持驼峰命名自动转换（如 `sortField=createTime` → `create_time`）。

### 4.2 图标/示意图 URL 解析

从 `ability_p_t` 属性表按 `property_name='icon'/'diagram'` 查询 batchId（对应 spec.md §5.3 字段约束表的 `icon`/`diagram` 属性），拼接 `/ability-files/{batchId}` 作为最终 URL。

### 4.3 分页参数保护

- curPage < 1 → 修正为 1
- pageSize > 100 → 限制为 100
- pageSize < 1 → 修正为 1

## 5. 验证执行

```bash
# Java 单元测试（13/13 通过）
mvn -f market-server/pom.xml test -Dtest="AdminAbilityListControllerTest,AdminAbilityListServiceTest"

# Python 集成测试（18/18 通过）
cd market-server/src/test/python && pytest modules/ability/test_admin_list.py -m "" -v
```

## 6. 产物清单

- 源文件：3 修改 + 6 新增 = 9 个 Java 源文件
- 映射文件：1 修改 + 1 新增 = 2 个 XML 映射文件
- 单元测试：2 个 Java 测试类
- 集成测试：1 个 Python 测试文件（3 个测试类）
