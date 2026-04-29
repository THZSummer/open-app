# 数据迁移脚本

开放平台数据迁移脚本，用于创建分类、API、事件和回调。

## 快速使用

### 1. 修改参数

编辑对应脚本文件，修改顶部的参数配置：

```bash
BASE_URL="http://localhost:8080"       # 服务器地址
SESSION_ID="your-session-id-here"      # 登录后的 Session ID
```

### 2. 执行脚本

```bash
# 赋予执行权限
chmod +x *.sh

# 按顺序执行
./01-create-category.sh   # 创建分类
./02-create-api.sh        # 创建 API
./03-create-event.sh      # 创建事件
./04-create-callback.sh   # 创建回调
```

## 脚本说明

| 脚本 | 接口 | 说明 |
|------|------|------|
| 01-create-category.sh | POST /service/open/v2/categories | 创建分类 |
| 02-create-api.sh | POST /service/open/v2/apis | 创建 API |
| 03-create-event.sh | POST /service/open/v2/events | 创建事件 |
| 04-create-callback.sh | POST /service/open/v2/callbacks | 创建回调 |

## 参数说明

### 通用参数

- `BASE_URL`: 服务器地址（默认：http://localhost:8080）
- `SESSION_ID`: 登录后的 Session ID（从浏览器 Cookie 获取）

### 01-create-category.sh

| 参数 | 必填 | 说明 |
|------|------|------|
| NAME_CN | 是 | 中文名称 |
| NAME_EN | 是 | 英文名称 |
| CATEGORY_ALIAS | 否 | 分类别名 |
| PARENT_ID | 否 | 父分类ID，顶级分类为 null |
| SORT_ORDER | 否 | 排序值，默认0，值小优先 |

### 02-create-api.sh

| 参数 | 必填 | 说明 |
|------|------|------|
| NAME_CN | 是 | 中文名称 |
| NAME_EN | 是 | 英文名称 |
| PATH | 是 | API路径（如 /user/info） |
| METHOD | 是 | HTTP方法（GET/POST/PUT/DELETE等） |
| AUTH_TYPE | 是 | 认证类型（SESSION/TOKEN/NONE） |
| CATEGORY_ID | 是 | 分类ID |
| PERMISSION_NAME_CN | 是 | 权限中文名称 |
| PERMISSION_NAME_EN | 是 | 权限英文名称 |
| PERMISSION_SCOPE | 是 | 权限范围（READ/WRITE/ALL） |
| NEED_APPROVAL | 是 | 是否需要审批（true/false） |
| RESOURCE_NODES | 是 | 资源节点列表（JSON数组字符串） |
| PROPERTIES | 否 | 扩展属性 |

### 03-create-event.sh

| 参数 | 必填 | 说明 |
|------|------|------|
| NAME_CN | 是 | 中文名称 |
| NAME_EN | 是 | 英文名称 |
| TOPIC | 是 | 事件主题（如 order.created） |
| CATEGORY_ID | 是 | 分类ID |
| PERMISSION_NAME_CN | 是 | 权限中文名称 |
| PERMISSION_NAME_EN | 是 | 权限英文名称 |
| PERMISSION_SCOPE | 是 | 权限范围（READ/WRITE/ALL） |
| NEED_APPROVAL | 是 | 是否需要审批（true/false） |
| RESOURCE_NODES | 是 | 资源节点列表（JSON数组字符串） |
| PROPERTIES | 否 | 扩展属性 |

### 04-create-callback.sh

| 参数 | 必填 | 说明 |
|------|------|------|
| NAME_CN | 是 | 中文名称 |
| NAME_EN | 是 | 英文名称 |
| CATEGORY_ID | 是 | 分类ID |
| PERMISSION_NAME_CN | 是 | 权限中文名称 |
| PERMISSION_NAME_EN | 是 | 权限英文名称 |
| PERMISSION_SCOPE | 是 | 权限范围（READ/WRITE/ALL） |
| NEED_APPROVAL | 是 | 是否需要审批（true/false） |
| RESOURCE_NODES | 是 | 资源节点列表（JSON数组字符串） |
| PROPERTIES | 否 | 扩展属性 |

## 扩展属性格式

`PROPERTIES` 参数格式：

```bash
# 格式：name|type|value|description
# 多个属性用逗号分隔
PROPERTIES="rateLimit|NUMBER|100|每分钟请求限制,timeout|NUMBER|5000|超时时间(毫秒)"
```

属性类型：
- `STRING`: 字符串
- `NUMBER`: 数字
- `BOOL`: 布尔值（true/false）

## 注意事项

1. **执行顺序**: 先创建分类，再创建 API/事件/回调
2. **Session ID**: 从浏览器开发者工具的 Cookie 中获取 `SESSIONID`
3. **分类ID**: 创建 API/事件/回调时，需要确保对应的分类已存在
4. **幂等性**: 重复执行会创建重复数据

## 响应格式

所有接口返回统一的 JSON 格式：

**成功响应**:
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

**错误响应**:
```json
{
  "code": 400,
  "message": "参数错误：nameCn 不能为空",
  "data": null
}
```
