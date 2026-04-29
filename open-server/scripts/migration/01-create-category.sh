#!/bin/bash
set -ex

# ==================== 注册分类脚本 ====================
# 功能：创建开放平台分类
# 接口：POST /service/open/v2/categories
# 用法：./01-create-category.sh

# 导入配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# ==================== 接口参数说明 ====================
# 接口：POST /service/open/v2/categories
# 
# 请求参数（JSON Body）：
# ┌──────────────┬────────┬──────────────────────────────────┐
# │ 参数名       │ 类型   │ 说明                              │
# ├──────────────┼────────┼──────────────────────────────────┤
# │ nameCn       │ string │ 中文名称（必填）                  │
# │ nameEn       │ string │ 英文名称（必填）                  │
# │ categoryAlias│ string │ 分类别名（可选，建议使用英文）    │
# │ parentId     │ number │ 父分类ID（可选，顶级分类不传）    │
# │ sortOrder    │ number │ 排序值（可选，默认0，值小优先）   │
# └──────────────┴────────┴──────────────────────────────────┘
#
# 响应示例：
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "id": 1,
#     "nameCn": "用户管理",
#     "nameEn": "User Management",
#     "categoryAlias": "user_mgmt",
#     "parentId": null,
#     "sortOrder": 1
#   }
# }

# ==================== 示例：创建顶级分类 ====================

# 示例1：创建"用户管理"分类
echo "========== 创建顶级分类：用户管理 =========="

CATEGORY_DATA='{
  "nameCn": "用户管理",
  "nameEn": "User Management",
  "categoryAlias": "user_mgmt",
  "sortOrder": 1
}'

RESULT=$(request "POST" "/categories" "$CATEGORY_DATA" "category_user_mgmt.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "category_user_mgmt.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "category_user_mgmt.json" "$STATUS"
    exit 1
fi

# ==================== 示例：创建子分类 ====================

# 示例2：创建"用户信息"子分类（假设父分类ID为1）
echo ""
echo "========== 创建子分类：用户信息 =========="

SUB_CATEGORY_DATA='{
  "nameCn": "用户信息",
  "nameEn": "User Info",
  "categoryAlias": "user_info",
  "parentId": 1,
  "sortOrder": 1
}'

RESULT=$(request "POST" "/categories" "$SUB_CATEGORY_DATA" "category_user_info.json")
STATUS=$(get_http_status "$RESULT")

if is_success "$STATUS"; then
    echo "✅ 创建成功"
    print_result "category_user_info.json" "$STATUS"
else
    echo "❌ 创建失败"
    print_result "category_user_info.json" "$STATUS"
    exit 1
fi

# ==================== 批量创建分类示例 ====================

# 示例3：批量创建多个分类
echo ""
echo "========== 批量创建分类 =========="

# 定义分类数组
CATEGORIES=(
  "订单管理|Order Management|order_mgmt|1"
  "商品管理|Product Management|product_mgmt|2"
  "支付管理|Payment Management|payment_mgmt|3"
)

for category in "${CATEGORIES[@]}"; do
  IFS='|' read -r name_cn name_en alias sort_order <<< "$category"
  
  DATA=$(cat <<EOF
{
  "nameCn": "$name_cn",
  "nameEn": "$name_en",
  "categoryAlias": "$alias",
  "sortOrder": $sort_order
}
EOF
)
  
  RESULT=$(request "POST" "/categories" "$DATA" "category_${alias}.json")
  STATUS=$(get_http_status "$RESULT")
  
  if is_success "$STATUS"; then
      echo "✅ 分类 '$name_cn' 创建成功"
  else
      echo "❌ 分类 '$name_cn' 创建失败"
      print_result "category_${alias}.json" "$STATUS"
  fi
done

echo ""
echo "========== 分类创建完成 =========="