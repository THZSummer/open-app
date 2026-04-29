#!/bin/bash
set -ex

# ==================== 注册事件脚本 ====================
# 功能：创建开放平台事件
# 接口：POST /service/open/v2/events
# 用法：./03-create-event.sh

# 导入配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# ==================== 接口参数说明 ====================
# 接口：POST /service/open/v2/events
# 
# 请求参数（JSON Body）：
# ┌──────────────────┬────────┬──────────────────────────────────┐
# │ 参数名           │ 类型   │ 说明                              │
# ├──────────────────┼────────┼──────────────────────────────────┤
# │ nameCn           │ string │ 中文名称（必填）                  │
# │ nameEn           │ string │ 英文名称（必填）                  │
# │ topic            │ string │ 事件主题（必填，如 order.created  │
# │                  │        │   user.updated 等）               │
# │ categoryId       │ number │ 分类ID（必填）                    │
# │ permission       │ object │ 权限配置（必填）                  │
# │  ├ nameCn        │ string │ 权限中文名称                      │
# │  ├ nameEn        │ string │ 权限英文名称                      │
# │  ├ scope         │ string │ 权限范围（如 READ、WRITE、ALL）  │
# │  ├ needApproval  │ bool   │ 是否需要审批                      │
# │  └ resourceNodes │ array  │ 资源节点列表                      │
# │ properties       │ array  │ 扩展属性（可选）                  │
# │  ├ name          │ string │ 属性名称                         │
# │  ├ type          │ string │ 属性类型（STRING/NUMBER/BOOL）   │
# │  ├ value         │ string │ 属性值                           │
# │  └ description   │ string │ 属性描述                         │
# └──────────────────┴────────┴──────────────────────────────────┘
#
# 响应示例：
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "id": 1,
#     "nameCn": "订单创建事件",
#     "nameEn": "Order Created Event",
#     "topic": "order.created",
#     "categoryId": 2
#   }
# }

# ==================== 示例：创建简单事件 ====================

echo "========== 创建事件：订单创建事件 =========="

EVENT_DATA='{
  "nameCn": "订单创建事件",
  "nameEn": "Order Created Event",
  "topic": "order.created",
  "categoryId": 2,
  "permission": {
    "nameCn": "订阅订单创建事件权限",
    "nameEn": "Subscribe Order Created Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["event:order:created"]
  }
}'

RESULT=$(request "POST" "/events" "$EVENT_DATA" "event_order_created.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "event_order_created.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "event_order_created.json" "$STATUS"
    exit 1
fi

# ==================== 示例：创建带属性的事件 ====================

echo ""
echo "========== 创建带属性的事件：用户状态变更事件 =========="

EVENT_DATA_WITH_PROPS='{
  "nameCn": "用户状态变更事件",
  "nameEn": "User Status Changed Event",
  "topic": "user.status.changed",
  "categoryId": 1,
  "permission": {
    "nameCn": "订阅用户状态变更权限",
    "nameEn": "Subscribe User Status Changed Permission",
    "scope": "READ",
    "needApproval": true,
    "resourceNodes": ["event:user:status"]
  },
  "properties": [
    {
      "name": "retryPolicy",
      "type": "STRING",
      "value": "exponential",
      "description": "重试策略（linear/exponential）"
    },
    {
      "name": "maxRetry",
      "type": "NUMBER",
      "value": "3",
      "description": "最大重试次数"
    },
    {
      "name": "ttl",
      "type": "NUMBER",
      "value": "86400",
      "description": "消息存活时间（秒）"
    }
  ]
}'

RESULT=$(request "POST" "/events" "$EVENT_DATA_WITH_PROPS" "event_user_status_changed.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "event_user_status_changed.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "event_user_status_changed.json" "$STATUS"
    exit 1
fi

# ==================== 示例：批量创建事件 ====================

echo ""
echo "========== 批量创建事件 =========="

# 定义事件数组（格式：nameCn|nameEn|topic|categoryId|scope|needApproval）
EVENTS=(
  "订单支付成功|Order Paid|order.paid|2|READ|false"
  "订单发货事件|Order Shipped|order.shipped|2|READ|false"
  "订单完成事件|Order Completed|order.completed|2|READ|false"
  "订单取消事件|Order Cancelled|order.cancelled|2|READ|false"
  "商品上架事件|Product Listed|product.listed|3|READ|false"
  "商品下架事件|Product Delisted|product.delisted|3|READ|false"
)

for event in "${EVENTS[@]}"; do
  IFS='|' read -r name_cn name_en topic category_id scope need_approval <<< "$event"
  
  DATA=$(cat <<EOF
{
  "nameCn": "$name_cn",
  "nameEn": "$name_en",
  "topic": "$topic",
  "categoryId": $category_id,
  "permission": {
    "nameCn": "订阅${name_cn}权限",
    "nameEn": "Subscribe ${name_en} Permission",
    "scope": "$scope",
    "needApproval": $need_approval,
    "resourceNodes": ["event:${topic//./:}"]
  }
}
EOF
)
  
  RESULT=$(request "POST" "/events" "$DATA" "event_${topic//./_}.json")
  STATUS=$(get_http_status "$RESULT")
  
  if is_success "$STATUS"; then
      echo "✅ 事件 '$name_cn' 创建成功"
  else
      echo "❌ 事件 '$name_cn' 创建失败"
      print_result "event_${topic//./_}.json" "$STATUS"
  fi
done

echo ""
echo "========== 事件创建完成 =========="