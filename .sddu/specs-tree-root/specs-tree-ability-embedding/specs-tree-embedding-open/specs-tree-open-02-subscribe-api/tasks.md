# 任务：能力订阅增强 + 自动桥接

> 父 Feature: 嵌入能力开放面（EMBED-OPEN-001）  
> 对应任务: TASK-002 | 复杂度: M | FR: FR-002, FR-003  
> 前置依赖: 无（与 TASK-001 并行，复用其 VO 字段）

## 描述

修改能力订阅接口的校验逻辑和自动桥接扩展点。订阅校验从硬编码枚举 `AbilityTypeEnum.isValidCode()` 改为查询 DB 校验能力存在且启用；`autoSubscribeAfterAbility()` 从空实现改为记录日志，预留后续权限桥接钩子。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/.../ability/service/impl/AbilityServiceImpl.java` |
| NEW | `open-server/src/test/java/.../ability/service/AbilitySubscribeServiceTest.java` |
| NEW | `open-server/src/test/python/modules/ability/test_open_subscribe.py` |

## 验收标准

- [ ] 校验逻辑：移除 `AbilityTypeEnum.isValidCode()` 枚举校验
- [ ] 校验逻辑：改为查询 DB 校验 ability_t 中存在且 status=1
- [ ] 不存在或已失效返回 400 "能力不存在或已失效"
- [ ] 重复订阅检查逻辑不变
- [ ] 关联记录插入逻辑不变
- [ ] autoSubscribeAfterAbility 空实现 → `log.info("Auto-subscribe bridge triggered, appId={}, abilityType={}", ...)`
- [ ] 预留钩子：方法签名不变，后续可扩展
- [ ] 接口路径和请求参数不变（POST /service/open/v2/ability?appId=X）
- [ ] Java 单元测试: AbilitySubscribeServiceTest 通过（自定义类型通过/失效类型拒绝/日志断言）
- [ ] Python 集成测试: test_open_subscribe.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f open-server/pom.xml test -Dtest="AbilitySubscribeServiceTest"

# Python 集成测试
cd open-server/src/test/python
pytest modules/ability/test_open_subscribe.py -v
```
