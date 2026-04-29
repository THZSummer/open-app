# 数据迁移脚本使用指南

本目录包含开放平台数据迁移脚本，用于批量创建分类、API、事件和回调等资源。

## 📁 目录结构

```
migration/
├── config.sh              # 公共配置文件
├── 01-create-category.sh  # 分类创建脚本
├── 02-create-api.sh       # API 创建脚本
├── 03-create-event.sh     # 事件创建脚本
├── 04-create-callback.sh  # 回调创建脚本
└── README.md              # 本文档
```

## 🚀 快速开始

### 1. 配置环境

编辑 `config.sh` 文件，修改以下配置：

```bash
# 服务器地址
export BASE_URL="http://localhost:8080"
export API_PREFIX="/service/open/v2"

# 认证信息
export COOKIE_NAME="SESSIONID"
export COOKIE_VALUE="your-session-id-here"  # ⚠️ 修改为实际的 Session ID
```

### 2. 执行脚本

```bash
# 赋予执行权限
chmod +x *.sh

# 按顺序执行
./01-create-category.sh
./02-create-api.sh
./03-create-event.sh
./04-create-callback.sh
```

### 3. 查看结果

所有输出保存在 `migration-output/` 目录：

```bash
ls -lh migration-output/
```

## 📋 接口参数说明

### 1. 分类创建接口

**接口地址**: `POST /service/open/v2/categories`

**请求参数**:

| 参数名        | 类型   | 必填 | 说明                           |
|---------------|--------|------|--------------------------------|
| nameCn        | string | 是   | 中文名称                       |
| nameEn        | string | 是   | 英文名称                       |
| categoryAlias | string | 否   | 分类别名（建议使用英文）       |
| parentId      | number | 否   | 父分类ID（顶级分类不传）      |
| sortOrder     | number | 否   | 排序值（默认0，值小优先）      |

**请求示例**:
```json
{
  "nameCn": "用户管理",
  "nameEn": "User Management",
  "categoryAlias": "user_mgmt",
  "sortOrder": 1
}
```

---

### 2. API 创建接口

**接口地址**: `POST /service/open/v2/apis`

**请求参数**:

| 参数名          | 类型    | 必填 | 说明                                   |
|-----------------|---------|------|----------------------------------------|
| nameCn          | string  | 是   | 中文名称                               |
| nameEn          | string  | 是   | 英文名称                               |
| path            | string  | 是   | API路径（如 /user/info）              |
| method          | string  | 是   | HTTP方法（GET/POST/PUT/DELETE等）     |
| authType        | string  | 是   | 认证类型（SESSION/TOKEN/NONE）        |
| categoryId      | number  | 是   | 分类ID                                 |
| permission      | object  | 是   | 权限配置                               |
| ┝ nameCn        | string  | 是   | 权限中文名称                           |
| ┝ nameEn        | string  | 是   | 权限英文名称                           |
| ┝ scope         | string  | 是   | 权限范围（READ/WRITE/ALL）           |
| ┝ needApproval  | boolean | 是   | 是否需要审批                           |
| ┝ resourceNodes | array   | 是   | 资源节点列表                           |
| properties      | array   | 否   | 扩展属性列表                           |
| ┝ name          | string  | 是   | 属性名称                               |
| ┝ type          | string  | 是   | 属性类型（STRING/NUMBER/BOOL）        |
| ┝ value         | string  | 是   | 属性值                                 |
| ┝ description   | string  | 否   | 属性描述                               |

**请求示例**:
```json
{
  "nameCn": "获取用户信息",
  "nameEn": "Get User Info",
  "path": "/user/info",
  "method": "GET",
  "authType": "SESSION",
  "categoryId": 1,
  "permission": {
    "nameCn": "查询用户信息权限",
    "nameEn": "Query User Info Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["user:info:read"]
  },
  "properties": [
    {
      "name": "rateLimit",
      "type": "NUMBER",
      "value": "100",
      "description": "每分钟请求限制"
    }
  ]
}
```

---

### 3. 事件创建接口

**接口地址**: `POST /service/open/v2/events`

**请求参数**:

| 参数名          | 类型    | 必填 | 说明                                   |
|-----------------|---------|------|----------------------------------------|
| nameCn          | string  | 是   | 中文名称                               |
| nameEn          | string  | 是   | 英文名称                               |
| topic           | string  | 是   | 事件主题（如 order.created）          |
| categoryId      | number  | 是   | 分类ID                                 |
| permission      | object  | 是   | 权限配置                               |
| ┝ nameCn        | string  | 是   | 权限中文名称                           |
| ┝ nameEn        | string  | 是   | 权限英文名称                           |
| ┝ scope         | string  | 是   | 权限范围（READ/WRITE/ALL）           |
| ┝ needApproval  | boolean | 是   | 是否需要审批                           |
| ┝ resourceNodes | array   | 是   | 资源节点列表                           |
| properties      | array   | 否   | 扩展属性列表                           |

