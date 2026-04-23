#!/bin/bash
# 接口49: POST /api/v1/approvals/:id/cancel - 撤销审批

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口49: 撤销审批"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/approvals/$APPROVAL_ID/cancel"
echo ""
echo "说明: 仅申请人可撤销，且审批未完成时可撤销"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/approvals/$APPROVAL_ID/cancel" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s