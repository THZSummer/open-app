#!/bin/bash
set -ex

# ==================== 创建分类 ====================
# 接口：POST /service/open/v2/categories

# 修改以下配置
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称
# - categoryAlias (可选): 分类别名，建议使用英文
# - parentId (可选): 父分类ID，顶级分类为 null
# - sortOrder (可选): 排序值，默认0，值小优先

curl -X POST "${BASE_URL}/service/open/v2/categories" \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSIONID=${SESSION_ID}" \
  -d '{
    "nameCn": "用户管理",
    "nameEn": "User Management",
    "categoryAlias": "user_mgmt",
    "parentId": null,
    "sortOrder": 1
  }' | jq .
