#!/bin/bash
# 接口25: DELETE /api/v1/callbacks/:id - 删除回调

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口25: 删除回调"
echo "=========================================="
echo ""
echo "请求方法: DELETE"
echo "请求路径: $BASE_URL/api/v1/callbacks/$CALLBACK_ID"
echo ""
echo "说明: 已订阅的回调无法删除，需先取消所有订阅"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X DELETE "$BASE_URL/api/v1/callbacks/$CALLBACK_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s