#!/bin/bash
# 接口34: PUT /api/v1/apps/:appId/events/:id/config - 配置事件消费参数

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口34: 配置事件消费参数"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/apps/$APP_ID/events/$SUBSCRIPTION_ID/config"
echo ""
echo "说明: 权限审批通过后，消费方需配置事件接收方式"
echo ""

echo ">>> 枚举值说明:"
echo "  通道类型(channelType): 0=企业内部消息队列, 1=WebHook"
echo "  认证类型(authType): 0=应用类凭证A, 1=应用类凭证B"
echo ""

# 请求数据 - WebHook 方式
REQUEST_DATA='{
  "channelType": 1,
  "channelAddress": "https://webhook.example.com/events",
  "authType": 0
}'

echo ">>> 测试WebHook方式:"
echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/apps/$APP_ID/events/$SUBSCRIPTION_ID/config" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试企业内部消息队列方式:"
QUEUE_REQUEST='{
  "channelType": 0,
  "authType": 1
}'

echo ">>> 请求数据:"
echo "$QUEUE_REQUEST" | jq '.' 2>/dev/null || echo "$QUEUE_REQUEST"
echo ""

curl -X PUT "$BASE_URL/api/v1/apps/$APP_ID/events/$SUBSCRIPTION_ID/config" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$QUEUE_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s