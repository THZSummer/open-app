#!/bin/bash
set -ex
# 接口36: GET /api/v1/apps/:appId/callbacks - 获取应用回调订阅列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口36: 获取应用回调订阅列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/apps/$APP_ID/callbacks"
echo ""

# 执行请求
echo ">>> 发送请求（无参数）:"
curl -X GET "$BASE_URL/api/v1/apps/$APP_ID/callbacks" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带状态过滤:"
curl -X GET "$BASE_URL/api/v1/apps/$APP_ID/callbacks?status=1&curPage=1&pageSize=20" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带关键词搜索:"
curl -X GET "$BASE_URL/api/v1/apps/$APP_ID/callbacks?keyword=审批" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s