#!/bin/bash
set -ex

# ==================== 注册回调脚本 ====================
# 功能：创建开放平台回调
# 接口：POST /service/open/v2/callbacks
# 用法：./04-create-callback.sh

# 导入配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# ==================== 接口参数说明 ====================
# 接口：POST /service/open/v2/callbacks
# 
# 请求参数（JSON Body）：
# ┌──────────────────┬────────┬──────────────────────────────────┐
# │ 参数名           │ 类型   │ 说明                              │
# ├──────────────────┼────────┼──────────────────────────────────┤
# │ nameCn           │ string │ 中文名称（必填）                  │
# │ nameEn           │ string │ 英文名称（必填）                  │
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
#     "nameCn": "订单状态变更回调",
#     "nameEn": "Order Status Change Callback",
#     "categoryId": 2
#   }
# }

# ==================== 示例：创建简单回调 ====================

echo "========== 创建回调：订单状态变更回调 =========="

CALLBACK_DATA='{
  "nameCn": "订单状态变更回调",
  "nameEn": "Order Status Change Callback",
  "categoryId": 2,
  "permission": {
    "nameCn": "接收订单状态变更权限",
    "nameEn": "Receive Order Status Change Permission",
    "scope": "READ",
    "needApproval": false,
    "resourceNodes": ["callback:order:status"]
  }
}'

RESULT=$(request "POST" "/callbacks" "$CALLBACK_DATA" "callback_order_status.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "callback_order_status.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "callback_order_status.json" "$STATUS"
    exit 1
fi

# ==================== 示例：创建带属性的回调 ====================

echo ""
echo "========== 创建带属性的回调：支付结果通知回调 =========="

CALLBACK_DATA_WITH_PROPS='{
  "nameCn": "支付结果通知回调",
  "nameEn": "Payment Result Notification Callback",
  "categoryId": 3,
  "permission": {
    "nameCn": "接收支付结果权限",
    "nameEn": "Receive Payment Result Permission",
    "scope": "READ",
    "needApproval": true,
    "resourceNodes": ["callback:payment:result"]
  },
  "properties": [
    {
      "name": "timeout",
      "type": "NUMBER",
      "value": "5000",
      "description": "回调超时时间（毫秒）"
    },
    {
      "name": "retryTimes",
      "type": "NUMBER",
      "value": "3",
      "description": "重试次数"
    },
    {
      "name": "signType",
      "type": "STRING",
      "value": "SHA256",
      "description": "签名算法（MD5/SHA256/RSA）"
    },
    {
      "name": "async",
      "type": "BOOL",
      "value": "true",
      "description": "是否异步回调"
    }
  ]
}'

RESULT=$(request "POST" "/callbacks" "$CALLBACK_DATA_WITH_PROPS" "callback_payment_result.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "callback_payment_result.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "callback_payment_result.json" "$STATUS"
    exit 1
fi

# ==================== 示例：批量创建回调 ====================

echo ""
echo "========== 批量创建回调 =========="

# 定义回调数组（格式：nameCn|nameEn|categoryId|scope|needApproval）
CALLBACKS=(
  "退款结果通知|Refund Result Callback|3|READ|false"
  "用户注册通知|User Registration Callback|1|READ|false"
  "用户认证结果|User Auth Result Callback|1|READ|true"
  "商品审核结果|Product Audit Result Callback|3|READ|true"
  "物流状态更新|Logistics Status Update Callback|2|READ|false"
)

for callback in "${CALLBACKS[@]}"; do
  IFS='|' read -r name_cn name_en category_id scope need_approval <<< "$callback"
  
  # 生成 resourceNode
  RESOURCE_NODE="callback:${name_en// /_}"
  
  DATA=$(cat <<EOF
{
  "nameCn": "$name_cn",
  "nameEn": "$name_en",
  "categoryId": $category_id,
  "permission": {
    "nameCn": "接收${name_cn}权限",
    "nameEn": "Receive ${name_en} Permission",
    "scope": "$scope",
    "needApproval": $need_approval,
    "resourceNodes": ["$RESOURCE_NODE"]
  }
}
EOF
)
  
  RESULT=$(request "POST" "/callbacks" "$DATA" "callback_${name_en// /_}.json")
  STATUS=$(get_http_status "$RESULT")
  
  if is_success "$STATUS"; then
      echo "✅ 回调 '$name_cn' 创建成功"
  else
      echo "❌ 回调 '$name_cn' 创建失败"
      print_result "callback_${name_en// /_}.json" "$STATUS"
  fi
done

echo ""
echo "========== 回调创建完成 =========="

# ==================== 总结 ====================

echo ""
echo "========== 迁移脚本执行完成 =========="
echo "所有回调创建脚本已完成"
echo "输出文件保存在：$OUTPUT_DIR"
echo ""
echo "文件列表："
ls -lh "$OUTPUT_DIR"