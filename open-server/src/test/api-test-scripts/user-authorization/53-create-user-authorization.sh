#!/bin/bash
# 接口53: POST /api/v1/user-authorizations - 用户授权

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口53: 用户授权（设置有效期）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/user-authorizations"
echo ""

# 请求数据 - 带过期时间
REQUEST_DATA='{
  "userId": "user001",
  "appId": "'"$APP_ID"'",
  "scopes": ["api:im:send-message", "api:im:get-message"],
  "expiresAt": "2026-12-31T23:59:59"
}'

echo ">>> 测试带过期时间授权:"
echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/user-authorizations" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试永久授权（不设置过期时间）:"
PERMANENT_REQUEST='{
  "userId": "user002",
  "appId": "'"$APP_ID"'",
  "scopes": ["api:im:send-message"]
}'

echo ">>> 请求数据:"
echo "$PERMANENT_REQUEST" | jq '.' 2>/dev/null || echo "$PERMANENT_REQUEST"
echo ""

curl -X POST "$BASE_URL/api/v1/user-authorizations" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$PERMANENT_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s