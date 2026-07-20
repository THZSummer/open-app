# 任务：已订阅列表增强

> 父 Feature: 嵌入能力开放面（EMBED-OPEN-001）  
> 对应任务: TASK-003 | 复杂度: S | FR: FR-004  
> 前置依赖: 无（与 TASK-001/002 并行，复用 TASK-001 的 VO 字段）

## 描述

修改已订阅列表接口的返回字段。AppAbilityDetailVO 新增 5 个字段（entryUrl/routePath/aliasName/requireRelease/loadType）；移除硬编码排除 type=6（已订阅的不受 hidden 影响）。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/.../ability/vo/AppAbilityDetailVO.java` |
| MODIFY | `open-server/.../ability/service/impl/AbilityServiceImpl.java` |
| NEW | `open-server/src/test/java/.../ability/vo/AppAbilityDetailVOTest.java` |
| NEW | `open-server/src/test/java/.../ability/service/AbilitySubscribedServiceTest.java` |
| NEW | `open-server/src/test/python/modules/ability/test_open_subscribed.py` |

## 验收标准

- [ ] AppAbilityDetailVO 新增 entryUrl/routePath/aliasName/requireRelease/loadType 5 个字段（optional）
- [ ] VO 映射增加 5 新字段（有则返，无则 null）
- [ ] 移除硬编码排除 type=6（已订阅的不受 hidden 影响）
- [ ] 接口路径和请求参数不变（GET /service/open/v2/ability/subscribed?appId=X）
- [ ] Java 单元测试: AppAbilityDetailVOTest 通过（序列化）
- [ ] Java 单元测试: AbilitySubscribedServiceTest 通过（新字段 + 已订阅完整返回）
- [ ] Python 集成测试: test_open_subscribed.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f open-server/pom.xml test -Dtest="AppAbilityDetailVOTest,AbilitySubscribedServiceTest"

# Python 集成测试
cd open-server/src/test/python
pytest modules/ability/test_open_subscribed.py -v
```
