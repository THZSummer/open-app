#!/bin/bash
# 接口5: DELETE /api/v1/categories/:id - 删除分类

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口5: 删除分类"
echo "=========================================="
echo ""
echo "请求方法: DELETE"
echo "请求路径: $BASE_URL/api/v1/categories/$CATEGORY_ID"
echo ""
echo "说明: 删除前检查是否存在关联资源，若存在则返回错误"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X DELETE "$BASE_URL/api/v1/categories/$CATEGORY_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s