# LookUp管理 - 技术规划文档

## 文档元数据

| 字段 | 内容 |
|------|------|
| **Feature ID** | FR-LOOKUP-001 |
| **Feature 名称** | LookUp管理 |
| **版本** | 1.0.0 |
| **状态** | planned |
| **创建日期** | 2026-05-15 |
| **作者** | SDDU-Plan Agent |

---

## 1. 技术选型

### 1.1 技术栈选择

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| **前端** | React | 基于 demo.html 参考设计，采用现代前端框架 |
| **后端** | Java 21 + Spring Boot 3.x | 主流企业级技术栈 |
| **ORM** | MyBatis-Plus 3.5+ | 简化 CRUD 操作，支持分页 |
| **数据库** | MySQL 8.0 | 已有表结构，无需迁移 |
| **Excel处理** | Apache POI 5.x | Apache 官方，成熟稳定，支持大批量数据导入 |
| **接口文档** | SpringDoc OpenAPI | 自动生成 Swagger 文档 |
| **构建工具** | Maven 3.9+ | 项目管理与依赖管理 |

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (React)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ 分类管理页面  │  │ LookUp项页面 │  │ 详情/编辑面板     │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ REST API
┌────────────────────────▼────────────────────────────────────┐
│                      控制层 (Controller)                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
 │  │ ClassifyController│  │ LookUpItemController│                     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                      服务层 (Service)                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ ClassifyService │  │ LookUpItemService│                     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                      数据层 (DAO/Mapper)                      │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ ClassifyMapper  │  │ LookUpItemMapper│                  │
│  └─────────────────┘  └─────────────────┘                  │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                      数据存储 (MySQL)                         │
│  ┌────────────────────────┐  ┌────────────────────────┐    │
│  │ openplatform_lookup_   │  │ openplatform_lookup_   │    │
│  │    classify_t          │  │    item_t              │    │
│  └────────────────────────┘  └────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

```
market-openplatform/
└── src/main/java/com/huawei/it/market/
    ├── common/                       # 通用模块（各业务共用）
    │   ├── config/                    # 配置类
    │   │   ├── WebConfig.java         # Web配置
    │   │   └── PageHelperConfig.java  # 分页配置
    │   ├── controller/                # 通用控制器
    │   │   └── BaseController.java    # 基础控制器
    │   ├── enums/                     # 通用枚举
    │   │   ├── StatusEnum.java        # 状态枚举
    │   │   └── ErrorCodeEnum.java     # 错误码枚举
    │   ├── exception/                 # 通用异常
    │   │   ├── BusinessException.java # 业务异常
    │   │   └── GlobalExceptionHandler.java # 异常处理
    │   ├── model/                     # 通用模型
    │   │   ├── PageVO.java            # 分页VO
    │   │   ├── ResultDTO.java         # 统一返回
    │   │   └── PageDTO.java           # 分页请求
    │   ├── service/                   # 通用服务
    │   │   └── FileStorageService.java      # 文件存储接口
    │   │   └── impl/
    │   │       ├── LocalFileStorageService.java   # 本地实现（开发用）
    │   │       └── ObsFileStorageService.java     # OBS实现（生产用）
    │   └── util/                      # 通用工具
    │       └── StringUtils.java       # 字符串工具
    │
    └── modules/                      # 业务模块（按业务域划分）
        └── lookup/                   # LookUp业务模块
            ├── api/                   # API层（对外接口）
            │   ├── controller/        # 控制器
            │   │   ├── ClassifyController.java
            │   │   ├── LookUpItemController.java
            │   │   └── TaskController.java
            │   ├── dto/               # 数据传输对象
            │   │   ├── classify/
            │   │   │   ├── ClassifyCreateDTO.java
            │   │   │   ├── ClassifyUpdateDTO.java
            │   │   │   └── ClassifyQueryDTO.java
            │   │   ├── item/
            │   │   │   ├── ItemCreateDTO.java
            │   │   │   ├── ItemUpdateDTO.java
            │   │   │   └── ItemQueryDTO.java
            │   │   └── task/
            │   │       ├── TaskCreateDTO.java
            │   │       └── TaskQueryDTO.java
            │   ├── vo/                # 视图对象
            │   │   ├── classify/
            │   │   │   └── ClassifyVO.java
            │   │   ├── item/
            │   │   │   └── ItemVO.java
            │   │   └── task/
            │   │       └── TaskVO.java
            │   └── constants/         # 业务常量
            │       └── LookupConstants.java
            ├── service/               # 服务层
            │   ├── ClassifyService.java
            │   ├── ClassifyServiceImpl.java
            │   ├── LookUpItemService.java
            │   ├── LookUpItemServiceImpl.java
            │   ├── TaskService.java
            │   └── TaskServiceImpl.java
            ├── mapper/                # 数据访问层
            │   ├── ClassifyMapper.java
            │   ├── LookUpItemMapper.java
            │   └── TaskMapper.java
            └── domain/                # 领域层
                ├── entity/            # 实体类
                │   ├── ClassifyEntity.java
                │   ├── LookUpItemEntity.java
                │   └── TaskEntity.java
                └── enums/             # 业务枚举
                    ├── TaskTypeEnum.java
                    └── TaskStatusEnum.java
```

---

## 3. 数据库详细设计

**文件存储说明**：
- 当前环境无 OBS 桶，文件上传暂存本地文件系统
- 封装 FileStorageService 接口：
  ```java
  public interface FileStorageService {
      String upload(MultipartFile file, String path);
      void delete(String fileId);
      String getDownloadUrl(String fileId);
  }
  ```
- 本地实现：LocalFileStorageService（开发环境使用）
- OBS 实现：ObsFileStorageService（生产环境切换）



### 3.1 表结构定义

#### 3.1.1 分类表：openplatform_lookup_classify_t

| 字段名 | 数据类型 | 是否可空 | 默认值 | 说明 |
|--------|----------|----------|--------|------|
| classify_id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键，分类唯一标识 |
| classify_code | VARCHAR(100) | NO | - | 分类编码，同一路径下唯一 |
| classify_name | VARCHAR(100) | NO | - | 分类名称 |
| path | VARCHAR(100) | YES | '' | 路径，用于层级归类 |
| classify_desc | VARCHAR(4000) | YES | NULL | 分类描述 |
| status | TINYINT | NO | 1 | 状态：0-失效，1-有效 |
| create_by | VARCHAR(64) | NO | - | 创建人 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| last_update_by | VARCHAR(64) | YES | NULL | 最后更新人 |
| last_update_time | DATETIME | YES | NULL | 最后更新时间 |

