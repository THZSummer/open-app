#!/bin/bash
# ==================== 创建分类 ====================
# 接口：POST /service/open/v2/categories

# ==================== 参数配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# 请求参数
NAME_CN="用户管理"              # 必填：中文名称
NAME_EN="User Management"       # 必填：英文名称
CATEGORY_ALIAS="user_mgmt"      # 可选：分类别名，建议使用英文
PARENT_ID="null"                # 可选：父分类ID，顶级分类为 null
SORT_ORDER=1                    # 可选：排序值，默认0，值小优先

# ==================== 执行请求 ====================
curl -X POST "${BASE_URL}/service/open/v2/categories" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=${SESSION_ID}" \
  -d "{
    \"nameCn\": \"${NAME_CN}\",
    \"nameEn\": \"${NAME_EN}\",
    \"categoryAlias\": \"${CATEGORY_ALIAS}\",
    \"parentId\": ${PARENT_ID},
    \"sortOrder\": ${SORT_ORDER}
  }" | jq .
