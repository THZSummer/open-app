#!/bin/bash
set -ex
# 接口18: PUT /api/v1/events/:id - 更新事件

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../config.sh"

echo "=========================================="
echo "接口18: 更新事件及权限信息"
echo "=========================================="
echo ""
echo "请求方法: PUT"
echo "请求路径: $BASE_URL/api/v1/events/$EVENT_ID"
echo ""
echo "说明: 核心属性（topic、scope）变更需重新审批"
echo ""

# 请求数据
REQUEST_DATA='{
  "nameCn": "消息接收事件V2",
  "categoryId": "'"$CATEGORY_ID"'",
  "permission": {
    "nameCn": "消息接收权限V2"
  },
  "properties": [
    { "propertyName": "descriptionCn", "propertyValue": "消息接收事件描述（更新）" }
  ]
}'

echo ">>> 请求数据:"
echo "$REQUEST_DATA" | jq '.' 2>/dev/null || echo "$REQUEST_DATA"
echo ""

# 执行请求
echo ">>> 发送请求:"
curl -X PUT "$BASE_URL/api/v1/events/$EVENT_ID" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "$REQUEST_DATA" \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -s