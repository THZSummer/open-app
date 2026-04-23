#!/bin/bash
# 接口38: POST /api/v1/apps/:appId/callbacks/subscribe - 申请回调权限

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口38: 申请回调权限（支持批量）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/apps/$APP_ID/callbacks/subscribe"
echo ""

# 请求数据
REQUEST_DATA='{
  "permissionIds": ["202", "203", "204"]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/apps/$APP_ID/callbacks/subscribe" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试单个权限申请:"
SINGLE_REQUEST='{
  "permissionIds": ["202"]
}'

echo ">>> 请求数据:"
echo "$SINGLE_REQUEST" | jq '.' 2>/dev/null || echo "$SINGLE_REQUEST"
echo ""

curl -X POST "$BASE_URL/api/v1/apps/$APP_ID/callbacks/subscribe" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$SINGLE_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s