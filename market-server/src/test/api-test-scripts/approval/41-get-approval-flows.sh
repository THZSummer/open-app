#!/bin/bash
set -ex
# 接口41: GET /api/v1/approval-flows - 获取审批流程模板列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口41: 获取审批流程模板列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/approval-flows"
echo ""

# 执行请求
echo ">>> 发送请求（无参数）:"
curl -X GET "$BASE_URL/api/v1/approval-flows" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带分页:"
curl -X GET "$BASE_URL/api/v1/approval-flows?curPage=1&pageSize=20" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带关键词搜索:"
curl -X GET "$BASE_URL/api/v1/approval-flows?keyword=默认" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s