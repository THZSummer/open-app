#!/bin/bash
# 接口28: GET /api/v1/categories/:id/apis - 获取分类下API权限列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口28: 获取分类下API权限列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/categories/$CATEGORY_ID/apis"
echo ""
echo "说明: 权限树懒加载模式，点击分类节点时调用"
echo "      - include_children=true（默认）：递归获取当前分类及所有子分类下的权限"
echo "      - include_children=false：仅获取当前分类直接关联的权限"
echo ""

# 执行请求
echo ">>> 发送请求（包含子分类）:"
curl -X GET "$BASE_URL/api/v1/categories/$CATEGORY_ID/apis?includeChildren=true&curPage=1&pageSize=20" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试仅当前分类:"
curl -X GET "$BASE_URL/api/v1/categories/$CATEGORY_ID/apis?includeChildren=false" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带是否需要审核过滤:"
curl -X GET "$BASE_URL/api/v1/categories/$CATEGORY_ID/apis?needApproval=1" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s