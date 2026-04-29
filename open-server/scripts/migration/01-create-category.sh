#!/bin/bash
set -x  # 只打印命令，不自动退出

# ==================== 创建分类 ====================
# 接口：POST /service/open/v2/categories

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
CATEGORIES=(
  '{
    "nameCn": "用户管理",
    "nameEn": "User Management",
    "categoryAlias": "user_mgmt",
    "parentId": null,
    "sortOrder": 1
  }'
  '{
    "nameCn": "消息管理",
    "nameEn": "Message Management",
    "categoryAlias": "msg_mgmt",
    "parentId": null,
    "sortOrder": 2
  }'
  '{
    "nameCn": "文件管理",
    "nameEn": "File Management",
    "categoryAlias": "file_mgmt",
    "parentId": null,
    "sortOrder": 3
  }'
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称
# - categoryAlias (可选): 分类别名，建议使用英文
# - parentId (可选): 父分类ID，顶级分类为 null
# - sortOrder (可选): 排序值，默认0，值小优先

# ==================== 批量执行 ====================
echo "开始创建分类，共 ${#CATEGORIES[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0

for ((i=0; i<${#CATEGORIES[@]}; i++)); do
  data="${CATEGORIES[$i]}"
  name=$(echo "$data" | jq -r '.nameCn')
  
  echo ""
  echo "[$((i+1))/${#CATEGORIES[@]}] 创建分类: $name"
  echo "--------------------------------------"
  
  response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/service/open/v2/categories" \
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