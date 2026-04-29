#!/bin/bash
set -ex

# ==================== 创建事件 ====================
# 接口：POST /service/open/v2/events

# 修改以下配置
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - topic (必填): 事件主题，格式 {模块}.{事件}
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 event:{模块}:{事件}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表

curl -X POST "${BASE_URL}/service/open/v2/events" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=${SESSION_ID}" \
  -d '{
    "nameCn": "订单创建事件",
    "nameEn": "Order Created Event",
    "topic": "order.created",
    "categoryId": 2,
    "permission": {
      "nameCn": "订阅订单创建事件权限",
      "nameEn": "Subscribe Order Created Permission",
      "scope": "event:order:created",
      "needApproval": 1,
      "resourceNodes": null
    },
    "properties": [
      {
        "propertyName": "doc_url",
        "propertyValue": "https://doc.example.com/event/message-received"
      },
      {
        "propertyName": "retry_policy",
        "propertyValue": "3"
      }
    ]
  }' | jq .
