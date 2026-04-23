#!/bin/bash
# 接口26: POST /api/v1/callbacks/:id/withdraw - 撤回审核中的回调

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口26: 撤回审核中的回调"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/callbacks/$CALLBACK_ID/withdraw"
echo ""
echo "说明: 仅状态为'待审'的回调可撤回"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/callbacks/$CALLBACK_ID/withdraw" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s