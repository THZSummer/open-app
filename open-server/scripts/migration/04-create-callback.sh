#!/bin/bash
set -x  # 只打印命令，不自动退出

# ==================== 创建回调 ====================
# 接口：POST /service/open/v2/callbacks

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
CALLBACKS=(
  '{
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
  '{
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
  '{
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
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 callback:{模块}:{资源}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表

# ==================== 批量执行 ====================
echo "开始创建回调，共 ${#CALLBACKS[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0

for ((i=0; i<${#CALLBACKS[@]}; i++)); do
  data="${CALLBACKS[$i]}"
  name=$(echo "$data" | jq -r '.nameCn')
  
  echo ""
  echo "[$((i+1))/${#CALLBACKS[@]}] 创建回调: $name"
  echo "--------------------------------------"
  
  response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/service/open/v2/callbacks" \
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