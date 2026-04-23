#!/bin/bash
# 接口23: POST /api/v1/callbacks - 注册回调

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口23: 注册回调（附带权限定义）"
echo "=========================================="
echo ""
echo "请求方法: POST"
echo "请求路径: $BASE_URL/api/v1/callbacks"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "审批完成回调",
  "nameEn": "Approval Completed Callback",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "审批完成回调权限",
    "nameEn": "Approval Completed Callback Permission",
    "scope": "callback:approval:completed"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "审批完成后的回调通知" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/callback/approval-completed" }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X POST "$BASE_URL/api/v1/callbacks" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ">>> 测试带审批流程ID:"
REQUEST_DATA_WITH_FLOW='{
  "nameCn": "用户注册回调",
  "nameEn": "User Registered Callback",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "用户注册回调权限",
    "nameEn": "User Registered Callback Permission",
    "scope": "callback:user:registered",
    "approvalFlowId": "'"$FLOW_ID"'"
  }
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA_WITH_FLOW" | jq '.' 2>/dev/null || echo "$REQUEST_DATA_WITH_FLOW"
echo ""

curl -X POST "$BASE_URL/api/v1/callbacks" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA_WITH_FLOW" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s