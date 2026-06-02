# 数据字典管理 - 技术规划文档

## 文档元数据

| 字段 | 内容 |
|------|------|
| **Feature ID** | FR-DICTIONARY-001 |
| **Feature 名称** | 数据字典管理 |
| **版本** | 1.0.0 |
| **状态** | planned |
| **创建日期** | 2026-05-22 |
| **作者** | SDDU-Plan Agent |

---

## 1. 技术选型

与 LookUp 模块保持一致：React + Java 21 + Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Apache POI

## 2. 架构设计

单表扁平结构，模块路径：modules/dictionary/

## 3. 数据库设计

表名：openplatform_property_t

字段：id, code, name, value, description, path, status, create_by, create_time, last_update_by, last_update_time

唯一索引：idx_path_code (path, code)

### 3.1 表结构定义

#### 3.1.1 数据字典表：openplatform_property_t

| 字段名 | 数据类型 | 是否可空 | 默认值 | 说明 |
|--------|----------|----------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 主键 |
| code | VARCHAR(100) | NO | - | 编码，同路径下唯一 |
| name | VARCHAR(100) | YES | NULL | 名称 |
| value | VARCHAR(2000) | YES | NULL | 值 |
| description | VARCHAR(4000) | YES | NULL | 描述 |
| path | VARCHAR(100) | YES | NULL | 路径 |

| status | TINYINT | NO | 1 | 状态：0-失效，1-有效 |
| create_by | VARCHAR(100) | YES | NULL | 创建人 |
| create_time | DATETIME(3) | NO | CURRENT_TIMESTAMP(3) | 创建时间 |
| last_update_by | VARCHAR(100) | YES | NULL | 最后更新人 |
| last_update_time | DATETIME(3) | NO | CURRENT_TIMESTAMP(3) | 最后更新时间 |

### 3.2 索引设计

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | id | 主键索引 |
| idx_path_code | 唯一 | path, code | 同路径下编码唯一 |
| idx_path_name | 普通 | path, name | 路径+名称联合查询 |
| idx_name | 普通 | name | 名称查询索引 |

### 3.3 DDL 脚本

