#!/bin/bash
set -ex
# 接口11: POST /api/v1/apis - 注册API

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口11: 注册API（附带权限定义）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/apis"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "发送消息",
  "nameEn": "Send Message",
  "path": "/api/v1/messages",
  "method": "POST",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "发送消息权限",
    "nameEn": "Send Message Permission",
    "scope": "api:im:send-message"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述" },
    { "propertyName": "descriptionEn", "propertyValue": "Send message API description" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message" }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/apis" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带审批流程ID:"
REQUEST_DATA_WITH_FLOW='{
  "nameCn": "发送消息V2",
  "nameEn": "Send Message V2",
  "path": "/api/v1/messages/v2",
  "method": "POST",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "发送消息权限V2",
    "nameEn": "Send Message Permission V2",
    "scope": "api:im:send-message-v2",
    "approvalFlowId": "'"$FLOW_ID"'"
  }
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA_WITH_FLOW" | jq '.' 2>/dev/null || echo "$REQUEST_DATA_WITH_FLOW"
echo ""

curl -X POST "$BASE_URL/api/v1/apis" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA_WITH_FLOW" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s