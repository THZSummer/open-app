#!/bin/bash
set -ex
# 接口12: PUT /api/v1/apis/:id - 更新API

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口12: 更新API及权限信息"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/apis/$API_ID"
echo ""
echo "说明: 核心属性（path、method、scope）变更需重新审批"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "发送消息V2",
  "nameEn": "Send Message V2",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "发送消息权限V2",
    "nameEn": "Send Message Permission V2"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "发送消息API的中文描述（更新）" },
    { "propertyName": "docUrl", "propertyValue": "https://docs.example.com/api/send-message-v2" }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/apis/$API_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s