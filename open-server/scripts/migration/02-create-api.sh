#!/bin/bash
set -ex

# ==================== 注册 API 脚本 ====================
# 功能：创建开放平台 API
# 接口：POST /service/open/v2/apis
# 用法：./02-create-api.sh

# 导入配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# ==================== 接口参数说明 ====================
# 接口：POST /service/open/v2/apis
# 
# 请求参数（JSON Body）：
# ┌──────────────────┬────────┬──────────────────────────────────┐
# │ 参数名           │ 类型   │ 说明                              │
# ├──────────────────┼────────┼──────────────────────────────────┤
# │ nameCn           │ string │ 中文名称（必填）                  │
# │ nameEn           │ string │ 英文名称（必填）                  │
# │ path             │ string │ API路径（必填，如 /user/info）     │
# │ method           │ string │ HTTP方法（必填，GET/POST/PUT/    │
# │                  │        │   DELETE等）                      │
# │ authType         │ string │ 认证类型（必填，如 SESSION、      │
# │                  │        │   TOKEN、NONE）                   │
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
#     "nameCn": "获取用户信息",
#     "nameEn": "Get User Info",
#     "path": "/user/info",
#     "method": "GET",
#     "authType": "SESSION",
#     "categoryId": 1
#   }
# }

# ==================== 示例：创建简单 API ====================

echo "========== 创建 API：获取用户信息 =========="

API_DATA='{
  "nameCn": "获取用户信息",
  "nameEn": "Get User Info",
  "path": "/user/info",
  "method": "GET",
  "authType": "SESSION",
  "categoryId": 1,
  "permission": {
    "nameCn": "查询用户信息权限",
    "nameEn": "Query User Info Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["user:info:read"]
  }
}'

RESULT=$(request "POST" "/apis" "$API_DATA" "api_user_info.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "api_user_info.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "api_user_info.json" "$STATUS"
    exit 1
fi

# ==================== 示例：创建带属性的 API ====================

echo ""
echo "========== 创建带属性的 API：创建订单 =========="

API_DATA_WITH_PROPS='{
  "nameCn": "创建订单",
  "nameEn": "Create Order",
  "path": "/order/create",
  "method": "POST",
  "authType": "TOKEN",
  "categoryId": 2,
  "permission": {
    "nameCn": "创建订单权限",
    "nameEn": "Create Order Permission",
    "scope": "WRITE",
    "needApproval": true,
    "resourceNodes": ["order:create", "order:write"]
  },
  "properties": [
    {
      "name": "rateLimit",
      "type": "NUMBER",
      "value": "100",
      "description": "每分钟请求限制"
    },
    {
      "name": "timeout",
      "type": "NUMBER",
      "value": "5000",
      "description": "超时时间（毫秒）"
    }
  ]
}'

RESULT=$(request "POST" "/apis" "$API_DATA_WITH_PROPS" "api_create_order.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "api_create_order.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "api_create_order.json" "$STATUS"
    exit 1
fi

# ==================== 示例：批量创建 API ====================

echo ""
echo "========== 批量创建 API =========="

# 定义 API 数组（格式：nameCn|nameEn|path|method|authType|categoryId|scope|needApproval）
APIS=(
  "查询订单列表|Query Order List|/orders|GET|SESSION|2|READ|false"
  "更新订单状态|Update Order Status|/order/status|PUT|TOKEN|2|WRITE|true"
  "删除订单|Delete Order|/order/{id}|DELETE|TOKEN|2|WRITE|true"
)

for api in "${APIS[@]}"; do
  IFS='|' read -r name_cn name_en path method auth_type category_id scope need_approval <<< "$api"
  
  DATA=$(cat <<EOF
{
  "nameCn": "$name_cn",
  "nameEn": "$name_en",
  "path": "$path",
  "method": "$method",
  "authType": "$auth_type",
  "categoryId": $category_id,
  "permission": {
    "nameCn": "${name_cn}权限",
    "nameEn": "${name_en} Permission",
    "scope": "$scope",
    "needApproval": $need_approval,
    "resourceNodes": ["${path//\//_}"]
  }
}
EOF
)
  
  RESULT=$(request "POST" "/apis" "$DATA" "api_${path//\//_}.json")
  STATUS=$(get_http_status "$RESULT")
  
  if is_success "$STATUS"; then
      echo "✅ API '$name_cn' 创建成功"
  else
      echo "❌ API '$name_cn' 创建失败"
      print_result "api_${path//\//_}.json" "$STATUS"
  fi
done

echo ""
echo "========== API 创建完成 =========="