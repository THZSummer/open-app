# 数据库设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.2)  
**版本**: v2.0  
**创建日期**: 2026-05-21  
**更新说明**: 对齐 spec.md v4.0——移除审批字段、更新执行状态枚举、新增 data_processor 节点类型、更新触发方式枚举、移除 MQS 引用

---

## 表清单

| 表名 | 类型 | 归属模块 | 说明 |
|------|------|---------|------|
| `cp_connector` | 主表 | connector | 连接器基本信息 |
| `cp_connector_version` | 主表 | connector | 连接器版本信息（含连接配置快照） |
| `cp_flow` | 主表 | flow | 连接流基本信息 |
| `cp_flow_version` | 主表 | flow | 连接流版本信息（含编排配置快照） |
| `cp_flow_node` | 子表 | flow | 流节点定义（entry/connector/data_processor/exit） |
| `cp_flow_edge` | 子表 | flow | 流连线定义（含数据映射） |
| `cp_execution_record` | 主表 | runtime | 执行记录 |
| `cp_execution_step` | 子表 | runtime | 执行步骤详情 |
| `cp_connector_auth_config` | 主表 | runtime | 连接器认证凭证（AES-256-GCM 加密存储） |

**总计**：9 张表（5 张主表 + 4 张子表），分属 connector / flow / runtime 三个模块。

---

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| 表前缀 | 统一使用 `cp_`（Connector Platform） |
| 主键 | 统一使用 `bigint auto_increment`，业务 ID 通过 `varchar(32)` 独立字段存储 |
| 时间字段 | 统一使用 `datetime(3)`（毫秒精度） |
| JSON 字段 | 统一使用 `json` 类型（MySQL 5.7+ 原生支持） |
| 软删除 | 关键业务表（connector/flow）支持 `is_deleted` 标记 |
| 审计字段 | 每表包含 `created_at`, `created_by`, `updated_at`, `updated_by` |

---

## 2. 通用数据库设计规范

> 💡 以下规范沿用能力开放平台（CAP-OPEN-001 `plan.md §4.2`）已确立的数据库设计标准，连接器平台保持一致。

### 2.1 命名规范

| 规则 | 说明 | 连接器平台示例 |
|------|------|---------------|
| **表前缀** | 能力平台使用 `openplatform_v2_`，连接器平台使用 `cp_` | `cp_connector` |
| **命名风格** | 小写字母 + 下划线分隔 | `cp_execution_record` |
| **索引命名** | `idx_字段名` 或 `idx_字段名1_字段名2` | `idx_connector_id`, `idx_flow_id_status` |
| **唯一索引命名** | `uk_字段名` | `uk_connector_id`, `uk_version_id` |

### 2.2 主键规范

| 规则 | 说明 |
|------|------|
| **主键类型** | BIGINT，自增 |
| **主键命名** | 统一使用 `id` |
| **业务ID** | 通过独立 `varchar(32)` 字段存储业务标识（如 `connector_id`）|
| **关联字段** | 使用逻辑外键（存储关联 ID），**不使用物理外键约束**，关联关系由应用层维护 |

> **禁止使用外键**：所有表关联关系通过存储逻辑字段实现，不使用数据库物理外键约束（FOREIGN KEY）。

### 2.3 审计字段规范

所有业务表必须包含以下审计字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `created_at` | DATETIME(3) | 创建时间，默认 `CURRENT_TIMESTAMP(3)` |
| `updated_at` | DATETIME(3) | 更新时间，默认 `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` |
| `created_by` | VARCHAR(64) | 创建人账号 |
| `updated_by` | VARCHAR(64) | 更新人账号 |

### 2.4 枚举字段规范

| 规则 | 说明 |
|------|------|
| **字段类型** | 状态枚举使用 `varchar(20)` |
| **注释说明** | 在 COMMENT 中说明所有枚举值含义 |
| **示例** | `varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft / published'` |

**枚举字段汇总**：