```sql
CREATE TABLE IF NOT EXISTS `openplatform_property_t` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(100) NOT NULL COMMENT '编码',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '名称',
    `value` VARCHAR(2000) DEFAULT NULL COMMENT '值',
    `description` VARCHAR(4000) DEFAULT NULL COMMENT '描述',
    `path` VARCHAR(100) DEFAULT NULL COMMENT '路径',

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-失效 1-有效',
    `create_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `last_update_by` VARCHAR(100) DEFAULT NULL COMMENT '最后更新人',
    `last_update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_path_code` (`path`, `code`) USING BTREE,
    KEY `idx_path_name` (`path`, `name`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';
```

---

## 4. 接口设计

### 4.1 接口概览

| 接口 | URL | Method |
|------|-----|--------|
| 列表查询 | /api/v1/dictionary/list | GET |
| 新增 | /api/v1/dictionary | POST |
| 详情 | /api/v1/dictionary/{id} | GET |
| 编辑 | /api/v1/dictionary/{id} | PUT |
| 删除 | /api/v1/dictionary/{id} | DELETE |
| 异步导入 | /api/v1/dictionary/import/async | POST |
| 异步导出 | /api/v1/dictionary/export/async | POST |
| 下载模板 | /api/v1/dictionary/import/template | GET |

### 4.2 详细接口

#### 4.2.1 查询数据字典列表

- **URL**: `/api/v1/dictionary/list`
- **Method**: GET
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | String | 否 | 编码，模糊匹配 |
| name | String | 否 | 名称，模糊匹配 |
| path | String | 否 | 路径，模糊匹配 |

| status | Integer | 否 | 状态：0-失效，1-有效，空-全部 |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "code": "USER_STATUS",
        "name": "用户状态",
        "value": "active",
        "description": "用户账户状态字典",
        "path": "system/user",

        "status": 1,
        "createBy": "admin",
        "createTime": "2024-01-10 09:00:00",
        "lastUpdateBy": "admin",
        "lastUpdateTime": "2024-01-20 11:00:00"
      }
    ],
    "totalCnt": 20,
    "pageVO": {
      "totalRows": 20,
      "curPage": 1,
      "pageSize": 10,
      "startIndex": 1,
      "endIndex": 10,
      "maxPageSize": 1000
    }
  }
}
```

- **业务逻辑**:
  1. 构建查询条件，支持模糊匹配
  2. 按 create_time 倒序排列
  3. 返回分页结果（使用 PageHelper 分页）

#### 4.2.2 新增数据字典

- **URL**: `/api/v1/dictionary`
- **Method**: POST
- **Content-Type**: application/json
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | String | 是 | 编码，1-100字符，支持字母、数字、下划线、点、横杠，同一路径下唯一 |
| name | String | 是 | 名称，1-100字符 |
| value | String | 否 | 值，0-2000字符 |
| path | String | 否 | 路径，0-100字符 |
| description | String | 否 | 描述，0-4000字符 |


- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

- **业务逻辑**:
  1. 校验 code 格式和唯一性（同路径下）
  2. 校验 name 非空
  3. 填充 createBy/createTime、lastUpdateBy/lastUpdateTime（lastUpdateBy=createBy, lastUpdateTime=createTime）
  4. 插入数据，返回创建结果

#### 4.2.3 获取数据字典详情

- **URL**: `/api/v1/dictionary/{id}`
- **Method**: GET
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 数据字典ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "id": 1,
    "code": "USER_STATUS",
    "name": "用户状态",
    "value": "active",
    "description": "用户账户状态字典",
    "path": "system/user",
    "status": 1,
    "createBy": "admin",
    "createTime": "2024-01-10 09:00:00",
    "lastUpdateBy": "admin",
    "lastUpdateTime": "2024-01-10 09:00:00"
  }
}
```

- **业务逻辑**:
  1. 根据 id 查询数据字典
  2. 不存在则抛 404
  3. 返回完整信息

#### 4.2.4 编辑数据字典

- **URL**: `/api/v1/dictionary/{id}`
- **Method**: PUT
- **Content-Type**: application/json
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 数据字典ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 否 | 名称，1-100字符 |
| value | String | 否 | 值，0-2000字符 |
| path | String | 否 | 路径，0-100字符 |
| description | String | 否 | 描述，0-4000字符 |
| status | Integer | 否 | 目标状态：0-失效，1-有效 |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

- **业务约束**:
  - 编码(code)不可修改

- **业务逻辑**:
  1. 校验 id 存在
  2. 更新数据字典信息（不含 code）
  3. 记录更新人和更新时间

#### 4.2.5 删除数据字典

- **URL**: `/api/v1/dictionary/{id}`
- **Method**: DELETE
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 数据字典ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

- **业务约束**:
  - 删除前需校验状态为失效(0)，有效(1)的数据不可删除

- **业务逻辑**:
  1. 校验 id 存在
  2. 校验状态为失效(0)
  3. 物理删除数据
  4. 记录操作日志

#### 4.2.6 下载导入模板

- **URL**: `/api/v1/dictionary/import/template`
- **Method**: GET
- **响应**: Excel 文件流

- **业务逻辑**:
  1. 生成包含表头的 Excel 模板
  2. 返回文件流供前端下载

### 4.3 导入接口详细设计

#### 4.3.1 提交导入任务

- **URL**: `/api/v1/dictionary/import/async`
- **Method**: POST
- **Content-Type**: multipart/form-data
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | Excel文件，支持 .xlsx/.xls，单Sheet |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "taskId": "IMPORT_20240115_153000_abc123"
  }
}
```

- **错误响应示例**（数据量超出1000条）:
```json
{
  "code": 40002,
  "message": "单次最多处理1000条数据，请分批操作",
  "data": null
}
```

- **错误响应示例**（编码重复）:
```json
{
  "code": 40901,
  "message": "编码 [USER_STATUS] 在路径 [system/user] 下已存在",
  "data": null
}
```

- **业务逻辑**:
  1. 接收 Excel 文件，解析单Sheet
  2. 校验Excel总行数，超出1000条则返回错误
  3. 校验每行数据的编码唯一性（同路径下）
  4. 创建任务记录
  5. 启动异步线程处理

- **业务约束**:
  - 提交前校验，超出1000条则返回错误
  - 编码(code)同路径下唯一

**导入Excel格式（单Sheet）**:

| 列号 | 字段名 | 说明 |
|:----:|:-------|:-----|
| A | 编码 | code，必填 |
| B | 名称 | name，必填 |
| C | 值 | value |
| D | 路径 | path |
| E | 描述 | description |
| F | 状态 | status（1-有效，0-失效） |

