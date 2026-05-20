# 数据库设计：连接器平台

**Feature ID**: CONN-PLAT-001  
**关联文档**: plan.md (§4.2)  
**版本**: v1.0  
**创建日期**: 2026-05-19

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
| **表前缀** | 能力平台使用 `openplatform_v2_`，连接器平台使用 `cp_`（Connector Platform） | `cp_connector` |
| **表后缀** | 统一使用 `_t` 后缀表示表 | `cp_connector_t`（此规范在连接器平台改为无后缀，**注意差异**） |
| **属性表后缀** | 扩展属性表使用 `_p_t` 后缀 | 连接器平台暂不使用属性表模式 |
| **命名风格** | 小写字母 + 下划线分隔 | `cp_execution_record` |
| **索引命名** | `idx_字段名` 或 `idx_字段名1_字段名2` | `idx_connector_id`, `idx_flow_id_status` |
| **唯一索引命名** | `uk_字段名` | `uk_connector_id`, `uk_version_id` |

### 2.2 主键规范

| 规则 | 说明 |
|------|------|
| **主键类型** | BIGINT，自增（连接器平台）/ 雪花ID（能力平台） |
| **主键命名** | 统一使用 `id` |
| **业务ID** | 通过独立 `varchar(32)` 字段存储业务标识（如 `connector_id`），便于分布式场景和 URL 安全 |
| **关联字段** | 使用逻辑外键（存储关联 ID），**不使用物理外键约束**，关联关系由应用层维护 |

> **禁止使用外键**：所有表关联关系通过存储逻辑字段实现，不使用数据库物理外键约束（FOREIGN KEY）。关联关系由应用层维护。— 来源：能力开放平台规范

### 2.3 审计字段规范

所有业务表必须包含以下审计字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `created_at` | DATETIME(3) | 创建时间，默认 `CURRENT_TIMESTAMP(3)` |
| `updated_at` | DATETIME(3) | 更新时间，默认 `CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)` |
| `created_by` | VARCHAR(64) | 创建人账号 |
| `updated_by` | VARCHAR(64) | 更新人账号 |

> 时间字段统一使用 `DATETIME(3)` 精确到毫秒，确保高并发场景下的时间精度。

### 2.4 枚举字段规范

| 规则 | 说明 |
|------|------|
| **字段类型** | 状态枚举使用 `varchar(20)`（连接器平台，可读性强）/ `TINYINT`（能力平台） |
| **注释说明** | 在 COMMENT 中说明所有枚举值含义 |
| **示例** | `varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft / published'` |

> 连接器平台选择 varchar 枚举值（替代 TINYINT），原因是：状态值可读性强、日志排查直观、API 返回值无需转换映射。— 遵循与能力平台一致的文档规范，具体实现选择适配连接器场景。

### 2.5 Scope 命名规范（复用能力开放平台）

连接器平台复用能力开放平台的 Scope 权限模型，Scope 命名遵循统一规范：

**格式**: `{资源类型}:{模块}:{资源标识}`

| 资源类型 | Scope 示例 | 说明 |
|----------|------------|------|
| API | `api:im:send-message` | IM 模块发送消息 API |
| Event | `event:im:message-received` | IM 模块消息接收事件 |

**命名规则**：

| 部分 | 说明 | 示例 |
|------|------|------|
| `{资源类型}` | api / event / callback | `api` |
| `{模块}` | 业务模块名 | `im`、`meeting`、`approval` |
| `{资源标识}` | 具体资源的唯一标识 | `send-message`、`message-received` |

> 连接器定义中若引用内部系统 API/事件，需通过 Scope 获得授权，Scope 命名必须符合上述规范，确保全局唯一。

### 2.6 名称和描述字段规范

涉及名称、描述场景的字段，统一使用中英文双语（与能力开放平台一致）：

| 字段类型 | 字段命名 | 类型 | 说明 |
|----------|----------|------|------|
| **名称** | `name` | VARCHAR(100) | 名称（连接器平台暂用单字段，后续可扩展 `name_cn`/`name_en`） |
| **描述** | `description` | VARCHAR(2000) | 描述（连接器平台暂用单字段） |

> 能力开放平台使用 `name_cn`/`name_en` 双语设计。连接器平台 MVP 阶段暂用单字段 `name`/`description`，后续如需国际化可扩展为 `name_cn`/`name_en` + `description_cn`/`description_en`。

---

## 3. 表结构定义

### 3.1 `cp_connector` — 连接器基本信息

