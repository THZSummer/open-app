#!/bin/bash
set -ex
# 接口39: PUT /api/v1/apps/:appId/callbacks/:id/config - 配置回调消费参数

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口39: 配置回调消费参数"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/apps/$APP_ID/callbacks/$SUBSCRIPTION_ID/config"
echo ""
echo "说明: 权限审批通过后，消费方需配置回调接收方式"
echo ""

echo ">>> 枚举值说明:"
echo "  通道类型(channelType): 0=WebHook, 1=SSE, 2=WebSocket"
echo ""

# 请求数据 - WebHook 方式
REQUEST_DATA='{
  "channelType": 0,
  "channelAddress": "https://webhook.example.com/callbacks",
  "authType": 0
}'

echo ">>> 测试WebHook方式:"
echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/apps/$APP_ID/callbacks/$SUBSCRIPTION_ID/config" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试SSE方式:"
SSE_REQUEST='{
  "channelType": 1,
  "authType": 1
}'

echo ">>> 请求数据:"
echo "$SSE_REQUEST" | jq '.' 2>/dev/null || echo "$SSE_REQUEST"
echo ""

curl -X PUT "$BASE_URL/api/v1/apps/$APP_ID/callbacks/$SUBSCRIPTION_ID/config" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$SSE_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试WebSocket方式:"
WS_REQUEST='{
  "channelType": 2,
  "authType": 0
}'

echo ">>> 请求数据:"
echo "$WS_REQUEST" | jq '.' 2>/dev/null || echo "$WS_REQUEST"
echo ""

curl -X PUT "$BASE_URL/api/v1/apps/$APP_ID/callbacks/$SUBSCRIPTION_ID/config" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$WS_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s