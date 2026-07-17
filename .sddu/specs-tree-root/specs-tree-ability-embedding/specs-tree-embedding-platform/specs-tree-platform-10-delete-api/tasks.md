# 任务：删除接口（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-010 | 复杂度: S | FR: FR-004  
> 前置依赖: TASK-009

## 描述

实现删除能力接口。在 AdminAbilityService 中实现 delete()，删除前检查 app_ability_relation_t 是否有订阅，有关联订阅返回 409，无关联则删除主表+属性表记录。在 AdminAbilityController 中暴露 DELETE /ability/admin/{id}。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `market-server/.../ability/service/AdminAbilityService.java` |
| MODIFY | `market-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |
| MODIFY | `market-server/.../ability/controller/AdminAbilityController.java` |
| NEW | `market-server/src/test/java/.../ability/controller/AdminAbilityDeleteControllerTest.java` |
| NEW | `market-server/src/test/java/.../ability/service/AdminAbilityDeleteServiceTest.java` |
| NEW | `market-server/src/test/java/.../ability/entity/AbilityEntityTest.java` |
| NEW | `market-server/src/test/python/modules/ability/test_admin_delete.py` |

## 验收标准

- [ ] 无关联订阅 → 删除主表 + 属性表，返回 200
- [ ] 有关联订阅 → 禁止删除，返回 409 + 订阅数量
- [ ] 不存在的 id 返回 404
- [ ] 接口路径: DELETE /service/open/v2/ability/admin/{id}
- [ ] Java 单元测试: AdminAbilityDeleteControllerTest 通过（覆盖正常删除/有订阅禁止删除）
- [ ] Java 单元测试: AdminAbilityDeleteServiceTest 通过
- [ ] Java 单元测试: AbilityEntityTest 通过（entryUrl/hidden/routePath/aliasName/requireRelease 字段映射）
- [ ] Python 集成测试: test_admin_delete.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f market-server/pom.xml test -Dtest="AdminAbilityDeleteControllerTest,AdminAbilityDeleteServiceTest,AbilityEntityTest"

# Python 集成测试
cd market-server/src/test/python
pytest modules/ability/test_admin_delete.py -m "" -v
```
