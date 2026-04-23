#!/bin/bash
# 接口1: GET /api/v1/categories - 获取分类列表（树形结构）

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口1: 获取分类列表（树形结构）"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/categories"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X GET "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带过滤参数:"
curl -X GET "$BASE_URL/api/v1/categories?categoryAlias=app_type_a" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s