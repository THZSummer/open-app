#!/bin/bash

# api-server 测试数据初始化脚本
# 根据测试用例文档准备测试数据

# 数据库配置
DB_NAME="openplatform"

# 使用 sudo mysql 方式连接
MYSQL_CMD="sudo mysql"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "api-server 测试数据初始化"
echo "========================================"
echo ""

# 统计变量
APP_COUNT=0
PERMISSION_COUNT=0
SUBSCRIPTION_COUNT=0
USER_AUTH_COUNT=0

# 执行SQL并统计
execute_sql() {
    local sql="$1"
    $MYSQL_CMD "$DB_NAME" -e "$sql" 2>/dev/null
}

# 检查数据库连接
echo -n "检查数据库连接... "
if $MYSQL_CMD -e "SELECT 1" &>/dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}失败${NC}"
    echo "请检查数据库连接配置（可能需要 sudo 权限）"
    exit 1
fi

echo ""
echo "=== 1. 清理旧测试数据 ==="

execute_sql "DELETE FROM openplatform_v2_user_authorization_t WHERE id >= 600 AND id < 700;"
execute_sql "DELETE FROM openplatform_v2_subscription_t WHERE id >= 300 AND id < 400;"
execute_sql "DELETE FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 2000;"
execute_sql "DELETE FROM openplatform_v2_app_t WHERE id >= 10 AND id < 20;"

echo "旧测试数据清理完成"
echo ""

echo "=== 2. 插入应用数据 ==="

execute_sql "INSERT INTO openplatform_v2_app_t (id, name, app_key, status) VALUES
(10, '消息助手', 'app_key_10', 1),
(11, '云盘助手', 'app_key_11', 1);"

APP_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_app_t WHERE id >= 10 AND id < 20;" | tail -1)
echo "应用数据：$APP_COUNT 条"
echo ""

echo "=== 3. 插入权限数据 ==="

execute_sql "INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, status) VALUES
(1000, '发送消息权限', 'Send Message Permission', 'api:im:send-message', 'api', 100, 2, 1),
(1001, '获取消息权限', 'Get Message Permission', 'api:im:get-message', 'api', 101, 2, 1),
(2000, '文件上传权限', 'File Upload Permission', 'api:cloud:upload', 'api', 200, 3, 1);"

PERMISSION_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_permission_t WHERE id >= 1000 AND id < 3000;" | tail -1)
echo "权限数据：$PERMISSION_COUNT 条"
echo ""

echo "=== 4. 插入订阅关系数据 ==="

execute_sql "INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, auth_type, create_time) VALUES
(300, 10, 1000, 1, 0, NOW()),
(301, 10, 1001, 1, 0, NOW()),
(302, 11, 2000, 1, 1, NOW());"

SUBSCRIPTION_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_subscription_t WHERE id >= 300 AND id < 400;" | tail -1)
echo "订阅数据：$SUBSCRIPTION_COUNT 条"
echo ""

echo "=== 5. 插入用户授权数据 ==="

execute_sql "INSERT INTO openplatform_v2_user_authorization_t (id, user_id, app_id, scopes, expires_at, create_time) VALUES
(600, 'user001', 10, '[\"api:im:send-message\", \"api:im:get-message\"]', '2026-12-31 23:59:59', NOW()),
(601, 'user002', 10, '[\"api:im:send-message\"]', '2026-06-30 23:59:59', NOW()),
(602, 'user003', 11, '[\"api:cloud:upload\"]', NULL, NOW());"

USER_AUTH_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_user_authorization_t WHERE id >= 600 AND id < 700;" | tail -1)
echo "用户授权数据：$USER_AUTH_COUNT 条"
echo ""

echo "========================================"
echo "测试数据初始化完成"
echo "========================================"
echo ""
echo "统计："
echo "  应用数据：$APP_COUNT"
echo "  权限数据：$PERMISSION_COUNT"
echo "  订阅数据：$SUBSCRIPTION_COUNT"
echo "  用户授权：$USER_AUTH_COUNT"
echo ""
echo -e "${GREEN}✅ 测试数据初始化成功${NC}"
