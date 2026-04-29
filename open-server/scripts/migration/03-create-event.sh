#!/bin/bash
set -x  # 只打印命令，不自动退出

# ==================== 创建事件 ====================
# 接口：POST /service/open/v2/events

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
EVENTS=(
  '{
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
  '{
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
  '{
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
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - topic (必填): 事件主题，格式 {模块}.{事件}
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 event:{模块}:{事件}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表

# ==================== 批量执行 ====================
echo "开始创建事件，共 ${#EVENTS[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0

for ((i=0; i<${#EVENTS[@]}; i++)); do
  data="${EVENTS[$i]}"
  name=$(echo "$data" | jq -r '.nameCn')
  
  echo ""
  echo "[$((i+1))/${#EVENTS[@]}] 创建事件: $name"
  echo "--------------------------------------"
  
  response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/service/open/v2/events" \
    -H "Content-Type: application/json" \
    -H "Cookie: SESSIONID=${SESSION_ID}" \
    -d "$data")
  
  http_code=$(echo "$response" | tail -n 1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo "✅ 创建成功"
    echo "$body" | jq .
    ((SUCCESS++))
  else
    echo "❌ 创建失败 (HTTP $http_code)"
    echo "$body" | jq . 2>/dev/null || echo "$body"
    ((FAILED++))
  fi
done

echo ""
echo "======================================"
echo "执行完成: 成功 $SUCCESS 条，失败 $FAILED 条"