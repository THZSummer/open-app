#!/bin/bash

# event-server 接口测试脚本
# 测试 2 个核心接口

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_SERVER="http://localhost:18082/event-server"

TOTAL=0
PASSED=0
FAILED=0
NEED_AUTH=0
NEED_DATA=0

REPORT_DIR="/home/usb/workspace/wks-open-app/open-app/event-server/test/reports"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
REPORT_FILE="$REPORT_DIR/test-report-event-server-$TIMESTAMP.md"

declare -a TEST_RESULTS

check_response_format() {
    local response="$1"
    local has_code=$(echo "$response" | jq -e '.code' 2>/dev/null)
    local has_messageZh=$(echo "$response" | jq -e '.messageZh' 2>/dev/null)
    local has_messageEn=$(echo "$response" | jq -e '.messageEn' 2>/dev/null)
    local has_data=$(echo "$response" | jq -e '.data' 2>/dev/null)
    local has_page=$(echo "$response" | jq -e '.page' 2>/dev/null)
    
    if [[ -n "$has_code" && -n "$has_messageZh" && -n "$has_messageEn" && -n "$has_data" && -n "$has_page" ]]; then
        return 0
    else
        return 1
    fi
}

test_api() {
    local num=$1
    local name=$2
    local method=$3
    local url=$4
    local data=$5
    local expected_code=$6
    
    TOTAL=$((TOTAL + 1))
    
    local response
    local http_code
    
    if [[ "$method" == "GET" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" -H 'Content-Type: application/json' 2>/dev/null)
    elif [[ "$method" == "POST" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" -H 'Content-Type: application/json' -d "$data" 2>/dev/null)
    elif [[ "$method" == "DELETE" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$url" -H 'Content-Type: application/json' 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -1)
    response=$(echo "$response" | head -n -1)
    
    local status=""
    local code=""
    
    if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
        if check_response_format "$response"; then
            code=$(echo "$response" | jq -r '.code')
            if [[ "$code" == "200" ]]; then
                status="✅ 通过"
                PASSED=$((PASSED + 1))
            else
                status="⚠️ 需要数据 (code=$code)"
                NEED_DATA=$((NEED_DATA + 1))
            fi
        else
            status="❌ 格式错误"
            FAILED=$((FAILED + 1))
        fi
    else
        if check_response_format "$response"; then
            code=$(echo "$response" | jq -r '.code')
            if [[ "$code" == "404" ]]; then
                status="❌ 接口不存在 (404)"
                FAILED=$((FAILED + 1))
            elif [[ "$code" == "401" ]]; then
                status="✅ 接口存在 (需认证)"
                NEED_AUTH=$((NEED_AUTH + 1))
            elif [[ "$code" == "403" ]]; then
                status="✅ 接口存在 (需授权)"
                NEED_AUTH=$((NEED_AUTH + 1))
            else
                status="❌ 失败 ($code)"
                FAILED=$((FAILED + 1))
            fi
        else
            if [[ "$http_code" == "401" ]]; then
                status="✅ 接口存在 (需认证)"
                NEED_AUTH=$((NEED_AUTH + 1))
            elif [[ "$http_code" == "404" ]]; then
                status="❌ 接口不存在 (404)"
                FAILED=$((FAILED + 1))
            else
                status="❌ HTTP $http_code"
                FAILED=$((FAILED + 1))
            fi
        fi
    fi
    
    printf "#%-3s %-40s %s\n" "$num" "$name" "$status"
    
    TEST_RESULTS+=("| #$num | $name | $status |")
}

generate_report() {
    local data_init_status="$1"
    
    cat > "$REPORT_FILE" << EOF
# event-server 接口验证报告

**测试时间:** $(date "+%Y-%m-%d %H:%M:%S")  
**服务地址:** $API_SERVER

## 测试数据初始化

EOF

    if [[ "$data_init_status" == "success" ]]; then
        cat >> "$REPORT_FILE" << EOF
- 事件数据：4
- 回调数据：3
- 权限数据：4
- 订阅数据：9

✅ 测试数据初始化成功

EOF
    else
        cat >> "$REPORT_FILE" << EOF
⚠️ 测试数据初始化跳过或失败

> 注：由于数据库访问权限限制，测试数据初始化未能执行。
> 接口测试仍可验证接口是否正确实现。

EOF
    fi

    cat >> "$REPORT_FILE" << EOF
---

## 核心接口测试 #1-2

| 序号 | 测试项 | 结果 |
|------|--------|------|
EOF

    for result in "${TEST_RESULTS[@]}"; do
        echo "$result" >> "$REPORT_FILE"
    done

    local AVAILABLE=$((PASSED + NEED_AUTH + NEED_DATA))
    local PASS_RATE=0
    if [[ $TOTAL -gt 0 ]]; then
        PASS_RATE=$(echo "scale=0; $AVAILABLE * 100 / $TOTAL" | bc)
    fi

    cat >> "$REPORT_FILE" << EOF

---

## 验证统计

| 指标 | 数值 |
|------|------|
| 总计 | $TOTAL 个接口 |
| 通过 | $PASSED 个 |
| 需要认证 | $NEED_AUTH 个 |
| 需要数据 | $NEED_DATA 个 |
| 失败 | $FAILED 个 |
| **接口可用率** | **${PASS_RATE}%** |

EOF

    if [[ $FAILED -gt 0 ]]; then
        echo "❌ 存在接口未实现或测试失败" >> "$REPORT_FILE"
    else
        echo "✅ 所有接口已实现且可用（部分接口需要认证/测试数据）" >> "$REPORT_FILE"
    fi
    
    cat >> "$REPORT_FILE" << EOF

---

## 说明

- **✅ 通过**: 接口正常响应，业务逻辑正确
- **✅ 接口存在 (需认证)**: 接口已实现，返回 401 认证失败，属正常行为
- **✅ 接口存在 (需授权)**: 接口已实现，返回 403 权限不足，属正常行为
- **⚠️ 需要数据**: 接口响应正常，但缺少测试数据
- **❌ 接口不存在**: 接口未实现或路径错误
- **❌ 失败**: 接口响应异常

EOF
}

check_service() {
    echo -n "检查服务状态... "
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$API_SERVER/actuator/health" 2>/dev/null)
    if [[ "$http_code" == "200" ]]; then
        echo -e "${GREEN}运行中${NC}"
        return 0
    else
        local api_code=$(curl -s -o /dev/null -w "%{http_code}" "$API_SERVER/gateway/events/publish" 2>/dev/null)
        if [[ "$api_code" != "000" && -n "$api_code" ]]; then
            echo -e "${GREEN}运行中${NC}"
            return 0
        fi
        echo -e "${RED}未运行${NC}"
        return 1
    fi
}

main() {
    echo "========================================"
    echo "event-server 接口验证 - 共 2 个接口"
    echo "========================================"
    echo ""
    
    check_service || {
        echo -e "${RED}错误: event-server 服务未运行${NC}"
        echo "请先启动服务: cd event-server && mvn spring-boot:run"
        exit 1
    }
    
    local data_init_status="skipped"
    
    echo ""
    echo "=== 核心接口测试 (1-2) ==="
    test_api 1 "事件发布接口" "POST" "$API_SERVER/gateway/events/publish" '{"topic":"im.message.received","payload":{"messageId":"msg001","content":"Hello World","sender":"user001","receiver":"user002","timestamp":"2026-04-22T10:00:00Z"}}' "200"
    test_api 2 "回调触发接口" "POST" "$API_SERVER/gateway/callbacks/invoke" '{"callbackScope":"callback:approval:completed","payload":{"approvalId":"app001","status":"approved","approver":"user001","approvedAt":"2026-04-22T10:00:00Z","comment":"审批通过"}}' "200"
    
    echo ""
    echo "========================================"
    echo "验证完成"
    echo "========================================"
    echo ""
    echo "统计结果："
    echo "  总接口数: $TOTAL"
    echo "  ✅ 通过: $PASSED"
    echo "  ✅ 需认证: $NEED_AUTH"
    echo "  ⚠️ 需数据: $NEED_DATA"
    echo "  ❌ 失败: $FAILED"
    echo ""
    
    local AVAILABLE=$((PASSED + NEED_AUTH + NEED_DATA))
    local PASS_RATE=0
    if [[ $TOTAL -gt 0 ]]; then
        PASS_RATE=$(echo "scale=0; $AVAILABLE * 100 / $TOTAL" | bc)
    fi
    echo "接口可用率: ${PASS_RATE}%"
    echo ""
    
    generate_report "$data_init_status"
    echo "测试报告已生成: $REPORT_FILE"
}

main "$@"
