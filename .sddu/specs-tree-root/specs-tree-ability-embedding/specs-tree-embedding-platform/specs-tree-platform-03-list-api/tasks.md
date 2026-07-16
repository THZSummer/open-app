# 任务：列表接口（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-003 | 复杂度: M | FR: FR-001  
> 前置依赖: TASK-002

## 描述

实现能力目录分页查询接口。同步 AbilityEntity 实体类新增 5 个字段（entryUrl / hidden / routePath / aliasName / requireRelease）及 Mapper resultMap，新建 AdminAbilityListRequest / AdminAbilityVO，扩展 Mapper 分页查询方法，实现 AdminAbilityService.list()，在 AdminAbilityController 中暴露 GET /ability/admin/list。

> 💡 Entity / Mapper 变更是所有后端任务的公共基础，由本任务（首个创建 VO 的后端任务）承接。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `market-server/.../ability/entity/AbilityEntity.java` |
| MODIFY | `market-server/.../ability/mapper/AbilityMapper.java` |
| NEW | `market-server/.../ability/dto/admin/AdminAbilityListRequest.java` |
| NEW | `market-server/.../ability/vo/admin/AdminAbilityVO.java` |
| NEW | `market-server/.../ability/service/AdminAbilityService.java` |
| NEW | `market-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |
| NEW | `market-server/.../ability/controller/AdminAbilityController.java` |
| NEW | `market-server/src/test/java/.../ability/controller/AdminAbilityListControllerTest.java` |
| NEW | `market-server/src/test/java/.../ability/service/AdminAbilityListServiceTest.java` |
| NEW | `market-server/src/test/python/conftest.py` |
| NEW | `market-server/src/test/python/common/__init__.py` |
| NEW | `market-server/src/test/python/common/client.py` |
| NEW | `market-server/src/test/python/common/db.py` |
| NEW | `market-server/src/test/python/pytest.ini` |
| NEW | `market-server/src/test/python/modules/ability/test_admin_list.py` |

## 验收标准

- [ ] AbilityEntity 新增 5 个字段及 getter/setter
- [ ] Mapper resultMap 同步新增 5 个字段映射
- [ ] 分页查询正常（curPage / pageSize）
- [ ] keyword 模糊搜索按中文名/英文名
- [ ] sortField / sortOrder 排序
- [ ] 返回字段含 entryUrl / routePath / aliasName / requireRelease / hidden
- [ ] 图标/示意图 URL 从属性表关联查询
- [ ] 接口: GET /service/open/v2/ability/admin/list
- [ ] Java 单元测试: AdminAbilityListControllerTest 通过（覆盖分页/搜索/排序/字段映射）
- [ ] Java 单元测试: AdminAbilityListServiceTest 通过
- [ ] Python 集成测试: test_admin_list.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f market-server/pom.xml test -pl . -Dtest="AdminAbilityListControllerTest,AdminAbilityListServiceTest"

# Python 集成测试
cd market-server/src/test/python
pip install -r requirements.txt -q
pytest modules/ability/test_admin_list.py -m "" -v
```
