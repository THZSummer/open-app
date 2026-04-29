#!/bin/bash
set -e

# ==================== 创建事件 ====================
# 接口：POST /service/open/v2/events

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
declare -a NAMES=("订单创建事件" "用户登录事件" "消息接收事件")

DATA_1='{
  "nameCn": "订单创建事件",
  "nameEn": "Order Created Event",
  "topic": "order.created",
  "categoryId": 2,
  "permission": {
    "nameCn": "订阅订单创建事件权限",
    "nameEn": "Subscribe Order Created Permission",
    "scope": "event:order:created",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/event/order-created"
    },
    {
      "propertyName": "retry_policy",
      "propertyValue": "3"
    }
  ]
}'

DATA_2='{
  "nameCn": "用户登录事件",
  "nameEn": "User Login Event",
  "topic": "user.login",
  "categoryId": 1,
  "permission": {
    "nameCn": "订阅用户登录事件权限",
    "nameEn": "Subscribe User Login Permission",
    "scope": "event:user:login",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/event/user-login"
    },
    {
      "propertyName": "retry_policy",
      "propertyValue": "5"
    }
  ]
}'

DATA_3='{
  "nameCn": "消息接收事件",
  "nameEn": "Message Received Event",
  "topic": "message.received",
  "categoryId": 2,
  "permission": {
    "nameCn": "订阅消息接收事件权限",
    "nameEn": "Subscribe Message Received Permission",
    "scope": "event:message:received",
    "needApproval": 0,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/event/message-received"
    },
    {
      "propertyName": "retry_policy",
      "propertyValue": "3"
    }
  ]
}'

declare -a DATA=("$DATA_1" "$DATA_2" "$DATA_3")

# ==================== 批量执行 ====================
echo "开始创建事件，共 ${#DATA[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0
RESPONSE_FILE="/tmp/event_response.json"

for i in "${!DATA[@]}"; do
  echo ""
  echo "[$((i+1))/${#DATA[@]}] 创建事件: ${NAMES[$i]}"
  echo "--------------------------------------"
  
  http_code=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" -X POST \
    "${BASE_URL}/service/open/v2/events" \
    -H "Content-Type: application/json" \
    -H "Cookie: SESSIONID=${SESSION_ID}" \
    -d "${DATA[$i]}")
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo "✅ 创建成功"
    cat "$RESPONSE_FILE"
    ((SUCCESS++))
  else
    echo "❌ 创建失败 (HTTP $http_code)"
    cat "$RESPONSE_FILE"
    ((FAILED++))
  fi
done

echo ""
echo "======================================"
echo "执行完成: 成功 $SUCCESS 条，失败 $FAILED 条"
