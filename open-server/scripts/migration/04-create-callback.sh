#!/bin/bash
set -e

# ==================== 创建回调 ====================
# 接口：POST /service/open/v2/callbacks

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
declare -a NAMES=("订单状态变更回调" "用户登录通知回调" "消息推送回调")

DATA_1='{
  "nameCn": "订单状态变更回调",
  "nameEn": "Order Status Change Callback",
  "categoryId": 2,
  "permission": {
    "nameCn": "接收订单状态变更权限",
    "nameEn": "Receive Order Status Change Permission",
    "scope": "callback:order:status",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/callback/order-status"
    },
    {
      "propertyName": "timeout",
      "propertyValue": "5000"
    }
  ]
}'

DATA_2='{
  "nameCn": "用户登录通知回调",
  "nameEn": "User Login Notification Callback",
  "categoryId": 1,
  "permission": {
    "nameCn": "接收用户登录通知权限",
    "nameEn": "Receive User Login Notification Permission",
    "scope": "callback:user:login",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/callback/user-login"
    },
    {
      "propertyName": "timeout",
      "propertyValue": "3000"
    }
  ]
}'

DATA_3='{
  "nameCn": "消息推送回调",
  "nameEn": "Message Push Callback",
  "categoryId": 2,
  "permission": {
    "nameCn": "接收消息推送权限",
    "nameEn": "Receive Message Push Permission",
    "scope": "callback:message:push",
    "needApproval": 0,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/callback/message-push"
    },
    {
      "propertyName": "timeout",
      "propertyValue": "10000"
    }
  ]
}'

declare -a DATA=("$DATA_1" "$DATA_2" "$DATA_3")

# ==================== 批量执行 ====================
echo "开始创建回调，共 ${#DATA[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0
RESPONSE_FILE="/tmp/callback_response.json"

for i in "${!DATA[@]}"; do
  echo ""
  echo "[$((i+1))/${#DATA[@]}] 创建回调: ${NAMES[$i]}"
  echo "--------------------------------------"
  
  http_code=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" -X POST \
    "${BASE_URL}/service/open/v2/callbacks" \
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