#### 3.1.2 LookUp项表：openplatform_lookup_item_t

| 字段名 | 数据类型 | 是否可空 | 默认值 | 说明 |
|--------|----------|----------|--------|------|
| item_id | BIGINT UNSIGNED | NO | AUTO_INCREMENT | 主键，项唯一标识 |
| classify_id | BIGINT UNSIGNED | NO | - | 外键，关联分类表 |
| item_code | VARCHAR(100) | NO | - | 项编码，同一分类下唯一 |
| item_name | VARCHAR(100) | NO | - | 项名称 |
| item_value | VARCHAR(2000) | YES | NULL | 项值 |
| item_index | INT | YES | 0 | 排序序号 |
| item_desc | VARCHAR(4000) | YES | NULL | 项描述 |
| item_attr1 | TEXT | YES | NULL | 扩展属性1 |
| item_attr2 | TEXT | YES | NULL | 扩展属性2 |
| item_attr3 | TEXT | YES | NULL | 扩展属性3 |
| item_attr4 | TEXT | YES | NULL | 扩展属性4 |
| item_attr5 | TEXT | YES | NULL | 扩展属性5 |
| item_attr6 | TEXT | YES | NULL | 扩展属性6 |
| status | TINYINT | NO | 1 | 状态：0-失效，1-有效 |
| create_by | VARCHAR(64) | NO | - | 创建人 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| last_update_by | VARCHAR(64) | YES | NULL | 最后更新人 |
| last_update_time | DATETIME | YES | NULL | 最后更新时间 |

#### 3.1.3 异步任务表：openplatform_task_t

| 字段名 | 数据类型 | 是否可空 | 默认值 | 说明 |
|--------|----------|----------|--------|------|
| task_id | VARCHAR(64) | NO | - | 主键，任务ID |
| task_type | TINYINT | NO | - | 任务类型：1-IMPORT, 2-EXPORT |
| biz_type | TINYINT | NO | - | 业务类型：1-LOOKUP, 2-DATA_DICTIONARY |
| status | TINYINT | NO | 1 | 状态：1-PROCESSING, 2-COMPLETED, 3-FAILED |
| file_id | VARCHAR(128) | YES | NULL | 文件ID（OBS桶中文件的ID） |
| result | TEXT | YES | NULL | 结果描述（如：成功导入95条，跳过3条，失败2条；或任务执行失败: Excel解析异常） |
| params | TEXT | YES | NULL | 任务参数（如导出时的筛选条件 selectedIds, filters, creator 等） |
| create_by | VARCHAR(64) | NO | - | 创建人 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| last_update_by | VARCHAR(64) | YES | NULL | 最后更新人 |
| last_update_time | DATETIME | YES | NULL | 最后更新时间 |

**枚举值说明**:
- task_type: 1-导入, 2-导出
- biz_type: 1-LookUp, 2-数据字典
- status: 1-处理中, 2-已完成, 3-失败

### 3.2 索引设计

#### 3.2.1 分类表索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | classify_id | 主键索引 |
| uk_classify_code | 唯一 | path, classify_code | 同一路径下分类编码唯一 |
| idx_path | 普通 | path | 路径查询索引 |
| idx_status | 普通 | status | 状态筛选索引 |
| idx_create_time | 普通 | create_time | 创建时间排序索引 |

#### 3.2.2 LookUp项表索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | item_id | 主键索引 |
| uk_classify_item_code | 唯一 | classify_id, item_code | 同一分类下编码唯一 |
| idx_classify_id | 普通 | classify_id | 分类查询索引 |
| idx_status | 普通 | status | 状态筛选索引 |
| idx_item_index | 普通 | item_index | 排序索引 |
| idx_classify_status | 联合 | classify_id, status | 分类+状态联合查询 |

#### 3.2.3 异步任务表索引

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | task_id | 主键索引 |
| idx_file_id | 普通 | file_id | 文件ID索引 |


### 3.3 DDL 脚本

