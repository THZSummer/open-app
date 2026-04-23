#!/bin/bash
# 接口45: GET /api/v1/approvals/pending - 获取待审批列表

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口45: 获取待审批列表"
echo "=========================================="
echo ""
echo "请求方法: GET"
echo "请求路径: $BASE_URL/api/v1/approvals/pending"
echo ""
echo "说明: 返回当前用户待处理的审批单"
echo ""

# 执行请求
echo ">>> 发送请求（无参数）:"
curl -X GET "$BASE_URL/api/v1/approvals/pending" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带类型过滤:"
curl -X GET "$BASE_URL/api/v1/approvals/pending?type=resource_register&curPage=1&pageSize=20" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试权限申请类型:"
curl -X GET "$BASE_URL/api/v1/approvals/pending?type=permission_apply" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带关键词搜索:"
curl -X GET "$BASE_URL/api/v1/approvals/pending?keyword=发送消息" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s