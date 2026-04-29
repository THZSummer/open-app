#!/bin/bash
set -ex

# ==================== 创建 API ====================
# 接口：POST /service/open/v2/apis

# 修改以下配置
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - path (必填): API路径
# - method (必填): HTTP方法（GET/POST/PUT/DELETE）
# - authType (必填): 认证类型（0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN）
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 api:{模块}:{资源}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表
# - properties.propertyName (可选): 属性名称
# - properties.propertyValue (可选): 属性值

curl -X POST "${BASE_URL}/service/open/v2/apis" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=${SESSION_ID}" \
  -d '{
    "nameCn": "获取用户信息",
    "nameEn": "Get User Info",
    "path": "/api/v1/user/info",
    "method": "GET",
    "authType": 1,
    "categoryId": 1,
    "permission": {
      "nameCn": "查询用户信息权限",
      "nameEn": "Query User Info Permission",
      "scope": "api:user:query-info",
      "needApproval": 1,
      "resourceNodes": null
    },
    "properties": [
      {
        "propertyName": "doc_url",
        "propertyValue": "https://doc.example.com/api/user-info"
      },
      {
        "propertyName": "rate_limit",
        "propertyValue": "100/min"
      }
    ]
  }' | jq .
