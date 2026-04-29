#!/bin/bash
set -ex
# 接口58: GET /gateway/permissions/check - 权限校验接口

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口58: 权限校验接口（供网关内部调用）"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/gateway/permissions/check"
echo ""

# 执行请求
echo ">>> 测试权限校验（已授权）:"
curl -X GET "$BASE_URL/gateway/permissions/check?appId=$APP_ID&scope=api:im:send-message" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试权限校验（未授权）:"
curl -X GET "$BASE_URL/gateway/permissions/check?appId=$APP_ID&scope=api:admin:delete-user" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试事件权限校验:"
curl -X GET "$BASE_URL/gateway/permissions/check?appId=$APP_ID&scope=event:im:message-received" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试回调权限校验:"
curl -X GET "$BASE_URL/gateway/permissions/check?appId=$APP_ID&scope=callback:approval:completed" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s