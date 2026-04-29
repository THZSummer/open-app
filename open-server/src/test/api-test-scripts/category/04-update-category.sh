#!/bin/bash
set -ex
# 接口4: PUT /api/v1/categories/:id - 更新分类

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口4: 更新分类"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/categories/$CATEGORY_ID"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "IM业务能力",
  "nameEn": "IM Business Capability",
  "sortOrder": 1
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/categories/$CATEGORY_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s