**请求示例**:
```json
{
  "nameCn": "订单创建事件",
  "nameEn": "Order Created Event",
  "topic": "order.created",
  "categoryId": 2,
  "permission": {
    "nameCn": "订阅订单创建事件权限",
    "nameEn": "Subscribe Order Created Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["event:order:created"]
  },
  "properties": [
    {
      "name": "maxRetry",
      "type": "NUMBER",
      "value": "3",
      "description": "最大重试次数"
    }
  ]
}
```

---

### 4. 回调创建接口

**接口地址**: `POST /service/open/v2/callbacks`

**请求参数**:

| 参数名          | 类型    | 必填 | 说明                                   |
|-----------------|---------|------|----------------------------------------|
| nameCn          | string  | 是   | 中文名称                               |
| nameEn          | string  | 是   | 英文名称                               |
| categoryId      | number  | 是   | 分类ID                                 |
| permission      | object  | 是   | 权限配置                               |
| ┝ nameCn        | string  | 是   | 权限中文名称                           |
| ┝ nameEn        | string  | 是   | 权限英文名称                           |
| ┝ scope         | string  | 是   | 权限范围（READ/WRITE/ALL）           |
| ┝ needApproval  | boolean | 是   | 是否需要审批                           |
| ┝ resourceNodes | array   | 是   | 资源节点列表                           |
| properties      | array   | 否   | 扩展属性列表                           |

**请求示例**:
```json
{
  "nameCn": "订单状态变更回调",
  "nameEn": "Order Status Change Callback",
  "categoryId": 2,
  "permission": {
    "nameCn": "接收订单状态变更权限",
    "nameEn": "Receive Order Status Change Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["callback:order:status"]
  },
  "properties": [
    {
      "name": "timeout",
      "type": "NUMBER",
      "value": "5000",
      "description": "回调超时时间（毫秒）"
    }
  ]
}
```

## 🔧 高级用法

### 自定义批量导入

可以修改脚本中的数组定义来自定义批量导入的数据：

```bash
# 示例：批量创建分类
CATEGORIES=(
  "订单管理|Order Management|order_mgmt|1"
  "商品管理|Product Management|product_mgmt|2"
  "支付管理|Payment Management|payment_mgmt|3"
)

# 格式：nameCn|nameEn|categoryAlias|sortOrder
```

### 使用单独的 curl 命令

如果需要单独执行某个请求，可以直接使用 curl：

```bash
# 设置环境变量
source config.sh

# 创建分类
curl -X POST "${BASE_URL}${API_PREFIX}/categories" \
  -H "Content-Type: application/json" \
  -H "Cookie: ${COOKIE_NAME}=${COOKIE_VALUE}" \
  -d '{
    "nameCn": "测试分类",
    "nameEn": "Test Category"
  }'
```

### 从文件导入数据

如果数据量较大，可以将数据保存在 JSON 文件中：

```bash
# 从文件读取并创建
curl -X POST "${BASE_URL}${API_PREFIX}/apis" \
  -H "Content-Type: application/json" \
  -H "Cookie: ${COOKIE_NAME}=${COOKIE_VALUE}" \
  -d @api_data.json
```

## 📊 响应格式

所有接口返回统一的 JSON 格式：

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "nameCn": "示例名称",
    "nameEn": "Example Name"
  }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数错误：nameCn 不能为空",
  "data": null
}
```

## ⚠️ 注意事项

1. **认证信息**: 确保在 `config.sh` 中配置了正确的 Session ID
2. **分类依赖**: 创建 API/事件/回调时，需要确保对应的分类已存在
3. **幂等性**: 重复执行脚本会创建重复数据，建议在测试环境先验证
4. **错误处理**: 脚本遇到错误会立即退出，检查输出日志定位问题
5. **输出文件**: 所有响应保存在 `migration-output/` 目录，便于事后审计

## 📝 常见问题

### Q1: 如何获取 Session ID？

A: 登录开放平台后，在浏览器开发者工具的 Cookie 中找到 `SESSIONID` 的值。

### Q2: 如何修改批量数据？

A: 编辑脚本中的数组定义，按照 `|` 分隔的格式添加或修改数据。

### Q3: 如何处理创建失败？

A: 检查 `migration-output/` 目录中的响应文件，查看错误信息。

### Q4: 能否跳过某些步骤？

A: 可以，脚本之间无强制依赖关系，可根据需要单独执行。

## 📞 技术支持

如有问题，请联系开放平台技术团队。