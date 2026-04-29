#!/bin/bash
set -ex
# 接口10: GET /api/v1/apis/:id - 获取API详情

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口10: 获取API详情（含权限信息及属性）"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/apis/$API_ID"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X GET "$BASE_URL/api/v1/apis/$API_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s