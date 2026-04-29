#!/bin/bash
set -ex

# ==================== 创建回调 ====================
# 接口：POST /service/open/v2/callbacks

# 修改以下配置
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 callback:{模块}:{资源}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表

curl -X POST "${BASE_URL}/service/open/v2/callbacks" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=${SESSION_ID}" \
  -d '{
    "nameCn": "订单状态变更回调",
    "nameEn": "Order Status Change Callback",
    "categoryId": 2,
    "permission": {
      "nameCn": "接收订单状态变更权限",
      "nameEn": "Receive Order Status Change Permission",
      "scope": "callback:order:status",
      "needApproval": 1,
      "resourceNodes": null
    },
    "properties": [
      {
        "propertyName": "doc_url",
        "propertyValue": "https://doc.example.com/callback/user-login"
      },
      {
        "propertyName": "timeout",
        "propertyValue": "5000"
      }
    ]
  }' | jq .
