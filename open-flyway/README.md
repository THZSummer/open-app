# open-flyway — 开发环境数据库迁移工程

独立 Maven 工程，用 Flyway 统一管理**开发环境** openapp 库的 DDL。

> **为什么独立成工程**：open-server / market-server / api-server / event-server / connector-api 共用同一个 openapp 库。不在各服务工程内追加 Flyway（避免标准环境行为变化），统一由本工程管理开发库演进。标准环境脚本（各服务 `db/migration/`）保持独立，迁移时由开发人员自行判断。

## 快速开始

> ⚠️ **安全默认**：本工程默认连接**临时/测试库**（`openapp_flyway_test`），避免误操作影响正式库。对开发库/正式库执行迁移时，**必须显式指定连接**。

```bash
# 查看迁移状态（不执行，默认连临时库）
mvn -f open-flyway/pom.xml flyway:info

# 在临时库执行迁移（验证脚本）
mvn -f open-flyway/pom.xml flyway:migrate

# 对开发库/正式库执行迁移（显式指定连接！）
mvn -f open-flyway/pom.xml flyway:migrate \
  -Ddb.url="jdbc:mysql://192.168.3.155:3306/openapp?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -Ddb.user=openapp -Ddb.password=openapp
```

## 存量开发库接入（baseline）

开发库已有表但无 `flyway_schema_history` 时，`baseline-on-migrate: true` 会自动执行 baseline：

- `baseline-version: 7`（pom.xml 中 `flyway.baselineVersion`）——标记 V1~V7 已应用，**不重跑**历史脚本
- 之后 Flyway 只执行 V8+ 增量脚本
- 全新空库：Flyway 自动从 V1 全量执行（空库无需 baseline）

> ⚠️ baseline 只标记版本，不校验库结构与脚本是否完全一致。若开发库结构与 V1~V7 有历史漂移，需开发人员自行核对。

## 脚本规范

| 版本 | 来源 | 内容 |
|------|------|------|
| V1 | docs/app 早期表（Navicat 导出） | 早期 16 表合并：应用域(7) + 能力域(2) + 数据字典(2) + 运维域(5) |
| V2 | open-server `db/migration/V1` | 能力开放平台基础 schema（分类/API/事件/回调/权限/审批等 15 表） |
| V3 | open-server `db/migration/V2` | 连接器平台 schema（connector/flow 4 表） |
| V4 | open-server `db/migration/V3` | 连接器平台 V3 schema（执行记录/步骤等） |
| V5 | open-server `db/migration/V4` | ability 管理字段 |
| V6 | open-server `db/migration/V5` | common file 表 |
| V7 | market-server `db/migration/V2` | lookup 文件表（原 market-server V2，统一编号避免版本冲突） |

> **V1 提取说明**：源自 `docs/app/*.sql`（Navicat Premium 从 MySQL 8.4 导出）。仅提取**表结构 DDL**（不含 INSERT 数据），`CREATE TABLE` 统一为 `IF NOT EXISTS`（开发库已有表时安全跳过），collation 已从 8.x 的 `utf8mb4_0900_ai_ci` 转为 5.7 兼容的 `utf8mb4_unicode_ci`。

**开发流程**：
1. 新 DDL 变更 → 新增 `V{n+1}__描述.sql`（append-only，**严禁修改已发布的 V*.sql**）
2. 执行 `mvn flyway:migrate` 应用到开发库
3. 迁移到标准环境时：由开发人员自行判断如何同步（复制到对应服务 `db/migration/` 并注意标准环境版本号/checksum 校验）

## 常用命令

```bash
mvn -f open-flyway/pom.xml flyway:validate   # 校验脚本 checksum（标准环境同步前自查）
mvn -f open-flyway/pom.xml flyway:repair     # 修复 history（谨慎使用，仅确认漂移时）
mvn -f open-flyway/pom.xml flyway:clean      # 清空 schema（危险！勿用于有数据环境）
```