```sql
CREATE TABLE `cp_connector` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `connector_id`    varchar(32)  NOT NULL                  COMMENT '业务ID（如 con_xxxxxxxx）',
  `name`            varchar(100) NOT NULL                  COMMENT '连接器名称',
  `icon`            varchar(500) DEFAULT NULL              COMMENT '连接器图标 URL',
  `description`     varchar(2000) DEFAULT NULL             COMMENT '连接器描述',
  `connector_type`  varchar(50)  NOT NULL                  COMMENT '类型：HTTP / MySQL / Redis / Kafka / gRPC / CUSTOM',
  `visibility`      varchar(20)  NOT NULL DEFAULT 'private' COMMENT '可见性：public / private',
  `creator_app_id`  varchar(32)  NOT NULL                  COMMENT '创建者应用ID',
  `creator_user_id` varchar(64)  DEFAULT NULL              COMMENT '创建者用户ID',
  `status`          varchar(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active / disabled',
  `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0        COMMENT '软删除标记',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`      varchar(64)  DEFAULT NULL,
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`      varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_connector_id` (`connector_id`),
  KEY `idx_creator_app_id` (`creator_app_id`),
  KEY `idx_visibility_status` (`visibility`, `status`),
  KEY `idx_connector_type` (`connector_type`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器基本信息表';
```

### 3.2 `cp_connector_version` — 连接器版本

