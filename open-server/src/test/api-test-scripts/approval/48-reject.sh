#!/bin/bash
# 接口48: POST /api/v1/approvals/:id/reject - 驳回审批

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口48: 驳回审批（需填写原因）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/approvals/$APPROVAL_ID/reject"
echo ""

# 请求数据
REQUEST_DATA='{
  "reason": "API 文档缺失，请补充后重新提交"
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/approvals/$APPROVAL_ID/reject" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s