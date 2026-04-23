#!/bin/bash
# 接口47: POST /api/v1/approvals/:id/approve - 同意审批

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口47: 同意审批"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/approvals/$APPROVAL_ID/approve"
echo ""

# 请求数据
REQUEST_DATA='{
  "comment": "API 设计合理，同意上架"
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/approvals/$APPROVAL_ID/approve" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试无意见同意:"
NO_COMMENT_REQUEST='{}'

echo ">>> 请求数据:"
echo "$NO_COMMENT_REQUEST" | jq '.' 2>/dev/null || echo "$NO_COMMENT_REQUEST"
echo ""

curl -X POST "$BASE_URL/api/v1/approvals/$APPROVAL_ID/approve" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$NO_COMMENT_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s