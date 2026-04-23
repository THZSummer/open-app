#!/bin/bash
# 接口43: POST /api/v1/approval-flows - 创建审批流程模板

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口43: 创建审批流程模板"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/approval-flows"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "API注册审批流",
  "nameEn": "API Registration Approval Flow",
  "code": "api_register",
  "isDefault": 0,
  "nodes": [
    { "type": "approver", "userId": "user001", "order": 1 },
    { "type": "approver", "userId": "user002", "order": 2 }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/approval-flows" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试创建默认流程:"
DEFAULT_REQUEST='{
  "nameCn": "默认审批流",
  "nameEn": "Default Approval Flow",
  "code": "default",
  "isDefault": 1,
  "nodes": [
    { "type": "approver", "userId": "admin", "order": 1 }
  ]
}'

echo ">>> 请求数据:"
echo "$DEFAULT_REQUEST" | jq '.' 2>/dev/null || echo "$DEFAULT_REQUEST"
echo ""

curl -X POST "$BASE_URL/api/v1/approval-flows" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$DEFAULT_REQUEST" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s