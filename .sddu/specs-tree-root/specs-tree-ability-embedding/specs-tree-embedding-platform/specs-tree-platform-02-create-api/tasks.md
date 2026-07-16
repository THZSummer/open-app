# 任务：新增接口（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-005 | 复杂度: M | 波次: 2 | FR: FR-002  
> 前置依赖: TASK-001, TASK-002

## 描述

实现创建能力接口。新建 AdminAbilityCreateRequest（含所有字段 + JSR-303 校验），在 AdminAbilityService 中实现 create()（含编码唯一性校验 + URL 格式校验），写主表 + 属性表（图标/示意图），在 AdminAbilityController 中暴露 POST /ability/admin。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../ability/dto/admin/AdminAbilityCreateRequest.java` |
| MODIFY | `market-server/.../ability/service/AdminAbilityService.java` |
| MODIFY | `market-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |
| MODIFY | `market-server/.../ability/controller/AdminAbilityController.java` |
| MODIFY | `market-server/.../ability/mapper/AbilityPropertyMapper.java` |

## 验收标准

- [ ] abilityType 编码唯一性校验，冲突返回 409 "编码已被占用"
- [ ] entryUrl 格式校验（http/https 协议），不合法返回 400
- [ ] 创建成功写入 ability_t（主表）+ ability_p_t（图标/示意图属性）
- [ ] 接口路径: POST /service/open/v2/ability/admin

## 验证

```bash
mvn -f market-server/pom.xml compile -q
```
