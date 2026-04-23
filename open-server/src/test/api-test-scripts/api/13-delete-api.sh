#!/bin/bash
# 接口13: DELETE /api/v1/apis/:id - 删除API

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口13: 删除API"
echo "=========================================="
echo ""
echo "请求方法: DELETE"
echo "请求路径: $BASE_URL/api/v1/apis/$API_ID"
echo ""
echo "说明: 已订阅的API无法删除，需先取消所有订阅"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X DELETE "$BASE_URL/api/v1/apis/$API_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s