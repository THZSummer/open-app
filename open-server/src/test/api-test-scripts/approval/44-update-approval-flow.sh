#!/bin/bash
# 接口44: PUT /api/v1/approval-flows/:id - 更新审批流程模板

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口44: 更新审批流程模板"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/approval-flows/$FLOW_ID"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "API注册审批流V2",
  "nodes": [
    { "type": "approver", "userId": "user003", "order": 1 }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/approval-flows/$FLOW_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s