**异步处理流程**:
1. **文件上传**: 用户上传Excel文件，后端调用 FileStorageService.upload() 保存文件，返回 file_id
2. **创建任务**: 任务表记录 (task_id, status=PROCESSING, file_id=文件ID)
3. **异步处理**: 创建任务后立即创建异步线程处理：下载Excel、解析数据、插入数据库
   - 通过 file_id 从存储服务下载Excel文件
   - 解析单Sheet数据
   - 校验每行数据的编码唯一性（同路径下）
   - 实时更新任务进度 (progress, success_count, fail_count)
4. **完成处理**: 
   - 生成导入结果报告（Excel：编码、名称、导入结果、失败原因）
   - 上传到存储服务，file_id 覆盖为结果报告的路径
   - 更新任务状态为 COMPLETED，result 字段记录结果描述（如任务失败则记录失败原因）
5. **用户下载**: 任务中心点击下载，通过 task_id 查到 file_id，下载结果报告

### 4.4 导出接口详细设计

#### 4.4.1 提交导出任务

- **URL**: `/api/v1/dictionary/export/async`
- **Method**: POST
- **Content-Type**: application/json
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| params | Object | 否 | 导出参数JSON对象，包含筛选条件和选中的ID |

**params 对象结构**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| selectedIds | Array | 否 | 勾选的数据字典ID列表，若有值则只导出这些数据（优先级高于filters） |
| filters | Object | 否 | 筛选条件对象 |
| filters.code | String | 否 | 编码，模糊匹配 |
| filters.name | String | 否 | 名称，模糊匹配 |
| filters.path | String | 否 | 路径，模糊匹配 |

| filters.status | Integer | 否 | 状态：1-有效，0-失效，空-全部 |

- **请求示例**:
```json
{
  "params": {
    "selectedIds": [1, 2, 3],
    "filters": {
      "code": "USER",
      "name": "用户",
      "path": "system",
      "status": 1
    }
  }
}
```

- **业务约束**:
  - 后端校验，超出1000条时自动截断只返回前1000条

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "taskId": "EXPORT_20240115_153000_xyz789"
  }
}
```

- **业务逻辑**:
  1. 解析 params 参数，提取 selectedIds 和 filters
  2. 校验数据量，超出1000条时自动截断只返回前1000条
  3. 生成任务ID，将 params JSON 存入任务记录
  4. 创建异步任务记录
  5. 返回任务ID

**异步处理流程**:
1. 创建导出任务后立即启动异步线程处理
2. 根据 params 中的 selectedIds 或 filters 查询数据
3. 若有 selectedIds，按勾选ID查询；否则按 filters 条件查询
4. 生成单Sheet的Excel
5. 写入所有数据字典信息
6. 使用 Apache POI 生成 Excel 文件
7. 调用 FileStorageService.upload() 保存文件
8. 更新任务状态为已完成

### 4.5 任务中心

数据字典模块复用 LookUp 的任务中心接口，通过 biz_type 参数区分业务类型。

**任务表复用说明**:
- 任务表：openplatform_task_t
- biz_type=2 表示数据字典业务

**复用的 LookUp 任务接口**:

| 接口 | URL | Method | 说明 |
|------|-----|--------|------|
| 查询任务列表 | /api/v1/lookup/task/list | GET | biz_type=2 查询数据字典任务 |
| 查询任务进度 | /api/v1/lookup/task/progress/{taskId} | GET | 查询任务执行进度 |

**查询任务列表参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bizType | String | 否 | 业务类型：LOOKUP/DATA_DICTIONARY，空-全部 |
| taskType | String | 否 | 任务类型：IMPORT/EXPORT，空-全部 |
| status | String | 否 | 状态：PROCESSING/COMPLETED/FAILED，空-全部 |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |

**数据字典任务查询示例**:
```
GET /api/v1/lookup/task/list?bizType=DATA_DICTIONARY&pageNum=1&pageSize=10
```

---

## 5. 业务规则

- 编码(code)不可修改
- 删除前校验status=0
- 状态切换需二次确认
- 新增时自动填充 lastUpdateBy=createBy, lastUpdateTime=createTime
- 任务表复用 openplatform_task_t，biz_type=2
- 任务状态：1-PROCESSING, 2-COMPLETED, 3-FAILED
- 分页：min 10, max 1000, default 10
- PageVO: totalRows, curPage, pageSize, startIndex, endIndex, maxPageSize

## 6. 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 40002 | 导入数据超限 |
| 40401 | 数据不存在 |
| 40901 | 编码已存在 |
| 40003 | 有效状态不可删除 |

## 7. 工作量估算

后端开发：2人天
前端开发：2人天
联调测试：1人天
合计：5人天
