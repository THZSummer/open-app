# 任务：能力列表增强

> 父 Feature: 嵌入能力开放面（EMBED-OPEN-001）  
> 对应任务: TASK-001 | 复杂度: M | FR: FR-001, FR-005  
> 前置依赖: 无

## 描述

修改能力列表接口的内部逻辑和返回字段。AbilityVO 新增 5 个字段（entryUrl/routePath/aliasName/requireRelease/loadType）作为公共基础；过滤逻辑从硬编码 `abilityType != 6` 改为 `hidden = 0`；自定义类型不再受 `AbilityTypeEnum` 限制。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/.../ability/vo/AbilityVO.java` |
| MODIFY | `open-server/.../ability/service/impl/AbilityServiceImpl.java` |
| NEW | `open-server/src/test/java/.../ability/vo/AbilityVOTest.java` |
| NEW | `open-server/src/test/java/.../ability/service/AbilityListServiceTest.java` |
| NEW | `open-server/src/test/python/modules/ability/test_open_list.py` |

## 验收标准

- [ ] AbilityVO 新增 entryUrl/routePath/aliasName/requireRelease/loadType 5 个字段（optional，null 安全）
- [ ] 过滤逻辑：移除 `abilityType != 6` 硬编码排除 → 改为 `WHERE hidden = 0`
- [ ] 自定义类型：DB 中所有 `status=1` 的能力均返回，不依赖 AbilityTypeEnum
- [ ] 返回字段含 5 新字段（有则返，无则 null）
- [ ] 已订阅标记逻辑不变
- [ ] 接口路径和请求参数不变（GET /service/open/v2/ability/list?appId=X）
- [ ] Java 单元测试: AbilityVOTest 通过（序列化/反序列化）
- [ ] Java 单元测试: AbilityListServiceTest 通过（hidden过滤 + 新字段 + 自定义类型）
- [ ] Python 集成测试: test_open_list.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f open-server/pom.xml test -Dtest="AbilityVOTest,AbilityListServiceTest"

# Python 集成测试
cd open-server/src/test/python
pytest modules/ability/test_open_list.py -v
```
