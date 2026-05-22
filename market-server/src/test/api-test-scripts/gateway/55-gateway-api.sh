#!/bin/bash
set -ex
# 接口55: ANY /gateway/api/* - API请求代理与鉴权

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口55: API请求代理与鉴权"
echo "=========================================="
echo ""
echo "说明: 三方应用通过此网关调用内部 API"
echo "流程:"
echo "  1. 验证应用身份（AKSK/Bearer Token）"
echo "  2. 查询应用订阅关系"
echo "  3. 验证请求路径与方法是否在授权 Scope 范围内"
echo "  4. 转发请求到内部中台网关"
echo "  5. 返回响应"
echo ""

# 测试 GET 请求
echo ">>> 测试 GET 请求:"
echo "请求路径: $BASE_URL/gateway/api/v1/messages"
curl -X GET "$BASE_URL/gateway/api/v1/messages" \
  -H "Content-Type: application/json" \
  -H "$X_APP_ID" \
  -H "$X_AUTH_TYPE" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试 POST 请求:"
POST_DATA='{
  "content": "Hello World",
  "receiver": "user001"
}'

echo "请求路径: $BASE_URL/gateway/api/v1/messages"
echo ">>> 请求数据:"
echo "$POST_DATA" | jq '.' 2>/dev/null || echo "$POST_DATA"
echo ""

curl -X POST "$BASE_URL/gateway/api/v1/messages" \
  -H "Content-Type: application/json" \
  -H "$X_APP_ID" \
  -H "$X_AUTH_TYPE" \
  -H "$AUTH_HEADER" \
  -d "$POST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试 PUT 请求:"
PUT_DATA='{
  "content": "Updated message"
}'

echo "请求路径: $BASE_URL/gateway/api/v1/messages/msg001"
echo ">>> 请求数据:"
echo "$PUT_DATA" | jq '.' 2>/dev/null || echo "$PUT_DATA"
echo ""

curl -X PUT "$BASE_URL/gateway/api/v1/messages/msg001" \
  -H "Content-Type: application/json" \
  -H "$X_APP_ID" \
  -H "$X_AUTH_TYPE" \
  -H "$AUTH_HEADER" \
  -d "$PUT_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试 DELETE 请求:"
echo "请求路径: $BASE_URL/gateway/api/v1/messages/msg001"
curl -X DELETE "$BASE_URL/gateway/api/v1/messages/msg001" \
  -H "Content-Type: application/json" \
  -H "$X_APP_ID" \
  -H "$X_AUTH_TYPE" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s