#!/bin/bash
# 接口56: POST /gateway/events/publish - 事件发布接口

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口56: 事件发布接口"
echo "=========================================="
echo ""
echo "说明: 业务模块通过此接口发布事件，网关分发至订阅的消费方"
echo "流程: 提供方 → 内部消息网关 → event-server → 消费方"
echo ""

# 请求数据
REQUEST_DATA='{
  "topic": "im.message.received",
  "payload": {
    "messageId": "msg001",
    "content": "Hello World",
    "sender": "user001"
  }
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
echo "请求路径: $BASE_URL/gateway/events/publish"
curl -X POST "$BASE_URL/gateway/events/publish" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试其他事件:"
OTHER_EVENT_DATA='{
  "topic": "user.registered",
  "payload": {
    "userId": "user003",
    "username": "newuser",
    "registerTime": "2026-04-20T10:00:00Z"
  }
}'

echo ">>> 请求数据:"
echo "$OTHER_EVENT_DATA" | jq '.' 2>/dev/null || echo "$OTHER_EVENT_DATA"
echo ""

curl -X POST "$BASE_URL/gateway/events/publish" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$OTHER_EVENT_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s