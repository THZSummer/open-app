#!/bin/bash
set -ex
# 接口17: POST /api/v1/events - 注册事件

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口17: 注册事件（附带权限定义）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/events"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "消息接收事件",
  "nameEn": "Message Received Event",
  "topic": "im.message.received",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "消息接收权限",
    "nameEn": "Message Received Permission",
    "scope": "event:im:message-received"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "消息接收事件描述" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/event/message-received" }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/events" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带审批流程ID:"
REQUEST_DATA_WITH_FLOW='{
  "nameCn": "消息发送事件",
  "nameEn": "Message Sent Event",
  "topic": "im.message.sent",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "消息发送权限",
    "nameEn": "Message Sent Permission",
    "scope": "event:im:message-sent",
    "approvalFlowId": "'"$FLOW_ID"'"
  }
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA_WITH_FLOW" | jq '.' 2>/dev/null || echo "$REQUEST_DATA_WITH_FLOW"
echo ""

curl -X POST "$BASE_URL/api/v1/events" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA_WITH_FLOW" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s