```sql
-- 分类表
CREATE TABLE IF NOT EXISTS `openplatform_lookup_classify_t` (
    `classify_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `classify_code` VARCHAR(100) NOT NULL COMMENT '分类编码',
    `classify_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `path` VARCHAR(100) DEFAULT '' COMMENT '路径',
    `classify_desc` VARCHAR(4000) DEFAULT NULL COMMENT '分类描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失效，1-有效',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_by` VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    `last_update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`classify_id`),
    UNIQUE KEY `uk_classify_code` (`path`, `classify_code`),
    KEY `idx_path` (`path`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LookUp分类表';

-- LookUp项表
CREATE TABLE IF NOT EXISTS `openplatform_lookup_item_t` (
    `item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '项ID',
    `classify_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `item_code` VARCHAR(100) NOT NULL COMMENT '项编码',
    `item_name` VARCHAR(100) NOT NULL COMMENT '项名称',
    `item_value` VARCHAR(2000) DEFAULT NULL COMMENT '项值',
    `item_index` INT DEFAULT 0 COMMENT '排序序号',
    `item_desc` VARCHAR(4000) DEFAULT NULL COMMENT '项描述',
    `item_attr1` TEXT COMMENT '扩展属性1',
    `item_attr2` TEXT COMMENT '扩展属性2',
    `item_attr3` TEXT COMMENT '扩展属性3',
    `item_attr4` TEXT COMMENT '扩展属性4',
    `item_attr5` TEXT COMMENT '扩展属性5',
    `item_attr6` TEXT COMMENT '扩展属性6',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失效，1-有效',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_by` VARCHAR(64) DEFAULT NULL COMMENT '最后更新人',
    `last_update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`item_id`),
    UNIQUE KEY `uk_classify_item_code` (`classify_id`, `item_code`),
    KEY `idx_classify_id` (`classify_id`),
    KEY `idx_status` (`status`),
    KEY `idx_item_index` (`item_index`),
    KEY `idx_classify_status` (`classify_id`, `status`),
    CONSTRAINT `fk_item_classify` FOREIGN KEY (`classify_id`) 
        REFERENCES `openplatform_lookup_classify_t` (`classify_id`) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LookUp项表';

-- 异步任务表
CREATE TABLE IF NOT EXISTS `openplatform_task_t` (
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `task_type` VARCHAR(20) NOT NULL COMMENT '任务类型：IMPORT/EXPORT',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型：LOOKUP/DATA_DICTIONARY',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/COMPLETED/FAILED',
    `file_id` VARCHAR(128) COMMENT '文件ID（OBS桶中文件的ID）',
    `result` TEXT COMMENT '结果描述（如：成功导入95条，跳过3条，失败2条）',
    `params` TEXT COMMENT '任务参数（如导出时的筛选条件 selectedIds, filters, creator 等）',
    `create_by` VARCHAR(64) NOT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_update_by` VARCHAR(64) COMMENT '最后更新人',
    `last_update_time` DATETIME COMMENT '最后更新时间',
    PRIMARY KEY (`task_id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';
```

---

## 4. 接口设计

### 4.1 接口概览

| 接口分类 | 接口数量 | 说明 |
|----------|----------|------|
| 分类管理接口 | 5 | CRUD + 详情 |
| LookUp项管理接口 | 5 | CRUD + 详情 |
| 异步导入接口 | 1 | 提交任务 |
| 异步导出接口 | 1 | 提交任务 |
| 任务中心接口 | 2 | 分页查询任务列表 + 查询任务进度 |
| 通用文件下载接口 | 1 | 根据fileId获取下载链接 |
| **总计** | **15** | - |

### 4.2 统一响应格式

```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码：200成功，其他为错误码 |
| message | String | 响应消息 |
| data | Object | 响应数据，可为空 |

### 4.3 分类管理接口

#### 4.3.1 查询分类列表

- **URL**: `/api/v1/lookup/classify/list`
- **Method**: GET
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyCode | String | 否 | 分类编码，模糊匹配 |
| classifyName | String | 否 | 分类名称，模糊匹配 |
| classifyDesc | String | 否 | 描述，模糊匹配 |
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
        "classifyId": 1,
        "classifyCode": "USER_TYPE",
        "classifyName": "用户类型",
        "path": "system/user",
        "classifyDesc": "用户身份分类",
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
  3. 实时计算每个分类下的 LookUp 项数量
  4. 返回分页结果（使用 PageHelper 分页）

#### 4.3.2 新增分类

- **URL**: `/api/v1/lookup/classify`
- **Method**: POST
- **Content-Type**: application/json
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyCode | String | 是 | 分类编码，1-100字符，支持字母、数字、下划线、点、横杠，同一路径下唯一 |
| classifyName | String | 是 | 分类名称，1-100字符 |
| path | String | 否 | 路径，0-100字符 |
| classifyDesc | String | 否 | 描述，0-4000字符 |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

- **业务逻辑**:
  1. 校验 classifyCode 格式和唯一性
  2. 校验 classifyName 非空
  3. 填充 createBy/createTime、lastUpdateBy/lastUpdateTime（lastUpdateBy=createBy, lastUpdateTime=createTime）
  4. 插入数据，返回创建结果

#### 4.3.3 编辑分类

- **URL**: `/api/v1/lookup/classify/{classifyId}`
- **Method**: PUT
- **Content-Type**: application/json
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyId | Long | 是 | 分类ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyName | String | 是 | 分类名称，1-100字符 |
| path | String | 否 | 路径，0-100字符 |
| classifyDesc | String | 否 | 描述，0-4000字符 |
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
  - 分类编码(classifyCode)和路径(path)不可修改

- **业务逻辑**:
   1. 校验 classifyId 存在
   2. 更新分类信息（不含 classifyCode 和 path）
   3. 记录更新人和更新时间

#### 4.3.4 删除分类

- **URL**: `/api/v1/lookup/classify/{classifyId}`
- **Method**: DELETE
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyId | Long | 是 | 分类ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

- **业务约束**:
  - 删除前需校验分类状态为失效(0)，有效(1)的分类不可删除

- **业务逻辑**:
   1. 校验 classifyId 存在
   2. 校验状态为失效(0)
   3. 物理删除分类及其下所有 LookUp 项
   4. 记录操作日志

#### 4.3.5 获取分类详情

- **URL**: `/api/v1/lookup/classify/{classifyId}`
- **Method**: GET
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyId | Long | 是 | 分类ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "classifyId": 1,
    "classifyCode": "USER_TYPE",
    "classifyName": "用户类型",
    "path": "system/user",
    "classifyDesc": "用户身份分类",
    "status": 1,
    "createBy": "admin",
    "createTime": "2024-01-10 09:00:00",
    "lastUpdateBy": "admin",
    "lastUpdateTime": "2024-01-10 09:00:00"
  }
}
```

### 4.4 LookUp项管理接口

#### 4.4.1 查询LookUp项列表

- **URL**: `/api/v1/lookup/classify/{classifyId}/items`
- **Method**: GET
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyId | Long | 是 | 分类ID |

- **查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemCode | String | 否 | 项编码，模糊匹配 |
| itemName | String | 否 | 项名称，模糊匹配 |
| status | Integer | 否 | 状态：0-失效，1-有效，空-全部 |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "classifyInfo": {
      "classifyId": 1,
      "classifyCode": "USER_TYPE",
      "classifyName": "用户类型"
    },
    "list": [
      {
        "itemId": 1,
        "itemCode": "ADMIN",
        "itemName": "管理员",
        "itemValue": "1",
        "itemDesc": "系统管理员",
        "itemIndex": 1,
        "status": 1,
        "createBy": "admin",
        "createTime": "2024-01-15 10:30:00",
        "lastUpdateBy": "admin",
        "lastUpdateTime": "2024-01-20 11:00:00"
      }
    ],
    "totalCnt": 5,
    "pageVO": {
      "totalRows": 5,
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
  1. 校验分类存在
  2. 按 item_index 升序排列，item_index 相同时按创建时间倒序
  3. 返回分页结果和分类信息（使用 PageHelper 分页）

#### 4.4.2 新增LookUp项

- **URL**: `/api/v1/lookup/classify/{classifyId}/items`
- **Method**: POST
- **Content-Type**: application/json
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| classifyId | Long | 是 | 分类ID |

- **业务约束**:
  - 每个分类下最多支持X个LookUp项，X值从数据字典中读取（如：LOOKUP_ITEM_MAX_COUNT）

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemCode | String | 是 | 项编码，1-100字符，支持字母、数字、下划线、点、横杠，同一分类下唯一 |
| itemName | String | 是 | 项名称，1-100字符 |
| itemValue | String | 否 | 项值，0-2000字符 |
| itemIndex | Integer | 否 | 排序序号，正整数 |
| itemDesc | String | 否 | 描述，0-4000字符 |
| itemAttr1~6 | String | 否 | 扩展属性1-6，text类型 |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

#### 4.4.3 编辑LookUp项

- **URL**: `/api/v1/lookup/items/{itemId}`
- **Method**: PUT
- **Content-Type**: application/json
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Long | 是 | 项ID |

- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemCode | String | 否 | 项编码（不可修改，传入会被忽略） |
| itemName | String | 是 | 项名称，1-100字符 |
| itemValue | String | 否 | 项值，0-2000字符 |
| itemIndex | Integer | 否 | 排序序号，正整数 |
| itemDesc | String | 否 | 描述，0-4000字符 |
| itemAttr1~6 | String | 否 | 扩展属性1-6 |
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
  - 项编码(itemCode)不可修改

- **业务逻辑**:
   1. 校验 itemId 存在
   2. 更新项信息（不含 itemCode）
   3. 记录更新人和更新时间

#### 4.4.4 删除LookUp项

- **URL**: `/api/v1/lookup/items/{itemId}`
- **Method**: DELETE
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Long | 是 | 项ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": null
}
```

#### 4.4.5 获取LookUp项详情

- **URL**: `/api/v1/lookup/items/{itemId}`
- **Method**: GET
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Long | 是 | 项ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "itemId": 1,
    "classifyId": 1,
    "classifyName": "用户类型",
    "itemCode": "ADMIN",
    "itemName": "管理员",
    "itemValue": "1",
    "itemIndex": 1,
    "itemDesc": "系统管理员",
    "itemAttr1": "super",
    "itemAttr2": null,
    "itemAttr3": null,
    "itemAttr4": null,
    "itemAttr5": null,
    "itemAttr6": null,
    "status": 1,
    "createBy": "admin",
    "createTime": "2024-01-15 10:30:00",
    "lastUpdateBy": "admin",
    "lastUpdateTime": "2024-01-15 10:30:00"
  }
}
```

- **前端交互说明**:
  - 详情面板仅展示信息，点击编辑按钮才进入编辑状态
  - 点击编辑 → 右侧滑出编辑面板 → 修改数据 → 点击保存/取消 → 关闭面板

#### 4.4.6 批量导入（异步）

**接口1: 提交导入任务**

- **URL**: `/api/v1/lookup/import/async`
- **Method**: POST
- **Content-Type**: multipart/form-data
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | Excel文件，支持 .xlsx/.xls，Excel包含两个Sheet：Sheet1为分类信息，Sheet2为项信息 |

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
  "code": 400,
  "message": "单次最多处理1000条数据，请分批操作",
  "data": null
}
```

- **错误响应示例**（单分类项数超限）:
```json
{
  "code": 400,
  "message": "分类 [用户类型] 导入后项数量将超过限制（已有99条 + 导入2条 > 最多100条），请调整",
  "data": null
}
```

- **业务逻辑**:
  1. 接收 Excel 文件，解析两个Sheet
  2. 校验Excel总行数，超出1000条则返回错误
  3. 统计每个分类本次导入的项数，查询每个分类已有的项数
  4. 若 (已有项数 + 导入项数) > LOOKUP_ITEM_MAX_COUNT，则返回错误（需指明哪个分类超限）
  5. 创建任务记录
  6. 启动异步线程处理

- **业务约束**:
  - 提交前校验，超出则返回错误，前端提示：单次最多处理1000条数据，请分批操作
  - 每个分类下最多支持X个LookUp项（X值从数据字典 LOOKUP_ITEM_MAX_COUNT 读取），导入时校验：若 (该分类已有项数 + 本次导入项数) > X，则报错返回
  - 总数1000条限制和每分类限制均在提交时校验

   - **前端交互说明**:
    - 选择文件后直接开始导入，确认时会检查数据量，超出1000条则提示
    - 提交导入任务后显示任务通知弹窗
  - 导入完成后生成导入结果报告（成功/跳过/失败明细），可在任务中心下载

**异步处理流程**:
1. **文件上传**: 用户上传Excel文件，后端调用 FileStorageService.upload() 保存文件，返回 file_id
2. **创建任务**: 任务表记录 (task_id, status=PROCESSING, file_id=OBS文件ID)
3. **异步处理**: 创建任务后立即创建异步线程处理：下载Excel、解析数据、插入数据库
   - 通过 file_id 从OBS下载Excel文件
   - 解析两个Sheet（Sheet1：分类信息，Sheet2：项信息）
   - 先处理分类Sheet：校验并插入/更新分类
   - 再处理项Sheet：按classifyCode和path匹配分类，插入/更新项
   - 校验每个分类下的项数量（从数据字典读取LOOKUP_ITEM_MAX_COUNT），超限则跳过并记录
   - 实时更新任务进度 (progress, success_count, fail_count)
4. **完成处理**: 
   - 生成导入结果报告（Excel：分类编码、项编码、项名称、导入结果、失败原因）
   - 上传到OBS，file_id 覆盖为结果报告的OBS路径
   - 更新任务状态为 COMPLETED，result 字段记录结果描述（如任务失败则记录失败原因）
5. **用户下载**: 任务中心点击下载，通过 task_id 查到 file_id，从OBS下载结果报告

**导入Excel格式（两个Sheet，包含分类信息和项信息）**:

**Sheet 1: 分类信息**

| 列号 | 字段名 | 说明 |
|:----:|:-------|:-----|
| A | 分类编码 | classify_code，必填 |
| B | 分类名称 | classify_name，必填 |
| C | 路径 | path |
| D | 分类描述 | classify_desc |
| E | 状态 | status（1-有效，0-失效） |

**Sheet 2: 项信息**

| 列号 | 字段名 | 说明 |
|:----:|:-------|:-----|
| A | 分类编码 | classify_code，必填（关联分类Sheet） |
| B | 路径 | path（关联分类Sheet） |
| C | 项编码 | item_code，必填，同分类下唯一 |
| D | 项名称 | item_name |
| E | 项值 | item_value |
| F | 排序 | item_index |
| G | 项描述 | item_desc |
| H~M | 扩展属性1-6 | item_attr1~6 |
| N | 状态 | status（1-有效，0-失效） |

**说明**：第二Sheet中的classifyCode和path用于匹配所属分类

#### 4.4.7 导出LookUp项（异步）

**接口1: 提交导出任务**

- **URL**: `/api/v1/lookup/export/async`
- **Method**: POST
- **Content-Type**: application/json
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| params | Object | 否 | 导出参数JSON对象，包含筛选条件和选中的ID |

**params 对象结构**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| selectedIds | Array | 否 | 勾选的分类ID列表，若有值则只导出这些分类及其项（优先级高于filters，会精确导出指定分类） |
| filters | Object | 否 | 筛选条件对象，用于对导出数据进行精细过滤，与selectedIds配合使用时取交集 |
| filters.classifyCode | String | 否 | 分类编码，模糊匹配 |
| filters.classifyName | String | 否 | 分类名称，模糊匹配 |
| filters.classifyDesc | String | 否 | 分类描述，模糊匹配 |
| filters.status | Integer | 否 | 状态筛选，1-有效，0-失效，空-全部 |

- **请求示例**:
```json
{
  "params": {
    "selectedIds": [1, 2, 3],
    "filters": {
      "classifyCode": "USER",
      "classifyName": "用户",
      "classifyDesc": "测试",
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

- **错误响应示例**（数据量超出1000条）:
```json
{
  "code": 400,
  "message": "单次最多处理1000条数据，请分批操作",
  "data": null
}
```

- **业务逻辑**:
  1. 解析 params 参数，提取 selectedIds 和 filters
  2. 校验数据量，超出1000条时自动截断只返回前1000条
  3. 生成任务ID，将 params JSON 存入任务记录
  4. 创建异步任务记录
  5. 返回任务ID

- **前端交互说明**:
  - 导出根据当前页面筛选条件和勾选行确定范围
  - selectedIds 和 filters 存入任务记录，异步任务执行时读取
  - 导出需要二次确认，确认时会检查数据量，超出1000条则提示
  - 提交导出任务后显示任务通知弹窗

**异步处理流程**:
1. 创建导出任务后立即启动异步线程处理
2. 根据 params 中的 selectedIds 或 filters 查询数据
3. 若有 selectedIds，按勾选ID查询；否则按 filters 条件查询
4. 生成两个Sheet的Excel（Sheet1：分类信息，Sheet2：项信息）
5. Sheet1写入所有涉及的分类信息
6. Sheet2写入所有项信息（含classifyCode和path用于关联）
7. 使用 Apache POI 生成 Excel 文件
8. 调用 FileStorageService.upload() 保存文件
9. 更新任务状态为已完成

#### 4.4.8 任务中心

| 接口分类 | 接口数量 | 说明 |
|----------|----------|------|
| 任务查询接口 | 2 | 分页查询任务列表 + 查询任务进度 |
| **总计** | **2** | - |

**接口1: 查询任务列表**

- **URL**: `/api/v1/lookup/task/list`
- **Method**: GET
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bizType | String | 否 | 业务类型：LOOKUP/DATA_DICTIONARY，空-全部 |
| taskType | String | 否 | 任务类型：IMPORT/EXPORT，空-全部 |
| status | String | 否 | 状态：PROCESSING/COMPLETED/FAILED，空-全部 |
| pageNum | Integer | 否 | 页码，默认1，最大1000 |
| pageSize | Integer | 否 | 每页条数，默认10，最大1000 |

- **业务约束**:
  - 按创建时间倒序排列

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "list": [
      {
        "taskId": "IMPORT-000001",
        "bizType": "LOOKUP",
        "bizTypeName": "LookUp",
        "taskType": "IMPORT",
        "status": "COMPLETED",
        "result": "成功导入95条，跳过3条，失败2条",
        "fileName": "LookUp_Import_Result_20240115153000.xlsx",
        "createTime": "2024-01-15 15:30:00",
        "completeTime": "2024-01-15 15:30:30"
      }
    ],
    "totalCnt": 10,
    "pageVO": {
      "totalRows": 10,
      "curPage": 1,
      "pageSize": 10,
      "startIndex": 1,
      "endIndex": 10,
      "maxPageSize": 1000
    }
  }
}
```

---

**接口2: 查询任务进度**

- **URL**: `/api/v1/lookup/task/progress/{taskId}`
- **Method**: GET
- **路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "taskId": "IMPORT-000001",
    "taskType": "IMPORT",
    "status": "PROCESSING",
    "progress": 50,
    "result": "处理中，已导入 500 条",
    "fileId": null,
    "createTime": "2024-01-15 15:30:00",
    "completeTime": null
  }
}
```

- **状态说明**:
  - `PROCESSING`: 处理中
  - `COMPLETED`: 已完成（可下载）
  - `FAILED`: 失败

- **业务逻辑**:
  1. 根据 taskId 查询任务记录
  2. 不存在则抛 404
  3. 返回任务进度、状态、fileId（若已完成）

#### 4.x.x 文件下载（通用）

- **URL**: `/api/v1/lookup/file/download`
- **Method**: GET
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | String | 是 | 文件ID |

- **响应示例**:
```json
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "downloadUrl": "/api/v1/lookup/file/download?token=xxx&expires=3600"
  }
}
```

- **业务逻辑**:
  1. 根据 fileId 查询文件信息
  2. 生成带时效的下载URL
  3. 返回下载链接

## 4.5 后端详细设计

### 4.5.1 异常处理设计

**错误码枚举（OpenplatformErrorCodeEnum）：**

**异常处理机制：**
- 不使用全局异常拦截器，每个接口/Service方法根据具体业务逻辑返回对应的错误码
- 所有接口统一返回 `BaseResultOpenPlatform<T>` 格式，通过 `code` 字段区分成功/失败
- 每个接口的完整错误码定义如下：

**错误码明细：**

| 错误码 | 业务场景 | 说明 |
|:------:|:---------|:-----|
| 200 | 操作成功 | 所有正常返回 |
| 400 | 参数校验失败 | 必填字段为空、长度超限、格式错误 |
| 40002 | 导入数据超限 | 超出1000条限制或单分类项数超限 |
| 40401 | 分类不存在 | classifyId 在数据库中不存在 |
| 40402 | LookUp项不存在 | itemId 在数据库中不存在 |
| 40901 | 分类编码已存在 | 新增时 classifyCode 与已有记录冲突 |
| 40902 | 项编码已存在 | 同一分类下 itemCode 与已有记录冲突 |
| 500 | 系统内部错误 | 数据库异常、文件解析异常等未预期错误 |

### 4.5.2 参数校验设计

**新增/编辑接口的字段校验规则：**

| DTO字段 | 校验规则 |
|:--------|:---------|
| classifyCode | 必填，1-100字符，字母数字下划线点横杠，同一路径下唯一 |
| classifyName / itemName | 必填，1-100字符 |
| path | 可选，0-100字符 |
| classifyDesc / itemDesc | 可选，0-4000字符 |
| itemCode | 必填，1-100字符，字母数字下划线点横杠，同一分类下唯一 |
| itemValue | 可选，0-2000字符 |
| itemIndex | 正整数 |
| itemAttr1~6 | 可选 |
| status | 可选（编辑时传则变更状态），0或1 |

**校验方式：**
- 基础格式校验（必填、长度、格式）：使用 `@NotBlank`、`@Size`、`@Pattern` 等注解，在 Controller 层自动校验
- 业务校验（编码唯一性、分类是否存在）：在 Service 层手动校验，不满足则抛出 BusinessException

### 4.5.3 Service 业务逻辑流程

**ClassifyService：**

| 方法 | 业务逻辑流程 |
|:----|:------------|
| 查询列表 | ① 组装查询条件 → ② MyBatis-Plus 分页查询 → ③ 统计每个分类下的 LookUp 项数量 → ④ 返回分页结果 |
| 新增 | ① 校验 classifyCode 格式 → ② 校验 code 唯一性 → ③ 填充 createBy/createTime、lastUpdateBy/lastUpdateTime（lastUpdateBy=createBy, lastUpdateTime=createTime）→ ④ 插入数据库 → ⑤ 返回操作成功 |
| 编辑 | ① 校验 classifyId 存在 → ② 分类编码(classifyCode)和路径(path)不可修改 → ③ 校验 classifyName 非空 → ④ 若传了 status 则更新状态 → ⑤ 填充 lastUpdateBy/lastUpdateTime → ⑥ 更新数据库 |
| 删除 | ① 校验 classifyId 存在 → ② 校验状态为失效(0) → ③ 物理删除分类 → ④ 级联物理删除其下所有 LookUp 项 → ⑤ 记录操作日志 |
| 获取详情 | ① 根据 classifyId 查询 → ② 不存在则抛 404 → ③ 返回完整信息 |

**LookUpItemService：**

| 方法 | 业务逻辑流程 |
|:----|:------------|
| 查询列表 | ① 校验 classifyId 存在 → ② 组装查询条件（itemCode/itemName/status 模糊匹配）→ ③ 分页查询 → ④ 按 item_index 升序排列，item_index 相同时按创建时间倒序 → ⑤ 返回结果 |
| 新增 | ① 校验 classifyId 存在 → ② 校验 itemCode 格式和一分类下唯一性 → ③ 校验分类下项数量未超限（从数据字典读取LOOKUP_ITEM_MAX_COUNT）→ ④ 填充 createBy/createTime、lastUpdateBy/lastUpdateTime（lastUpdateBy=createBy, lastUpdateTime=createTime）→ ⑤ 插入数据库 |
| 编辑 | ① 校验 itemId 存在 → ② itemCode 不可修改 → ③ 若传了 status 则更新状态 → ④ 填充 lastUpdateBy/lastUpdateTime → ⑤ 更新数据库（不含 itemCode） |
| 删除 | ① 校验 itemId 存在 → ② 物理删除 → ③ 记录操作日志 |
| 获取详情 | ① 根据 itemId 查询 → ② 不存在则抛 404 |
| 批量导入 | ① 接收Excel → ② 解析两个Sheet → ③ 校验Excel总行数 → ④ 统计并校验每分类项数（已有+导入>X则报错）→ ⑤ 创建任务记录 → ⑥ 启动异步线程 |
| 导出 | ① 组装查询条件 → ② 生成任务ID → ③ 创建异步任务记录并立即启动异步线程处理 → ④ 异步处理：查询数据（最大1000条）→ ⑤ 使用 Apache POI 构建两个Sheet的 Excel → ⑥ 保存文件并更新任务状态 |

**LookUpTaskService：**

| 方法 | 业务逻辑流程 |
|:----|:------------|
| 提交导入任务 | ① 校验分类ID存在 → ② 接收 Excel 文件 → ③ 提交前校验Excel数据量，超出1000条则返回错误 → ④ 生成任务ID → ⑤ 创建任务记录 → ⑥ 立即启动异步线程处理导入 → ⑦ 返回任务ID |
| 查询导入进度 | ① 根据 taskId 查询任务 → ② 不存在则抛 404 → ③ 返回任务进度和结果 |
| 提交导出任务 | ① 解析params → ② 提取selectedIds和filters → ③ 创建任务记录（params存入JSON字段）→ ④ 立即启动异步线程处理导出 → ⑤ 返回任务ID |
| 查询导出进度 | ① 根据 taskId 查询任务 → ② 不存在则抛 404 → ③ 返回任务进度和下载地址 |
| 下载导出文件 | ① 校验任务状态为COMPLETED → ② 读取文件 → ③ 返回文件流 |

### 4.5.4 Mapper 层设计

**分页查询实现方式：**
- 使用 PageHelper 实现分页
- 条件查询使用 MyBatis-Plus 的 `LambdaQueryWrapper` 动态拼接 WHERE 条件
- 查询条件示例：
  - classifyCode / itemCode：`like` 模糊匹配
  - classifyName / itemName：`like` 模糊匹配
  - status：`eq` 精确匹配（为空则忽略）
  - classifyId（LookUp项）：`eq` 精确匹配（必填）

**批量操作：**
- 批量导入使用 MyBatis-Plus 的 `saveBatch` 方法分批插入
- 每批 100 条，避免一次插入过多数据导致内存溢出

**PageHelper 使用方式：**
```java
PageHelper.startPage(pageNum, pageSize);
List<Entity> list = mapper.selectList(queryWrapper);
Page<Object> page = PageHelper.getLocalPage();
PageVO pageVO = new PageVO();
pageVO.setTotalRows((int) page.getTotal());
pageVO.setCurPage((int) page.getPageNum());
pageVO.setPageSize((int) page.getPageSize());
pageVO.setStartIndex((int) page.getStartRow());
pageVO.setEndIndex((int) page.getEndRow());
pageVO.setMaxPageSize(1000);
```

### 4.5.5 分页设计

**技术选型**：使用 PageHelper 实现分页

**分页返回值结构**：

```java
{
  "code": 200,
  "messageZh": "操作成功", "messageEn": "success",
  "data": {
    "list": [...],           // Object List，数据列表
    "totalCnt": 100,         // Long，总记录数
    "pageVO": {
      "totalRows": 100,      // long，总记录数
      "curPage": 1,          // long，当前页
      "pageSize": 10,        // long，每页条数
      "startIndex": 1,       // long，起始索引
      "endIndex": 10,        // long，结束索引
      "maxPageSize": 1000    // long，最大每页条数
    }
  }
}
```

**分页参数校验**：
- pageSize：最小10，最大1000，默认10
- pageNum：最小1，默认1
- 超出范围自动修正

### 4.5.6 配置项设计

| 配置项 | 说明 | 建议值 |
|:-------|:-----|:-------|
| 分页默认大小 (pageSize) | 列表每页默认条数 | 10 |
| 分页最大大小 (maxPageSize) | 单次查询最大条数限制 | 1000 |
| 分页最小大小 | 单次查询最小条数限制 | 10 |
| Excel上传大小 | 导入文件大小限制 | 10MB |
| Excel支持格式 | 允许上传的文件扩展名 | .xlsx, .xls |
| 批量导入每批条数 | saveBatch 每批插入数 | 100 |
| 异步任务线程池大小 | 处理异步任务的线程数 | 5 |
| 文件存储方式 | 本地/OBS | local=本地，obs=OBS | local |
| 本地文件存储路径 | 开发者本地文件存储根目录 | /tmp/market-files |


| 跨域配置 | 开发环境允许前端跨域访问 | 允许 localhost:3000 |

## 5. 前端设计

### 5.1 页面路由设计

| 路由 | 页面 | 说明 |
|------|------|------|
| `/lookup/classify` | 分类列表页 | 分类管理主页面 |
| `/lookup/classify/:id/items` | LookUp项列表页 | 某分类下的项管理页面 |
| `/lookup/task` | 任务中心 | 统一查看导入导出任务 |

### 5.2 组件划分

```
src/views/lookup/
├── classify/
│   ├── index.vue              # 分类列表页
│   ├── components/
│   │   ├── ClassifyTable.vue   # 分类表格
│   │   ├── ClassifyFilter.vue  # 分类筛选区
│   │   ├── ClassifyModal.vue   # 新增/编辑分类弹窗
│   │   └── ClassifyPagination.vue # 分页组件
│   └── composables/
│       └── useClassify.js      # 分类相关逻辑
│
└── item/
    ├── index.vue               # LookUp项列表页
    ├── components/
    │   ├── ItemTable.vue       # 项表格
    │   ├── ItemFilter.vue      # 项筛选区
    │   ├── ItemDetailPanel.vue # 右侧详情/编辑面板
    │   ├── ImportModal.vue     # 导入弹窗
    │   └── ItemPagination.vue  # 分页组件
    └── composables/
        └── useItem.js          # 项相关逻辑
```

### 5.3 状态管理

```javascript
// hooks/useLookup.js
import { useState, useCallback } from 'react'

export function useLookupState() {
  const [classifyList, setClassifyList] = useState([])
  const [classifyTotal, setClassifyTotal] = useState(0)
  const [classifyQuery, setClassifyQuery] = useState({
    classifyCode: '',
    classifyName: '',
    classifyDesc: '',
    status: '',
    pageNum: 1,
    pageSize: 10
  })
  
  const [currentClassify, setCurrentClassify] = useState(null)
  
  const [itemList, setItemList] = useState([])
  const [itemTotal, setItemTotal] = useState(0)
  const [itemQuery, setItemQuery] = useState({
    itemCode: '',
    itemName: '',
    status: '',
    pageNum: 1,
    pageSize: 10
  })
  
  const [detailPanelVisible, setDetailPanelVisible] = useState(false)
  const [detailMode, setDetailMode] = useState('view')
  const [currentItem, setCurrentItem] = useState(null)
  
  const [importTask, setImportTask] = useState(null)
  const [exportTask, setExportTask] = useState(null)
  const [taskPolling, setTaskPolling] = useState(null)
  
  const classifyOps = useCallback(async () => { /* ... */ }, [])
  const createClassify = useCallback(async (data) => { /* ... */ }, [])
  const updateClassify = useCallback(async (id, data) => { /* ... */ }, [])
  const deleteClassify = useCallback(async (id) => { /* ... */ }, [])
  
  const itemOps = useCallback(async (classifyId) => { /* ... */ }, [])
  const createItem = useCallback(async (classifyId, data) => { /* ... */ }, [])
  const updateItem = useCallback(async (itemId, data) => { /* ... */ }, [])
  const deleteItem = useCallback(async (itemId) => { /* ... */ }, [])
  const fetchItemDetail = useCallback(async (itemId) => { /* ... */ }, [])
  
  const downloadTemplate = useCallback(async () => { /* ... */ }, [])
  const submitImportTask = useCallback(async (file, classifyId) => { /* ... */ }, [])
  const getImportProgress = useCallback(async (taskId) => { /* ... */ }, [])
  const submitExportTask = useCallback(async (params) => { /* ... */ }, [])
  const getExportProgress = useCallback(async (taskId) => { /* ... */ }, [])
  const downloadExportFile = useCallback(async (taskId) => { /* ... */ }, [])
  
  return {
    classifyList, classifyTotal, classifyQuery, setClassifyQuery,
    currentClassify, setCurrentClassify,
    itemList, itemTotal, itemQuery, setItemQuery,
    detailPanelVisible, setDetailPanelVisible,
    detailMode, setDetailMode,
    currentItem, setCurrentItem,
    importTask, exportTask, taskPolling, setTaskPolling,
    fetchClassifyList, createClassify, updateClassify, deleteClassify,
    fetchItemList, createItem, updateItem, deleteItem, fetchItemDetail,
    downloadTemplate, submitImportTask, getImportProgress,
    submitExportTask, getExportProgress, downloadExportFile
  }
}
```

### 5.4 接口调用封装

```javascript
// api/lookup.js
import request from '@/utils/request'

export const lookupApi = {
  // 分类管理
  getClassifyList: (params) => request.get('/api/v1/lookup/classify/list', { params }),
  createClassify: (data) => request.post('/api/v1/lookup/classify', data),
  updateClassify: (id, data) => request.put(`/api/v1/lookup/classify/${id}`, data),
  deleteClassify: (id) => request.delete(`/api/v1/lookup/classify/${id}`),
  getClassifyDetail: (id) => request.get(`/api/v1/lookup/classify/${id}`),
  
  // LookUp项管理
  getItemList: (classifyId, params) => request.get(`/api/v1/lookup/classify/${classifyId}/items`, { params }),
  createItem: (classifyId, data) => request.post(`/api/v1/lookup/classify/${classifyId}/items`, data),
  updateItem: (itemId, data) => request.put(`/api/v1/lookup/items/${itemId}`, data),
  deleteItem: (itemId) => request.delete(`/api/v1/lookup/items/${itemId}`),
  getItemDetail: (itemId) => request.get(`/api/v1/lookup/items/${itemId}`),
  
  // 异步导入
  downloadTemplate: () => request.get('/api/v1/lookup/import/template', { responseType: 'blob' }),
  submitImportTask: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/v1/lookup/import/async', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getImportProgress: (taskId) => request.get(`/api/v1/lookup/import/progress/${taskId}`),
  
  // 异步导出
  submitExportTask: (params) => request.post('/api/v1/lookup/export/async', params),
  getExportProgress: (taskId) => request.get(`/api/v1/lookup/export/progress/${taskId}`),
  downloadExportFile: (taskId) => request.get(`/api/v1/lookup/export/download/${taskId}`, { 
    responseType: 'blob' 
  })
}
```

---

## 6. 关键决策（ADR）

以下架构决策记录已单独创建文档：

| ADR编号 | 标题 | 状态 |
|---------|------|------|
| ADR-001 | 物理删除 vs 逻辑删除 | ACCEPTED |
| ADR-002 | status 字段设计（0失效/1有效） | ACCEPTED |
| ADR-003 | Excel 导入方案选择 | ACCEPTED |
详见同目录下 `ADR-001.md` ~ `ADR-003.md` 文件。

---

## 7. 风险评估

### 7.1 技术风险

| 风险项 | 可能性 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| Excel 大数据量导入内存溢出 | 中 | 高 | 使用 Apache POI SXSSF 流式读取，限制单次导入最大条数（如1000条） |
| 频繁查询导致数据库压力大 | 中 | 中 | 列表查询接口合理分页，避免一次性加载过多数据 |
| 编码重复校验并发问题 | 低 | 高 | 数据库层添加唯一索引约束，作为最后防线 |
| 异步任务线程池资源耗尽 | 中 | 高 | 合理配置线程池大小，添加任务队列和拒绝策略 |
| 异步任务执行失败未通知 | 低 | 中 | 任务状态持久化，前端手动刷新查看，失败时显示错误信息 |


### 7.2 依赖风险

| 风险项 | 可能性 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| Apache POI 版本兼容性 | 低 | 中 | 使用稳定版本 5.x，升级前充分测试 |
| MyBatis-Plus 分页插件配置 | 低 | 低 | 参考官方文档配置，添加配置类示例 |

### 7.3 时间风险

| 风险项 | 可能性 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| 异步任务处理复杂度高 | 中 | 高 | 使用成熟的异步框架（如Spring @Async），充分的单元测试和集成测试 |
| 导入导出功能复杂度高 | 中 | 中 | 分阶段实现：先实现基础导入导出，再优化异步处理和进度展示 |
| 前端交互细节多 | 中 | 中 | 参考 demo.html 实现，复用成熟的 React 组件，添加进度条和状态提示 |

---

## 8. 工作量估算

| 模块 | 功能点 | 估算工时（人天） |
|------|--------|------------------|
| 数据库 | DDL 脚本编写、索引优化（含任务表） | 0.5 |
| 后端-分类管理 | 5个接口开发 + 单元测试 | 1.5 |
| 后端-LookUp项管理 | 5个接口开发 + 单元测试 | 1.5 |
| 后端-异步任务管理 | 6个接口开发 + 异步处理逻辑 + 单元测试 | 2 |
| 前端-分类管理页面 | 列表、筛选、弹窗 | 2 |
| 前端-LookUp项管理页面 | 列表、详情面板、导入导出 | 3 |
| 前端-任务进度管理 | 手动刷新查询、状态展示 | 1 |
| 联调测试 | 前后端联调、集成测试 | 1.5 |
| **合计** | | **13** |

---

## 9. 文件影响清单

### 9.1 新增文件

```
【后端】
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/classify/ClassifyCreateDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/classify/ClassifyUpdateDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/classify/ClassifyQueryDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/item/ItemCreateDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/item/ItemUpdateDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/task/TaskCreateDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/dto/task/TaskQueryDTO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/vo/classify/ClassifyVO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/vo/item/ItemVO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/vo/task/TaskVO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/api/vo/task/TaskProgressVO.java
- [NEW] src/main/java/com/huawei/it/market/lookup/domain/entity/ClassifyEntity.java
- [NEW] src/main/java/com/huawei/it/market/lookup/domain/entity/LookUpItemEntity.java
- [NEW] src/main/java/com/huawei/it/market/lookup/domain/entity/LookUpTaskEntity.java
- [NEW] src/main/java/com/huawei/it/market/lookup/domain/enums/TaskTypeEnum.java
- [NEW] src/main/java/com/huawei/it/market/lookup/domain/enums/TaskStatusEnum.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/controller/ClassifyController.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/controller/LookUpItemController.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/controller/LookUpTaskController.java（异步任务接口）
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/ClassifyService.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/ClassifyServiceImpl.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/LookUpItemService.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/LookUpItemServiceImpl.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/LookUpTaskService.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/LookUpTaskServiceImpl.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/service/AsyncTaskExecutor.java（异步任务执行器）
- [NEW] src/main/java/com/huawei/it/market/lookup/service/mapper/ClassifyMapper.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/mapper/LookUpItemMapper.java
- [NEW] src/main/java/com/huawei/it/market/lookup/service/mapper/LookUpTaskMapper.java
- [NEW] src/main/resources/mapper/ClassifyMapper.xml
- [NEW] src/main/resources/mapper/LookUpItemMapper.xml
- [NEW] src/main/resources/mapper/LookUpTaskMapper.xml

【前端】
- [NEW] src/views/lookup/classify/index.vue
- [NEW] src/views/lookup/item/index.vue
- [NEW] src/views/lookup/classify/components/ClassifyTable.vue
- [NEW] src/views/lookup/item/components/ItemDetailPanel.vue
- [NEW] src/views/lookup/item/components/ImportModal.vue（导入弹窗，含进度展示）
- [NEW] src/views/lookup/item/components/ExportModal.vue（导出弹窗，含进度展示）
- [NEW] src/views/lookup/item/components/TaskProgressModal.vue（任务进度展示组件）
- [NEW] src/api/lookup.js
- [NEW] src/stores/lookup.js

【数据库】
- [NEW] db/migration/V1.0.0__create_lookup_tables.sql（含任务表）
```

### 9.2 修改文件

```
- [MODIFY] pom.xml - 添加 Apache POI 依赖
- [MODIFY] application.yml - 添加 MyBatis-Plus 分页配置
- [MODIFY] src/router/index.js - 添加 LookUp 管理路由
- [MODIFY] src/layout/components/Sidebar/index.vue - 添加菜单项
```

---

## 10. 下一步行动

1. **数据库准备**: 执行 DDL 脚本创建表结构
2. **后端开发**: 按照接口设计文档开发 API 接口
3. **前端开发**: 参考 demo.html 实现 React 版本页面
4. **联调测试**: 前后端接口联调，功能测试
5. **性能优化**: 对导入功能和查询接口进行性能调优

---

## ✅ 技术规划完成

**Feature**: LookUp管理  
**状态**: planned  
**文件**: `.sddu/specs-tree-root/specs-tree-lookup/plan.md`

### 生成的 ADR
- `ADR-001.md` - 物理删除 vs 逻辑删除
- `ADR-002.md` - status 字段设计（0失效/1有效）
- `ADR-003.md` - Excel 导入方案选择


### 下一步
👉 运行 `@sddu-tasks LookUp管理` 开始任务分解