| 枚举字段 | 适用表 | 枚举值 | 说明 |
|----------|--------|--------|------|
| `status` | `cp_connector` | `active` / `disabled` | 连接器启用/禁用 |
| `connector_type` | `cp_connector` | `HTTP`（MVP） | 连接器协议类型（V1 扩展 MySQL/Redis/Kafka 等） |
| `status` | `cp_connector_version`, `cp_flow_version` | `draft` / `published` | 版本状态 |
| `status` | `cp_flow` | `enabled` / `disabled` | 连接流启停状态 |
| `status` | `cp_execution_record` | `success` / `failed` / `timeout` | **同步执行终态**（无 pending/running） |
| `status` | `cp_execution_step` | `success` / `failed` | 步骤执行状态 |
| `status` | `cp_connector_auth_config` | `active` / `expired` / `revoked` | 凭证状态 |
| `trigger_type` | `cp_execution_record` | `http` / `manual` / `test` | 触发方式（MVP） |
| `node_type` | `cp_flow_node` | `entry` / `connector` / `data_processor` / `exit` | 节点类型（MVP） |
| `auth_type` | `cp_connector_auth_config` | `AKSK` / `OAUTH2` / `BASIC_AUTH` / `API_KEY` | 认证类型 |

> **与 v1.x 的差异**：
> - `cp_execution_record.status`：移除 `pending` / `running`（同步执行无中间状态）
> - `trigger_type`：移除 `event` / `webhook` / `scheduled`，新增 `http` / `test`（MVP 触发方式）
> - `node_type`：新增 `data_processor`（数据处理节点纳入 MVP）
> - `connector_type`：MVP 仅支持 `HTTP`（移除 MySQL/Redis/Kafka/gRPC）

---

## 3. 表结构定义

### 3.1 `cp_connector` — 连接器基本信息

```sql
-- ============================================
-- 连接器基本信息表
-- ============================================
CREATE TABLE `cp_connector` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `connector_id`    varchar(32)  NOT NULL                  COMMENT '业务ID（如 con_xxxxxxxx）',
  `name`            varchar(100) NOT NULL                  COMMENT '连接器名称',
  `icon`            varchar(500) DEFAULT NULL              COMMENT '连接器图标 URL',
  `description`     varchar(2000) DEFAULT NULL             COMMENT '连接器描述',
  `connector_type`  varchar(50)  NOT NULL                  COMMENT '类型：HTTP（MVP）',
  `status`          varchar(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active / disabled',
  `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0        COMMENT '软删除标记',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`      varchar(64)  DEFAULT NULL,
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`      varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_connector_id` (`connector_id`),
  KEY `idx_connector_type` (`connector_type`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器基本信息表';
```

> **变更说明**：移除 `visibility`, `creator_app_id`, `creator_user_id` 字段。MVP 角色统一为平台管理员（无需区分应用/用户），可见性无概念（无上架/下架 NG13）

### 3.2 `cp_connector_version` — 连接器版本

