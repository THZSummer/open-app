#!/bin/bash
# 接口7: GET /api/v1/categories/:id/owners - 获取分类责任人列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口7: 获取分类责任人列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/categories/$CATEGORY_ID/owners"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X GET "$BASE_URL/api/v1/categories/$CATEGORY_ID/owners" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s