# 批 ability：open-server / ability 审查报告

> 15 文件全部逐行读。意见按 §2.2 格式。

## 文件覆盖表（15/15）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| constants/AbilityPropertyConstants.java(23) | ✅ | 0 |
| controller/AbilityController.java(69) | ✅ | 0 |
| dto/AddAbilityRequest.java(21) | ✅ | 0 |
| entity/Ability.java(80) | ✅ | 0 |
| entity/AbilityProperty.java(65) | ✅ | 0 |
| entity/AppAbilityRelation.java(73) | ✅ | 0 |
| enums/AbilityTypeEnum.java(38) | ✅ | 0 |
| mapper/AbilityMapper.java(18) | ✅ | 0 |
| mapper/AbilityPropertyMapper.java(19) | ✅ | 0 |
| mapper/AppAbilityRelationMapper.java(27) | ✅ | 0 |
| service/AbilityService.java(22) | ✅ | 0 |
| service/impl/AbilityServiceImpl.java(238) | ✅ | 2 |
| snapshot/AbilitySnapshotLoader.java(66) | ✅ | 0 |
| vo/AbilityVO.java(67) | ✅ | 0 |
| vo/AppAbilityDetailVO.java(52) | ✅ | 0 |

## QC 意见（2 条）

### 意见 1
- 大类：业务功能
- 子类：功能需求遗漏
- 级别：一般
- 问题原因：service/impl/AbilityServiceImpl.java:170-172 `autoSubscribeAfterAbility` TODO 空实现。订阅能力后应自动订阅对应 API/事件权限，当前空实现 → 业务可能不完整
- 修改建议：实现自动订阅逻辑，或确认无下游依赖

### 意见 2
- 大类：基本代码问题
- 子类：代码逻辑错误
- 级别：一般
- 问题原因：service/impl/AbilityServiceImpl.java:143 `abilityId = ability!=null ? ability.getId() : (long)abilityType`。主表缺失时用 abilityType(int) 强转 Long 当 ID，潜在数据不一致（relation 表 abilityId 与 ability 主表 id 不匹配）
- 修改建议：主表缺失时抛异常，不用 abilityType 兜底
- **✅ 已修复（2026-06-30）**：主表缺失时抛 `BusinessException(ABILITY_TYPE_INVALID)`，`abilityId = ability.getId()`，不再用 abilityType 兜底

## 批次结论

- 一般：2

**亮点**：AbilityServiceImpl 每方法 resolveAndValidate 成员校验 ✅；loadPropsMap 批量查属性(selectByParentIds)避免 N+1 ✅；getSubscribedAbilities 按 orderNum 排序；AbilitySnapshotLoader override loadAfterData 从方法参数提取(无 resourceId 场景)；AddAbilityRequest @NotNull 校验；AbilityTypeEnum isValidCode；@Transactional 事务完整。

**有条件通过**：仅 2 个一般级问题，无严重。
