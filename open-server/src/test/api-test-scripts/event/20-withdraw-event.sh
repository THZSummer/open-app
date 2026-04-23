#!/bin/bash
# 接口20: POST /api/v1/events/:id/withdraw - 撤回审核中的事件

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口20: 撤回审核中的事件"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/events/$EVENT_ID/withdraw"
echo ""
echo "说明: 仅状态为'待审'的事件可撤回"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/events/$EVENT_ID/withdraw" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s