#!/bin/bash
# 接口14: POST /api/v1/apis/:id/withdraw - 撤回审核中的API

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口14: 撤回审核中的API"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/apis/$API_ID/withdraw"
echo ""
echo "说明: 仅状态为'待审'的API可撤回"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/apis/$API_ID/withdraw" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s