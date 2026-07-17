# 任务：编辑接口（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-007 | 复杂度: M | FR: FR-003  
> 前置依赖: TASK-006

## 描述

实现编辑能力接口。新建 AdminAbilityUpdateRequest（所有字段可选，abilityType 不可修改），在 AdminAbilityService 中实现 update()（部分更新，乐观锁，含 loadType 联动校验），在 AdminAbilityController 中暴露 PUT /ability/admin/{id}。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../ability/dto/admin/AdminAbilityUpdateRequest.java` |
| MODIFY | `market-server/.../ability/service/AdminAbilityService.java` |
| MODIFY | `market-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |
| MODIFY | `market-server/.../ability/controller/AdminAbilityController.java` |
| NEW | `market-server/src/test/java/.../ability/controller/AdminAbilityUpdateControllerTest.java` |
| NEW | `market-server/src/test/java/.../ability/service/AdminAbilityUpdateServiceTest.java` |
| NEW | `market-server/src/test/python/modules/ability/test_admin_update.py` |

## 验收标准

- [ ] 所有字段可选，仅更新传入字段
- [ ] abilityType 不可修改
- [ ] loadType 改为 2 时校验 entryUrl/routePath/aliasName 三要素必填
- [ ] 乐观锁处理（基于 last_update_time），冲突提示"数据已被修改，请刷新后重试"
- [ ] 不存在的 id 返回 404
- [ ] 接口路径: PUT /service/open/v2/ability/admin/{id}
- [ ] Java 单元测试: AdminAbilityUpdateControllerTest 通过（覆盖部分更新/abilityType不可改/乐观锁冲突）
- [ ] Java 单元测试: AdminAbilityUpdateServiceTest 通过
- [ ] Python 集成测试: test_admin_update.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f market-server/pom.xml test -Dtest="AdminAbilityUpdateControllerTest,AdminAbilityUpdateServiceTest"

# Python 集成测试
cd market-server/src/test/python
pytest modules/ability/test_admin_update.py -m "" -v
```
