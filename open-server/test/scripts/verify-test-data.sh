#!/bin/bash

# 测试数据初始化验证脚本
# 用于验证测试数据是否正确初始化

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="openapp"
DB_USER="root"
DB_PASS=""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "测试数据初始化验证"
echo "========================================"
echo ""

# 执行SQL查询函数
query_db() {
    local sql=$1
    if [ -n "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -e "$sql"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" -N -e "$sql"
    fi
}

# 验证分类数据
echo -n "验证分类数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_category_t WHERE id >= 1 AND id < 100")
if [ "$count" -ge 5 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少5条，实际 $count 条)${NC}"
fi

# 验证API数据
echo -n "验证API数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_api_t WHERE id >= 100 AND id < 200")
if [ "$count" -ge 3 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少3条，实际 $count 条)${NC}"
fi

# 验证待审API (ID=102)
echo -n "验证待审API (ID=102)... "
status=$(query_db "SELECT status FROM openplatform_v2_api_t WHERE id = 102")
if [ "$status" = "1" ]; then
    echo -e "${GREEN}✓ 通过 (状态: 待审)${NC}"
else
    echo -e "${RED}✗ 失败 (状态: $status)${NC}"
fi

# 验证事件数据
echo -n "验证事件数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300")
if [ "$count" -ge 2 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少2条，实际 $count 条)${NC}"
fi

# 验证待审事件 (ID=201)
echo -n "验证待审事件 (ID=201)... "
status=$(query_db "SELECT status FROM openplatform_v2_event_t WHERE id = 201")
if [ "$status" = "1" ]; then
    echo -e "${GREEN}✓ 通过 (状态: 待审)${NC}"
else
    echo -e "${RED}✗ 失败 (状态: $status)${NC}"
fi

# 验证回调数据
echo -n "验证回调数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400")
if [ "$count" -ge 2 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少2条，实际 $count 条)${NC}"
fi

# 验证待审回调 (ID=301)
echo -n "验证待审回调 (ID=301)... "
status=$(query_db "SELECT status FROM openplatform_v2_callback_t WHERE id = 301")
if [ "$status" = "1" ]; then
    echo -e "${GREEN}✓ 通过 (状态: 待审)${NC}"
else
    echo -e "${RED}✗ 失败 (状态: $status)${NC}"
fi

# 验证权限数据
echo -n "验证权限数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 1100")
if [ "$count" -ge 4 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少4条，实际 $count 条)${NC}"
fi

# 验证订阅数据
echo -n "验证订阅数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_subscription_t WHERE id >= 300 AND id < 400")
if [ "$count" -ge 3 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少3条，实际 $count 条)${NC}"
fi

# 验证订阅ID=300,301,302
echo -n "验证订阅ID=300,301,302存在... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_subscription_t WHERE id IN (300, 301, 302)")
if [ "$count" = "3" ]; then
    echo -e "${GREEN}✓ 通过 (3条订阅记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望3条，实际 $count 条)${NC}"
fi

# 验证审批流程数据
echo -n "验证审批流程数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_approval_flow_t WHERE id >= 1 AND id < 100")
if [ "$count" -ge 2 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少2条，实际 $count 条)${NC}"
fi

# 验证审批记录数据
echo -n "验证审批记录数据... "
count=$(query_db "SELECT COUNT(*) FROM openplatform_v2_approval_record_t WHERE id >= 500 AND id < 600")
if [ "$count" -ge 1 ]; then
    echo -e "${GREEN}✓ 通过 ($count 条记录)${NC}"
else
    echo -e "${RED}✗ 失败 (期望至少1条，实际 $count 条)${NC}"
fi

# 验证审批记录ID=500
echo -n "验证审批记录 (ID=500)... "
result=$(query_db "SELECT id, status FROM openplatform_v2_approval_record_t WHERE id = 500")
if [ -n "$result" ]; then
    echo -e "${GREEN}✓ 通过 (ID=500存在)${NC}"
else
    echo -e "${RED}✗ 失败 (ID=500不存在)${NC}"
fi

echo ""
echo "========================================"
echo "验证完成"
echo "========================================"
