#!/bin/bash
set -ex
# 接口57: POST /gateway/callbacks/invoke - 回调触发接口

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口57: 回调触发接口"
echo "=========================================="
echo ""
echo "说明: 业务模块通过此接口触发回调，网关调用已订阅的消费方回调地址"
echo "流程: 提供方 → event-server → 消费方（不经内部消息网关）"
echo ""

# 请求数据
REQUEST_DATA='{
  "callbackScope": "callback:approval:completed",
  "payload": {
    "approvalId": "app001",
    "status": "approved",
    "approver": "user001"
  }
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
echo "请求路径: $BASE_URL/gateway/callbacks/invoke"
curl -X POST "$BASE_URL/gateway/callbacks/invoke" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试其他回调:"
OTHER_CALLBACK_DATA='{
  "callbackScope": "callback:user:registered",
  "payload": {
    "userId": "user003",
    "username": "newuser",
    "action": "welcome_email_sent"
  }
}'

echo ">>> 请求数据:"
echo "$OTHER_CALLBACK_DATA" | jq '.' 2>/dev/null || echo "$OTHER_CALLBACK_DATA"
echo ""

curl -X POST "$BASE_URL/gateway/callbacks/invoke" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$OTHER_CALLBACK_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s