```sql
CREATE TABLE `cp_connector_version` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `version_id`         varchar(32)  NOT NULL                  COMMENT '业务ID（如 cv_xxxxxxxx）',
  `connector_id`       varchar(32)  NOT NULL                  COMMENT '所属连接器ID',
  `version_no`         varchar(20)  NOT NULL                  COMMENT '版本号（如 1.0.0）',
  `status`             varchar(20)  NOT NULL DEFAULT 'draft'  COMMENT '状态：draft / published',
  `basic_info_snapshot` json         DEFAULT NULL              COMMENT '基本信息快照（name/icon/description/type）',
  `connection_config`  json         DEFAULT NULL              COMMENT '连接配置（见下方 JSON Schema）',
  `change_log`         varchar(2000) DEFAULT NULL             COMMENT '版本变更说明',
  `approval_id`        varchar(32)  DEFAULT NULL              COMMENT '关联审批单ID（已发布版本）',
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

**connection_config JSON Schema**:
```json
{
  "protocol": "HTTP",
  "protocol_config": {
    "base_url": "https://api.example.com",
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
 CREATE TABLE `cp_flow` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `flow_id`         varchar(32)  NOT NULL                  COMMENT '业务ID（如 flow_xxxxxxxx）',
  `name`            varchar(100) NOT NULL                  COMMENT '连接流名称',
  `description`     varchar(2000) DEFAULT NULL             COMMENT '连接流描述',
  `status`          varchar(20)  NOT NULL DEFAULT 'disabled' COMMENT '运行状态：enabled / disabled',
  `creator_app_id`  varchar(32)  NOT NULL                  COMMENT '创建者应用ID',
  `creator_user_id` varchar(64)  DEFAULT NULL              COMMENT '创建者用户ID',
  `is_deleted`      tinyint(1)   NOT NULL DEFAULT 0        COMMENT '软删除标记',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by`      varchar(64)  DEFAULT NULL,
  `updated_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by`      varchar(64)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_id` (`flow_id`),
  KEY `idx_creator_app_id` (`creator_app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接流基本信息表';
```

### 3.4 `cp_flow_version` — 连接流版本

```sql
CREATE TABLE `cp_flow_version` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `version_id`           varchar(32)  NOT NULL                  COMMENT '业务ID（如 fv_xxxxxxxx）',
  `flow_id`              varchar(32)  NOT NULL                  COMMENT '所属连接流ID',
  `version_no`           varchar(20)  NOT NULL                  COMMENT '版本号（如 1.0.0）',
  `status`               varchar(20)  NOT NULL DEFAULT 'draft'  COMMENT '状态：draft / published',
  `basic_info_snapshot`  json         DEFAULT NULL              COMMENT '基本信息快照（name/description）',
  `orchestration_config` json         DEFAULT NULL              COMMENT '编排配置（见下方 JSON Schema）',
  `change_log`           varchar(2000) DEFAULT NULL             COMMENT '版本变更说明',
  `approval_id`          varchar(32)  DEFAULT NULL              COMMENT '关联审批单ID',
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

**orchestration_config JSON Schema**:
```json
{
  "trigger": {
    "type": "event",
    "config": {
      "event_source": "im:message:receive",
      "scope": "im:message:receive",
      "schema": {
        "type": "object",
        "properties": {
          "sender": { "type": "string" },
          "content": { "type": "string" },
          "timestamp": { "type": "integer" }
        }
      }
    }
  },
  "nodes": [
    {
      "node_id": "node_entry",
      "node_type": "entry",
      "label": "收到消息",
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
      "retry_policy": {
        "max_retries": 3,
        "interval_ms": 1000,
        "backoff_multiplier": 2.0
      },
      "position": { "x": 350, "y": 200 }
    },
    {
      "node_id": "node_exit",
      "node_type": "exit",
      "label": "返回值",
      "output_fields": ["result.code", "result.data"],
      "position": { "x": 600, "y": 200 }
    }
  ],
  "edges": [
    { "edge_id": "e1", "source": "node_entry", "target": "node_1" },
    { "edge_id": "e2", "source": "node_1", "target": "node_exit" }
  ]
}
```

**trigger.type 枚举**:
| 类型 | 说明 | config 关键字段 |
|------|------|-----------------|
| `event` | 事件触发 | `event_source` — 事件源标识, `scope` — 所需 Scope |
| `webhook` | Webhook 触发 | `webhook_path` — 自动生成唯一路径 |
| `scheduled` | 定时触发 | `cron` — Cron 表达式, `timezone` — 时区 |
| `manual` | 手动触发 | —（无需额外配置） |

---

### 3.5 `cp_flow_node` — 流节点定义

```sql
CREATE TABLE `cp_flow_node` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `node_id`              varchar(32)  NOT NULL                  COMMENT '业务ID（如 fn_xxxxxxxx）',
  `version_id`           varchar(32)  NOT NULL                  COMMENT '所属连接流版本ID',
  `node_type`            varchar(30)  NOT NULL                  COMMENT '节点类型：entry / connector / exit',
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

### 3.6 `cp_flow_edge` — 流连线定义

```sql
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

### 3.7 `cp_execution_record` — 执行记录

```sql
CREATE TABLE `cp_execution_record` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `execution_id`    varchar(32)  NOT NULL                  COMMENT '执行ID（如 exec_xxxxxxxx）',
  `flow_id`         varchar(32)  NOT NULL                  COMMENT '所属连接流ID',
  `version_id`      varchar(32)  NOT NULL                  COMMENT '执行的版本ID',
  `trigger_type`    varchar(20)  NOT NULL                  COMMENT '触发方式：event/webhook/scheduled/manual',
  `status`          varchar(20)  NOT NULL DEFAULT 'pending' COMMENT '执行状态：pending/running/success/failed/timeout',
  `trigger_data`    json         DEFAULT NULL              COMMENT '触发数据快照',
  `result_data`     json         DEFAULT NULL              COMMENT '执行返回值（出口节点输出）',
  `error_message`   varchar(5000) DEFAULT NULL             COMMENT '失败原因',
  `started_at`      datetime(3)  DEFAULT NULL              COMMENT '执行开始时间',
  `finished_at`     datetime(3)  DEFAULT NULL              COMMENT '执行结束时间',
  `duration_ms`     int          DEFAULT NULL              COMMENT '执行耗时（毫秒）',
  `retry_count`     int          NOT NULL DEFAULT 0        COMMENT '重试次数',
  `max_retries`     int          NOT NULL DEFAULT 0        COMMENT '最大重试次数',
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

### 3.8 `cp_execution_step` — 执行步骤详情

```sql
CREATE TABLE `cp_execution_step` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '自增主键',
  `step_id`         varchar(32)  NOT NULL                  COMMENT '步骤ID（如 step_xxxxxxxx）',
  `execution_id`    varchar(32)  NOT NULL                  COMMENT '所属执行记录ID',
  `node_id`         varchar(32)  NOT NULL                  COMMENT '对应节点ID',
  `node_name`       varchar(100) DEFAULT NULL              COMMENT '节点显示名称',
  `node_type`       varchar(30)  NOT NULL                  COMMENT '节点类型：entry/connector/exit',
  `status`          varchar(20)  NOT NULL DEFAULT 'pending' COMMENT '步骤状态：pending/running/success/failed/skipped',
  `input_data`      json         DEFAULT NULL              COMMENT '步骤输入数据',
  `output_data`     json         DEFAULT NULL              COMMENT '步骤输出数据',
  `error_message`   varchar(5000) DEFAULT NULL             COMMENT '错误信息',
  `started_at`      datetime(3)  DEFAULT NULL              COMMENT '步骤开始时间',
  `finished_at`     datetime(3)  DEFAULT NULL              COMMENT '步骤结束时间',
  `duration_ms`     int          DEFAULT NULL              COMMENT '步骤耗时',
  `retry_attempts`  int          NOT NULL DEFAULT 0        COMMENT '重试次数',
  `created_at`      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_id` (`step_id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_node_id` (`node_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行步骤详情表';
```

### 3.9 `cp_connector_auth_config` — 连接器认证凭证

> **与 spec.md 的设计差异**：spec.md FR-008 将认证凭证作为 `connection_config` JSON 的一部分存储在连接器版本中。Plan 阶段将其抽取为独立表 `cp_connector_auth_config`，核心原因是**同一连接器版本可能被不同消费方应用使用，每个应用需要独立的认证凭证**（如各三方平台使用自己的 AKSK 调用 IM 发送消息）。这是对 spec 模型的合理扩展，遵循「连接器版本定义能力接口，消费方提供自身凭证」的职责分离原则。

```sql
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
| `cp_connector_version` | 永久保留 | 软标记下架版本 |
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

**版本号格式**: `{major}.{minor}.{patch}` (如 `1.0.0`, `1.1.0`, `2.0.0`)