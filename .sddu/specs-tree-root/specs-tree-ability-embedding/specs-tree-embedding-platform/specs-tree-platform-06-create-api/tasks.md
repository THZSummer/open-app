# 任务：新增接口（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-006 | 复杂度: M | FR: FR-002  
> 前置依赖: TASK-005

## 描述

实现创建能力接口。新建 AdminAbilityCreateRequest（含所有字段 + JSR-303 校验），在 AdminAbilityService 中实现 create()（含编码唯一性校验 + URL 格式校验 + load_type 联动校验），写主表 + 属性表（图标/示意图），在 AdminAbilityController 中暴露 POST /ability/admin。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../ability/dto/admin/AdminAbilityCreateRequest.java` |
| MODIFY | `market-server/.../ability/service/AdminAbilityService.java` |
| MODIFY | `market-server/.../ability/service/impl/AdminAbilityServiceImpl.java` |
| MODIFY | `market-server/.../ability/controller/AdminAbilityController.java` |
| MODIFY | `market-server/.../ability/mapper/AbilityPropertyMapper.java` |
| NEW | `market-server/src/test/java/.../ability/controller/AdminAbilityCreateControllerTest.java` |
| NEW | `market-server/src/test/java/.../ability/service/AdminAbilityCreateServiceTest.java` |
| NEW | `market-server/src/test/python/modules/ability/test_admin_create.py` |

## 验收标准

- [x] abilityType 编码唯一性校验，冲突返回 409 "编码已被占用"
- [x] entryUrl 格式校验（http/https 协议），不合法返回 400
- [x] 名称校验：nameCn/nameEn 必填，2-30 字符，不满足返回 400
- [x] 描述校验：descCn/descEn 必填，5-200 字符，不满足返回 400
- [x] 图标必填校验：iconBatchId 必填，缺失返回 400 "图标为必填项"
- [x] 示意图格式校验（若提供）：diagramUrl 非必填，提供时校验文件格式/尺寸/大小
- [x] 排序号校验：orderNum 必填，正整数 ≥1，默认当前最大值+1
- [x] loadType=2 时 entryUrl/routePath/aliasName 三要素必填校验，缺失返回 400 "微前端加载模式下三要素必填"
- [x] 创建成功写入 ability_t（主表）+ ability_p_t（图标/示意图属性）
- [x] 接口路径: POST /service/open/v2/ability/admin
- [x] Java 单元测试: AdminAbilityCreateControllerTest 通过（覆盖正常创建/编码唯一性/URL校验/名称长度/描述长度/图标必填/排序号）
- [x] Java 单元测试: AdminAbilityCreateServiceTest 通过
- [x] Python 集成测试: test_admin_create.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f market-server/pom.xml test -Dtest="AdminAbilityCreateControllerTest,AdminAbilityCreateServiceTest"

# Python 集成测试
cd market-server/src/test/python
pytest modules/ability/test_admin_create.py -m "" -v
```