```sql
-- ============================================
-- 连接器版本表（发布时快照基本信息 + 连接配置）
-- ============================================
CREATE TABLE `cp_connector_version` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `version_id`         varchar(32)  NOT NULL                  COMMENT '业务ID（如 cv_xxxxxxxx）',
  `connector_id`       varchar(32)  NOT NULL                  COMMENT '所属连接器ID',
  `version_no`         varchar(20)  NOT NULL                  COMMENT '版本号（如 1.0.0）',
  `status`             varchar(20)  NOT NULL DEFAULT 'draft'  COMMENT '状态：draft / published',
  `basic_info_snapshot` json         DEFAULT NULL              COMMENT '基本信息快照（name/icon/description/type）',
  `connection_config`  json         DEFAULT NULL              COMMENT '连接配置（见下方 JSON Schema）',
  `change_log`         varchar(2000) DEFAULT NULL             COMMENT '版本变更说明',
  `published_at`       datetime(3)  DEFAULT NULL              COMMENT '发布时间',
  `created_at`         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`         varchar(64)  DEFAULT NULL,
  `updated_at`         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`         varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_id` (`version_id`),
  KEY `idx_connector_id` (`connector_id`),
  KEY `idx_connector_version` (`connector_id`, `version_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器版本表';
```

> **变更说明**：移除 `approval_id` 字段（本版本发布无需审批 NG19，V1 阶段引入）

**connection_config JSON Schema**：

```json
{
  "protocol": "HTTP",
  "protocol_config": {
    "url": "https://api.example.com/im/send",
    "method": "POST",
    "headers": { "Content-Type": "application/json" }
  },
  "auth": {
    "type": "AKSK",
    "config": {
      "access_key": "enc:AES256:xxx",
      "secret_key": "enc:AES256:xxx"
    }
  },
  "input_schema": {
    "type": "object",
    "properties": {
      "message": { "type": "string", "description": "消息内容" }
    },
    "required": ["message"]
  },
  "output_schema": {
    "type": "object",
    "properties": {
      "code": { "type": "integer" },
      "data": { "type": "object" }
    }
  },
  "timeout_ms": 30000,
  "rate_limit": {
    "max_per_second": 10,
    "max_concurrent": 5
  }
}
```

**auth.type 枚举**:
| 类型 | 说明 | 配置字段 |
|------|------|---------|
| `NONE` | 无需认证 | — |
| `AKSK` | AccessKey/SecretKey | `access_key`, `secret_key` |
| `OAUTH2_CLIENT` | OAuth2 Client Credentials | `token_url`, `client_id`, `client_secret`, `scopes` |
| `BASIC_AUTH` | HTTP Basic Auth | `username`, `password` |
| `API_KEY` | API Key (header/query) | `key_name`, `key_value`, `position` (header/query) |

---

### 3.3 `cp_flow` — 连接流基本信息

```sql
-- ============================================
-- 连接流基本信息表
-- ============================================
CREATE TABLE `cp_flow` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `flow_id`         varchar(32)  NOT NULL                  COMMENT '业务ID（如 flow_xxxxxxxx）',
  `name`            varchar(100) NOT NULL                  COMMENT '连接流名称',
  `description`     varchar(2000) DEFAULT NULL             COMMENT '连接流描述',
  `status`          varchar(20)  NOT NULL DEFAULT 'disabled' COMMENT '运行状态：enabled / disabled',
  `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0        COMMENT '软删除标记',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`      varchar(64)  DEFAULT NULL,
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`      varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_id` (`flow_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接流基本信息表';
```

> **变更说明**：移除 `creator_app_id`, `creator_user_id` 字段（角色统一为平台管理员）

### 3.4 `cp_flow_version` — 连接流版本

```sql
-- ============================================
-- 连接流版本表（发布时快照基本信息 + 编排配置）
-- ============================================
CREATE TABLE `cp_flow_version` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `version_id`           varchar(32)  NOT NULL                  COMMENT '业务ID（如 fv_xxxxxxxx）',
  `flow_id`              varchar(32)  NOT NULL                  COMMENT '所属连接流ID',
  `version_no`           varchar(20)  NOT NULL                  COMMENT '版本号（如 1.0.0）',
  `status`               varchar(20)  NOT NULL DEFAULT 'draft'  COMMENT '状态：draft / published',
  `basic_info_snapshot`  json         DEFAULT NULL              COMMENT '基本信息快照（name/description）',
  `orchestration_config` json         DEFAULT NULL              COMMENT '编排配置（见下方 JSON Schema）',
  `change_log`           varchar(2000) DEFAULT NULL             COMMENT '版本变更说明',
  `published_at`         datetime(3)  DEFAULT NULL              COMMENT '发布时间',
  `created_at`           datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`           varchar(64)  DEFAULT NULL,
  `updated_at`           datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`           varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_id` (`version_id`),
  KEY `idx_flow_id` (`flow_id`),
  KEY `idx_flow_version` (`flow_id`, `version_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接流版本表';
```

> **变更说明**：移除 `approval_id` 字段（本版本发布无需审批 NG19）

**orchestration_config JSON Schema**：

> 以下为数据库 JSON 列存储格式（snake_case）。API 响应时字段名统一转为 camelCase。

```json
{
  "trigger": {
    "type": "http",
    "config": {
      "method": "POST",
      "schema": {
        "type": "object",
        "properties": {
          "sender": { "type": "string" },
          "content": { "type": "string" }
        }
      }
    }
  },
  "nodes": [
    {
      "node_id": "node_entry",
      "node_type": "entry",
      "label": "接收请求",
      "position": { "x": 100, "y": 200 }
    },
    {
      "node_id": "node_1",
      "node_type": "connector",
      "label": "发送消息",
      "connector_version_id": "cv_xxxx",
      "input_mapping": {
        "message": "${trigger.content}"
      },
      "position": { "x": 350, "y": 200 }
    },
    {
      "node_id": "node_2",
      "node_type": "data_processor",
      "label": "格式化消息",
      "config": {
        "field_mappings": [
          { "source": "${node_1.msg_id}", "target": "result.id" },
          { "source": "constant:success", "target": "result.status" }
        ]
      },
      "position": { "x": 500, "y": 200 }
    },
    {
      "node_id": "node_exit",
      "node_type": "exit",
      "label": "返回结果",
      "output_fields": ["result.id", "result.status"],
      "position": { "x": 650, "y": 200 }
    }
  ],
  "edges": [
    { "edge_id": "e1", "source": "node_entry", "target": "node_1" },
    { "edge_id": "e2", "source": "node_1", "target": "node_2" },
    { "edge_id": "e3", "source": "node_2", "target": "node_exit" }
  ]
}
```

**trigger.type 枚举（MVP）**:
| 类型 | 说明 | config 关键字段 |
|------|------|-----------------|
| `http` | HTTP 触发 | `method` — 请求方法, `schema` — 请求体 Schema |
| `manual` | 手动触发 | —（无需额外配置） |

> **变更说明**：移除 `event`, `webhook`, `scheduled` 类型（V1 阶段引入）

---

### 3.5 `cp_flow_node` — 流节点定义

```sql
-- ============================================
-- 流节点定义表（entry / connector / data_processor / exit）
-- ============================================
CREATE TABLE `cp_flow_node` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `node_id`              varchar(32)  NOT NULL                  COMMENT '业务ID（如 fn_xxxxxxxx）',
  `version_id`           varchar(32)  NOT NULL                  COMMENT '所属连接流版本ID',
  `node_type`            varchar(30)  NOT NULL                  COMMENT '节点类型：entry / connector / data_processor / exit',
  `label`                varchar(100) DEFAULT NULL              COMMENT '节点显示名称',
  `connector_version_id` varchar(32)  DEFAULT NULL              COMMENT '引用的连接器版本ID（connector类型）',
  `config_json`          json         DEFAULT NULL              COMMENT '节点配置（见下方）',
  `position_x`           int          DEFAULT 0                 COMMENT '画布X坐标',
  `position_y`           int          DEFAULT 0                 COMMENT '画布Y坐标',
  `sort_order`           int          NOT NULL DEFAULT 0        COMMENT '节点排序（入口=0，出口=999）',
  `created_at`           datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`           datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_id` (`node_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流节点定义表';
```

> **变更说明**：`node_type` 枚举新增 `data_processor`（数据处理节点纳入 MVP）

### 3.6 `cp_flow_edge` — 流连线定义

```sql
-- ============================================
-- 流连线定义表（含源→目标字段映射）
-- ============================================
CREATE TABLE `cp_flow_edge` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `edge_id`         varchar(32)  NOT NULL                  COMMENT '业务ID（如 fe_xxxxxxxx）',
  `version_id`      varchar(32)  NOT NULL                  COMMENT '所属连接流版本ID',
  `source_node_id`  varchar(32)  NOT NULL                  COMMENT '源节点ID',
  `target_node_id`  varchar(32)  NOT NULL                  COMMENT '目标节点ID',
  `data_mappings`   json         DEFAULT NULL              COMMENT '源→目标字段映射（见下方）',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edge_id` (`edge_id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_source_node` (`source_node_id`),
  KEY `idx_target_node` (`target_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流连线定义表';
```

**data_mappings 示例**:
```json
[
  {
    "source": "${trigger.sender}",
    "target": "message.from",
    "transform": null
  },
  {
    "source": "${trigger.content}",
    "target": "message.text",
    "transform": "base64_encode"
  },
  {
    "source": "constant:Hello",
    "target": "message.greeting",
    "transform": null
  }
]
```

---

### 3.7 `cp_execution_record` — 执行记录

```sql
-- ============================================
-- 执行记录表（每次同步执行后生成）
-- ============================================
CREATE TABLE `cp_execution_record` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `execution_id`    varchar(32)  NOT NULL                  COMMENT '执行ID（如 exec_xxxxxxxx）',
  `flow_id`         varchar(32)  NOT NULL                  COMMENT '所属连接流ID',
  `version_id`      varchar(32)  NOT NULL                  COMMENT '执行的版本ID',
  `trigger_type`    varchar(20)  NOT NULL                  COMMENT '触发方式：http / manual / test',
  `status`          varchar(20)  NOT NULL DEFAULT 'success' COMMENT '执行状态：success / failed / timeout',
  `trigger_data`    json         DEFAULT NULL              COMMENT '触发数据快照',
  `result_data`     json         DEFAULT NULL              COMMENT '执行返回值（出口节点输出）',
  `error_message`   varchar(5000) DEFAULT NULL             COMMENT '失败原因',
  `started_at`      datetime(3)  DEFAULT NULL              COMMENT '执行开始时间',
  `finished_at`     datetime(3)  DEFAULT NULL              COMMENT '执行结束时间',
  `duration_ms`     int          DEFAULT NULL              COMMENT '执行耗时（毫秒）',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execution_id` (`execution_id`),
  KEY `idx_flow_id` (`flow_id`),
  KEY `idx_status` (`status`),
  KEY `idx_trigger_type` (`trigger_type`),
  KEY `idx_started_at` (`started_at`),
  KEY `idx_flow_status` (`flow_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录表';
```

> **变更说明**：
> - `status` 枚举：移除 `pending` / `running`（同步执行无中间状态）
> - 移除 `max_retries`, `retry_count` 字段（失败重试 NG15，V1 阶段引入）
> - `trigger_type` 枚举：`http` / `manual` / `test`

### 3.8 `cp_execution_step` — 执行步骤详情

```sql
-- ============================================
-- 执行步骤详情表（每次执行的每步记录）
-- ============================================
CREATE TABLE `cp_execution_step` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `step_id`         varchar(32)  NOT NULL                  COMMENT '步骤ID（如 step_xxxxxxxx）',
  `execution_id`    varchar(32)  NOT NULL                  COMMENT '所属执行记录ID',
  `node_id`         varchar(32)  NOT NULL                  COMMENT '对应节点ID',
  `node_name`       varchar(100) DEFAULT NULL              COMMENT '节点显示名称',
  `node_type`       varchar(30)  NOT NULL                  COMMENT '节点类型：entry/connector/data_processor/exit',
  `status`          varchar(20)  NOT NULL DEFAULT 'success' COMMENT '步骤状态：success / failed',
  `input_data`      json         DEFAULT NULL              COMMENT '步骤输入数据',
  `output_data`     json         DEFAULT NULL              COMMENT '步骤输出数据',
  `error_message`   varchar(5000) DEFAULT NULL             COMMENT '错误信息',
  `started_at`      datetime(3)  DEFAULT NULL              COMMENT '步骤开始时间',
  `finished_at`     datetime(3)  DEFAULT NULL              COMMENT '步骤结束时间',
  `duration_ms`     int          DEFAULT NULL              COMMENT '步骤耗时',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_id` (`step_id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行步骤详情表';
```

> **变更说明**：移除 `retry_attempts` 字段（失败重试 NG15，V1 阶段引入）。`node_type` 新增 `data_processor`。

### 3.9 `cp_connector_auth_config` — 连接器认证凭证

> **与 spec.md 的设计差异**：spec.md FR-006 说明认证凭证作为 `connection_config` JSON 的一部分存储在连接器版本中。Plan 阶段将其抽取为独立表 `cp_connector_auth_config`，核心原因是**同一连接器版本可能被不同消费方使用，每个消费方需要独立的认证凭证**。这是对 spec 模型的合理扩展，遵循「连接器版本定义能力接口，消费方提供自身凭证」的职责分离原则。（虽然本版本角色统一为平台管理员，但保留此设计为 V1 多应用场景预留扩展点）

```sql
-- ============================================
-- 连接器认证凭证表（AES-256-GCM 加密存储，按 app 隔离）
-- ============================================
CREATE TABLE `cp_connector_auth_config` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `auth_id`             varchar(32)  NOT NULL                  COMMENT '业务ID（如 auth_xxxxxxxx）',
  `connector_version_id` varchar(32) NOT NULL                  COMMENT '所属连接器版本ID',
  `app_id`              varchar(32)  NOT NULL                  COMMENT '消费方应用ID（认证凭据归属）',
  `auth_type`           varchar(30)  NOT NULL                  COMMENT '认证类型：AKSK/OAUTH2/BASIC_AUTH/API_KEY',
  `encrypted_credentials` json       NOT NULL                  COMMENT '加密凭证（AES-256-GCM 加密）',
  `expires_at`          datetime(3)  DEFAULT NULL              COMMENT '凭证过期时间',
  `status`              varchar(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active/expired/revoked',
  `created_at`          datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`          varchar(64)  DEFAULT NULL,
  `updated_at`          datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`          varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_id` (`auth_id`),
  UNIQUE KEY `uk_connector_version_app` (`connector_version_id`, `app_id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器认证凭证表（加密存储）';
```

---

## 4. 表关系总览

```
cp_connector (1) ──→ (N) cp_connector_version
                             │
                             │ (N)
                             ↓ (1)
cp_flow (1) ──→ (N) cp_flow_version (1) ──→ (N) cp_flow_node
                                      (1) ──→ (N) cp_flow_edge

cp_flow (1) ──→ (N) cp_execution_record (1) ──→ (N) cp_execution_step

cp_connector_version (1) ──→ (N) cp_connector_auth_config (N) ──→ app_id
```

---

## 5. 数据归档与清理策略

| 表 | 保留策略 | 清理方式 |
|---|---------|---------|
| `cp_execution_record` | 默认 30 天（可配置） | 定时任务清理已完成的记录 |
| `cp_execution_step` | 随父记录一同清理 | 按 `execution_id` 级联删除 |
| `cp_connector_version` | 永久保留 | — |
| `cp_connector_auth_config` | 过期凭证保留 90 天后清理 | 定时任务清理过期凭证 |

---

## 6. 版本号规则

| 实体 | 业务ID 前缀 | 示例 |
|------|-----------|------|
| 连接器 | `con_` | `con_a1b2c3d4` |
| 连接器版本 | `cv_` | `cv_e5f6g7h8` |
| 连接流 | `flow_` | `flow_i9j0k1l2` |
| 连接流版本 | `fv_` | `fv_m3n4o5p6` |
| 节点 | `fn_` | `fn_q7r8s9t0` |
| 连线 | `fe_` | `fe_u1v2w3x4` |
| 执行记录 | `exec_` | `exec_y5z6a7b8` |
| 执行步骤 | `step_` | `step_c9d0e1f2` |
| 认证凭据 | `auth_` | `auth_g3h4i5j6` |
| HTTP 触发 Token | `tr_` | `tr_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6` |

**版本号格式**: `{major}.{minor}.{patch}` (如 `1.0.0`, `1.1.0`, `2.0.0`)

---

## 附录 A：变更日志（v1.x → v2.0）

| 变更项 | v1.x | v2.0（基于 spec v4.0） | 原因 |
|--------|------|----------------------|------|
| `cp_connector` 字段 | 含 `visibility`, `creator_app_id`, `creator_user_id` | 移除这些字段 | 角色统一为平台管理员，无上架/下架 |
| `cp_connector_version` 字段 | 含 `approval_id` | 移除 | 版本发布无需审批（NG19） |
| `cp_flow` 字段 | 含 `creator_app_id`, `creator_user_id` | 移除 | 角色统一为平台管理员 |
| `cp_flow_version` 字段 | 含 `approval_id` | 移除 | 版本发布无需审批（NG19） |
| `cp_execution_record.status` | `pending/running/success/failed/timeout` | `success/failed/timeout` | 同步执行无中间状态 |
| `cp_execution_record.trigger_type` | `event/webhook/scheduled/manual` | `http/manual/test` | MVP 仅 HTTP/手动触发 |
| `cp_execution_record` 字段 | 含 `retry_count`, `max_retries` | 移除 | 失败重试移至 NG15，V1 |
| `cp_execution_step` 字段 | 含 `retry_attempts` | 移除 | 同上 |
| `cp_execution_step.node_type` | `entry/connector/exit` | `entry/connector/data_processor/exit` | 数据处理节点纳入 MVP |
| `cp_flow_node.node_type` | `entry/connector/exit` | `entry/connector/data_processor/exit` | 同上 |
| `cp_connector.connector_type` | `HTTP/MySQL/Redis/Kafka/gRPC` | `HTTP`（MVP） | NG12，V1 扩展 |