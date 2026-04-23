#!/bin/bash
# 接口15: GET /api/v1/events - 获取事件列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口15: 获取事件列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/events"
echo ""

# 执行请求
echo ">>> 发送请求（无参数）:"
curl -X GET "$BASE_URL/api/v1/events" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带分类过滤:"
curl -X GET "$BASE_URL/api/v1/events?categoryId=$CATEGORY_ID&curPage=1&pageSize=20" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带状态过滤:"
curl -X GET "$BASE_URL/api/v1/events?status=2" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带关键词搜索:"
curl -X GET "$BASE_URL/api/v1/events?keyword=消息" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s