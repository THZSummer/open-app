#!/bin/bash
set -ex
# 接口50: POST /api/v1/approvals/batch-approve - 批量同意审批

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口50: 批量同意审批"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/approvals/batch-approve"
echo ""

# 请求数据
REQUEST_DATA='{
  "approvalIds": ["500", "501", "502"],
  "comment": "批量审批通过"
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/approvals/batch-approve" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试无意见批量同意:"
NO_COMMENT_REQUEST='{
  "approvalIds": ["503", "504"]
}'

echo ">>> 请求数据:"
echo "$NO_COMMENT_REQUEST" | jq '.' 2>/dev/null || echo "$NO_COMMENT_REQUEST"
echo ""

curl -X POST "$BASE_URL/api/v1/approvals/batch-approve" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$NO_COMMENT_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s