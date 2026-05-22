#!/bin/bash
set -e

# ==================== 创建分类 ====================
# 接口：POST /service/open/v2/categories

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
declare -a NAMES=("用户管理" "消息管理" "文件管理")

DATA_1='{
  "nameCn": "用户管理",
  "nameEn": "User Management",
  "categoryAlias": "user_mgmt",
  "parentId": null,
  "sortOrder": 1
}'

DATA_2='{
  "nameCn": "消息管理",
  "nameEn": "Message Management",
  "categoryAlias": "msg_mgmt",
  "parentId": null,
  "sortOrder": 2
}'

DATA_3='{
  "nameCn": "文件管理",
  "nameEn": "File Management",
  "categoryAlias": "file_mgmt",
  "parentId": null,
  "sortOrder": 3
}'

declare -a DATA=("$DATA_1" "$DATA_2" "$DATA_3")

# ==================== 批量执行 ====================
echo "开始创建分类，共 ${#DATA[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0
RESPONSE_FILE="/tmp/category_response.json"

for i in "${!DATA[@]}"; do
  echo ""
  echo "[$((i+1))/${#DATA[@]}] 创建分类: ${NAMES[$i]}"
  echo "--------------------------------------"
  
  http_code=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" -X POST \
    "${BASE_URL}/service/open/v2/categories" \
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
