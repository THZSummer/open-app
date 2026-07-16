# 任务：模块重组

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-002 | 复杂度: M | 波次: 1

## 描述

将 market-server 中寄生在 `approval` 模块的 ability 相关代码独立为 `modules/ability/` 模块，修正历史代码结构问题，为后续 Admin CRUD 铺平道路。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../modules/ability/` (新模块目录) |
| MOVE | `approval/entity/AbilityEntity.java` → `ability/entity/AbilityEntity.java` |
| MOVE | `approval/mapper/AbilityMapper.java` → `ability/mapper/AbilityMapper.java` |
| MODIFY | `market-server/src/main/resources/mapper/AbilityMapper.xml` (namespace 更新为 `...ability.mapper.AbilityMapper`) |
| MODIFY | 全局替换 import `...approval.entity.AbilityEntity` → `...ability.entity.AbilityEntity` |
| MODIFY | 全局替换 import `...approval.mapper.AbilityMapper` → `...ability.mapper.AbilityMapper` |

## 验收标准

- [ ] `modules/ability/entity/AbilityEntity.java` 存在，包名正确
- [ ] `modules/ability/mapper/AbilityMapper.java` 存在，包名正确
- [ ] `AbilityMapper.xml` namespace 已更新
- [ ] 全项目无残留 `import ...approval...Ability*`

## 验证

```bash
# 1. 确认新包结构存在
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/entity/AbilityEntity.java
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/mapper/AbilityMapper.java

# 2. 确认无残留的 approval import（应无输出）
grep -r "approval\.entity\.AbilityEntity\|approval\.mapper\.AbilityMapper" market-server/src/main/java/ || echo "No stale imports - OK"

# 3. 编译检查
mvn -f market-server/pom.xml compile -q
```
