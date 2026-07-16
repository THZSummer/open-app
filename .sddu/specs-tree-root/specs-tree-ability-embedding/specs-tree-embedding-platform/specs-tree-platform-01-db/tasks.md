# 任务：数据库变更

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-001 | 复杂度: S | 波次: 1 | FR: FR-002

## 描述

新增 Flyway V4 迁移脚本，为 `openplatform_ability_t` 新增 5 个字段（entry_url / hidden / route_path / alias_name / require_release），调整 ability_type 类型为 tinyint。

> ⚠️ 迁移脚本放在 open-server（表 `openplatform_ability_t` 属于 open-server schema，Flyway 在此管理）。  
> ⚠️ Entity / Mapper 等应用代码同步由 TASK-003（列表接口后端）承接。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql` |

## 验收标准

- [ ] 迁移文件命名为 `V4__add_ability_admin_fields.sql`，Flyway 可识别
- [ ] 新增 5 个字段：entry_url / hidden / route_path / alias_name / require_release
- [ ] ability_type MODIFY 为 tinyint
- [ ] 迁移在测试库实际执行成功

## 验证

```bash
# 1. 编译检查
mvn -f open-server/pom.xml compile -q

# 2. 执行迁移
mvn -f open-server/pom.xml flyway:migrate

# 3. 验证新字段（预期输出 5 行）
mysql -e "DESC openplatform_ability_t" openapp | grep -E "entry_url|hidden|route_path|alias_name|require_release"
```
