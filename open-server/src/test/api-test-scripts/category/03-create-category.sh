#!/bin/bash
set -ex
# 接口3: POST /api/v1/categories - 创建分类

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口3: 创建分类"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/categories"
echo ""

# 请求数据
REQUEST_DATA='{
  "categoryAlias": "app_type_a",
  "nameCn": "A类应用权限",
  "nameEn": "App Type A Permissions",
  "parentId": null,
  "sortOrder": 0
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试创建子分类:"
SUB_REQUEST_DATA='{
  "categoryAlias": null,
  "nameCn": "IM业务",
  "nameEn": "IM Business",
  "parentId": "1",
  "sortOrder": 0
}'

echo ">>> 请求数据:"
echo "$SUB_REQUEST_DATA" | jq '.' 2>/dev/null || echo "$SUB_REQUEST_DATA"
echo ""

curl -X POST "$BASE_URL/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$SUB_REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s