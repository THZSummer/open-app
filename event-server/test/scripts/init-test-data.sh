#!/bin/bash
set -x
# event-server 测试数据初始化脚本
# 根据测试用例文档准备测试数据

DB_NAME="openplatform_v2"

MYSQL_CMD="sudo mysql"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "event-server 测试数据初始化"
echo "========================================"
echo ""

EVENT_COUNT=0
CALLBACK_COUNT=0
PERMISSION_COUNT=0
SUBSCRIPTION_COUNT=0

execute_sql() {
    local sql="$1"
    $MYSQL_CMD "$DB_NAME" -e "$sql" 2>/dev/null
}

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

execute_sql "DELETE FROM openplatform_v2_subscription_t WHERE id >= 401 AND id < 600;"
execute_sql "DELETE FROM openplatform_v2_permission_t WHERE id >= 2000 AND id < 4000;"
execute_sql "DELETE FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;"
execute_sql "DELETE FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;"

echo "旧测试数据清理完成"
echo ""

echo "=== 2. 插入事件数据 ==="

execute_sql "INSERT INTO openplatform_v2_event_t (id, name_cn, name_en, topic, status) VALUES
(200, '消息接收事件', 'Message Received Event', 'im.message.received', 2),
(201, '消息已读事件', 'Message Read Event', 'im.message.read', 2),
(202, '用户上线事件', 'User Online Event', 'user.online', 2),
(203, '会议开始事件', 'Meeting Started Event', 'meeting.started', 1);"

EVENT_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_event_t WHERE id >= 200 AND id < 300;" | tail -1)
echo "事件数据：$EVENT_COUNT 条"
echo ""

echo "=== 3. 插入回调数据 ==="

execute_sql "INSERT INTO openplatform_v2_callback_t (id, name_cn, name_en, status) VALUES
(300, '审批完成回调', 'Approval Completed Callback', 2),
(301, '文件上传回调', 'File Upload Callback', 2),
(302, '订单状态变更回调', 'Order Status Changed Callback', 1);"

CALLBACK_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_callback_t WHERE id >= 300 AND id < 400;" | tail -1)
echo "回调数据：$CALLBACK_COUNT 条"
echo ""

echo "=== 4. 插入权限数据 ==="

execute_sql "INSERT INTO openplatform_v2_permission_t (id, name_cn, name_en, scope, resource_type, resource_id, category_id, status) VALUES
(2000, '消息接收权限', 'Message Received Permission', 'event:im:message-received', 'event', 200, 2, 1),
(2001, '消息已读权限', 'Message Read Permission', 'event:im:message-read', 'event', 201, 2, 1),
(3000, '审批完成回调权限', 'Approval Completed Callback Permission', 'callback:approval:completed', 'callback', 300, 5, 1),
(3001, '文件上传回调权限', 'File Upload Callback Permission', 'callback:file:uploaded', 'callback', 301, 5, 1);"

PERMISSION_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_permission_t WHERE id >= 2000 AND id < 4000;" | tail -1)
echo "权限数据：$PERMISSION_COUNT 条"
echo ""

echo "=== 5. 插入订阅关系数据 ==="

execute_sql "INSERT INTO openplatform_v2_subscription_t (id, app_id, permission_id, status, channel_type, channel_address, auth_type, create_time) VALUES
(401, 10, 2000, 1, 0, NULL, 0, NOW()),
(402, 11, 2000, 1, 1, 'https://webhook.app11.com/events', 0, NOW()),
(403, 12, 2001, 1, 1, 'https://webhook.app12.com/events', 1, NOW()),
(501, 10, 3000, 1, 0, 'https://webhook.app10.com/callback', 0, NOW()),
(502, 11, 3000, 1, 1, 'https://sse.app11.com/callback', 0, NOW()),
(503, 12, 3001, 1, 2, 'wss://ws.app12.com/callback', 0, NOW()),
(404, 13, 2000, 0, NULL, NULL, 0, NOW()),
(405, 14, 2000, 3, NULL, NULL, 0, NOW()),
(504, 15, 3000, 0, NULL, NULL, 0, NOW());"

SUBSCRIPTION_COUNT=$(execute_sql "SELECT COUNT(*) FROM openplatform_v2_subscription_t WHERE id >= 401 AND id < 600;" | tail -1)
echo "订阅数据：$SUBSCRIPTION_COUNT 条"
echo ""

echo "========================================"
echo "测试数据初始化完成"
echo "========================================"
echo ""
echo "统计："
echo "  事件数据：$EVENT_COUNT"
echo "  回调数据：$CALLBACK_COUNT"
echo "  权限数据：$PERMISSION_COUNT"
echo "  订阅数据：$SUBSCRIPTION_COUNT"
echo ""
echo -e "${GREEN}✅ 测试数据初始化成功${NC}"
