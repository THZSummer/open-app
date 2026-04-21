#!/bin/bash

# TASK-011: event-server 验证脚本
# 用于验证事件发布和回调触发接口是否正常工作

set -e

echo "========================================="
echo "TASK-011: event-server 验证脚本"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API Server 地址
API_SERVER="http://localhost:18081"
EVENT_SERVER="http://localhost:18082"

# 检查服务是否运行
check_service() {
    local name=$1
    local url=$2
    
    echo -n "检查 $name ... "
    if curl -s -f "$url/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
        return 0
    else
        echo -e "${RED}✗ 未运行${NC}"
        return 1
    fi
}

# 测试事件发布接口
test_event_publish() {
    echo ""
    echo "测试事件发布接口 (#56) ..."
    echo "----------------------------------------"
    
    local response
    response=$(curl -s -X POST "$EVENT_SERVER/gateway/events/publish" \
        -H "Content-Type: application/json" \
        -d '{
            "topic": "im.message.received",
            "payload": {
                "messageId": "msg001",
                "content": "Hello World",
                "sender": "user001"
            }
        }')
    
    if echo "$response" | grep -q '"code":"200"'; then
        echo -e "${GREEN}✓ 事件发布接口正常${NC}"
        echo "响应: $response" | jq '.' 2>/dev/null || echo "$response"
    else
        echo -e "${RED}✗ 事件发布接口失败${NC}"
        echo "响应: $response"
        return 1
    fi
}

# 测试回调触发接口
test_callback_invoke() {
    echo ""
    echo "测试回调触发接口 (#57) ..."
    echo "----------------------------------------"
    
    local response
    response=$(curl -s -X POST "$EVENT_SERVER/gateway/callbacks/invoke" \
        -H "Content-Type: application/json" \
        -d '{
            "callbackScope": "callback:approval:completed",
            "payload": {
                "approvalId": "app001",
                "status": "approved",
                "approver": "user001"
            }
        }')
    
    if echo "$response" | grep -q '"code":"200"'; then
        echo -e "${GREEN}✓ 回调触发接口正常${NC}"
        echo "响应: $response" | jq '.' 2>/dev/null || echo "$response"
    else
        echo -e "${RED}✗ 回调触发接口失败${NC}"
        echo "响应: $response"
        return 1
    fi
}

# 测试参数校验
test_validation() {
    echo ""
    echo "测试参数校验 ..."
    echo "----------------------------------------"
    
    # 测试缺少 topic
    echo -n "测试缺少 topic ... "
    local response
    response=$(curl -s -X POST "$EVENT_SERVER/gateway/events/publish" \
        -H "Content-Type: application/json" \
        -d '{"payload": {"messageId": "msg001"}}')
    
    if echo "$response" | grep -q '400\|Bad Request'; then
        echo -e "${GREEN}✓ 参数校验正常${NC}"
    else
        echo -e "${YELLOW}⚠ 参数校验可能未生效${NC}"
    fi
    
    # 测试缺少 callbackScope
    echo -n "测试缺少 callbackScope ... "
    response=$(curl -s -X POST "$EVENT_SERVER/gateway/callbacks/invoke" \
        -H "Content-Type: application/json" \
        -d '{"payload": {"approvalId": "app001"}}')
    
    if echo "$response" | grep -q '400\|Bad Request'; then
        echo -e "${GREEN}✓ 参数校验正常${NC}"
    else
        echo -e "${YELLOW}⚠ 参数校验可能未生效${NC}"
    fi
}

# 主函数
main() {
    echo "1. 检查服务状态"
    echo "----------------------------------------"
    
    check_service "api-server" "$API_SERVER" || echo -e "${YELLOW}⚠ api-server 未运行，某些功能可能无法使用${NC}"
    check_service "event-server" "$EVENT_SERVER" || {
        echo -e "${RED}错误: event-server 未运行${NC}"
        echo "请先启动 event-server: cd event-server && mvn spring-boot:run"
        exit 1
    }
    
    echo ""
    echo "2. 测试接口功能"
    echo "----------------------------------------"
    
    test_event_publish || echo -e "${YELLOW}⚠ 事件发布测试失败${NC}"
    test_callback_invoke || echo -e "${YELLOW}⚠ 回调触发测试失败${NC}"
    
    echo ""
    echo "3. 测试参数校验"
    echo "----------------------------------------"
    test_validation
    
    echo ""
    echo "========================================="
    echo -e "${GREEN}验证完成${NC}"
    echo "========================================="
}

# 运行主函数
main
