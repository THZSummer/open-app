# 任务：数据库变更

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-002 | 复杂度: S | FR: FR-002 | 前置依赖: TASK-001

## 描述

新增 V4 数据库变更脚本（按 Flyway 命名规范，实际通过 mysql 命令行执行），为 `openplatform_ability_t` 新增 5 个字段（entry_url / hidden / route_path / alias_name / require_release），调整 ability_type 类型为 tinyint。

> ⚠️ 迁移脚本放在 open-server（表 `openplatform_ability_t` 属于 open-server schema，脚本跟随 open-server 管理）。  
> ⚠️ Entity / Mapper 等应用代码同步由 TASK-003（列表接口后端）承接。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql` |

## 验收标准

- [ ] 迁移文件命名为 `V4__add_ability_admin_fields.sql`（遵循 Flyway 命名规范）
- [ ] 新增 5 个字段：entry_url / hidden / route_path / alias_name / require_release
- [ ] ability_type MODIFY 为 tinyint
- [ ] 迁移在测试库实际执行成功

## 验证步骤

> ⚠️ 安全原则：禁止直接操作原库。先在副本库完成全部验证，通过后方可推广到原库。
> 详细流程见 plan.md §10.1（隔离副本策略：root 建新库 → 全量复制 → 副本验证 → 原库执行）。

### 阶段一：副本库验证

```bash
# 1. 创建副本库
mysql -h 192.168.3.155 -P 3306 -u root -proot -e \
  "CREATE DATABASE IF NOT EXISTS openapp_v4_migration_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 全量复制原库到副本库
mysqldump -h 192.168.3.155 -P 3306 -u root -proot --single-transaction openapp \
  | mysql -h 192.168.3.155 -P 3306 -u root -proot openapp_v4_migration_test

# 3. 在副本库执行 V4 脚本
mysql -h 192.168.3.155 -P 3306 -u root -proot openapp_v4_migration_test \
  < open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql

# 4. 验证新字段
mysql -h 192.168.3.155 -P 3306 -u root -proot openapp_v4_migration_test -e \
  "DESC openplatform_ability_t" | grep -E "entry_url|hidden|route_path|alias_name|require_release"

# 5. 验证数据完整性（行数一致）
mysql -h 192.168.3.155 -P 3306 -u root -proot -e \
  "SELECT (SELECT COUNT(*) FROM openapp.openplatform_ability_t) AS original,
          (SELECT COUNT(*) FROM openapp_v4_migration_test.openplatform_ability_t) AS copy;"

# 6. 验证 CRUD 操作正常
mysql -h 192.168.3.155 -P 3306 -u root -proot openapp_v4_migration_test -e \
  "INSERT INTO openplatform_ability_t (id, ability_name_cn, ability_name_en, ability_type, order_num, entry_url, hidden, route_path, alias_name, require_release) VALUES (99999, '验证', 'verify', 250, 0, 'http://example.com', 0, '/verify', 'verify-app', 0);
   UPDATE openplatform_ability_t SET entry_url = 'http://updated.com' WHERE id = 99999;
   SELECT id, entry_url FROM openplatform_ability_t WHERE id = 99999;
   DELETE FROM openplatform_ability_t WHERE id = 99999;"
```

### 阶段二：推广到原库（阶段一全部 ✅ 后执行）

```bash
# 原库执行 V4 脚本
mysql -h 192.168.3.155 -P 3306 -u root -proot openapp \
  < open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql
```

## 验证

```bash
# 原库执行 V4 脚本（副本验证已通过）
mysql -h 192.168.3.155 -P 3306 -u root -proot openapp \
  < open-server/src/main/resources/db/migration/V4__add_ability_admin_fields.sql
```
