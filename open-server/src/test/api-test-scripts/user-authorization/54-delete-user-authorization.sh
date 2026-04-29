#!/bin/bash
set -ex
# 接口54: DELETE /api/v1/user-authorizations/:id - 取消授权

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口54: 取消授权"
echo "=========================================="
echo ""
echo "请求方法: DELETE"
echo "请求路径: $BASE_URL/api/v1/user-authorizations/$USER_AUTH_ID"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X DELETE "$BASE_URL/api/v1/user-authorizations/$USER_AUTH_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s