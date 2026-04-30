#!/bin/bash
set -x
# 能力开放平台接口验证脚本
# 测试所有 58 个接口

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务端口
OPEN_SERVER="http://localhost:18080/open-server"
API_SERVER="http://localhost:18081/api-server"
EVENT_SERVER="http://localhost:18082/event-server"

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
NEED_DATA=0

# 存储测试结果
RESULTS=""

# 测试用应用ID（假设使用测试应用）
TEST_APP_ID="10"

# 检查响应格式是否符合规范
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

# 测试接口
test_api() {
    local num=$1
    local name=$2
    local method=$3
    local url=$4
    local data=$5
    local server=$6
    local need_app_id=$7
    
    TOTAL=$((TOTAL + 1))
    
    # 构建请求头
    local headers="-H 'Content-Type: application/json'"
    if [[ "$need_app_id" == "true" ]]; then
        headers="$headers -H 'X-App-Id: $TEST_APP_ID'"
    fi
    
    # 发送请求
    local response
    local http_code
    
    if [[ "$method" == "GET" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$url" $headers 2>/dev/null)
    elif [[ "$method" == "POST" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" $headers -d "$data" 2>/dev/null)
    elif [[ "$method" == "PUT" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$url" $headers -d "$data" 2>/dev/null)
    elif [[ "$method" == "DELETE" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$url" $headers 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -1)
    response=$(echo "$response" | head -n -1)
    
    # 检查结果
    local status=""
    local code=""
    local format_ok=false
    
    # 检查HTTP状态码
    if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
        # 检查响应格式
        if check_response_format "$response"; then
            code=$(echo "$response" | jq -r '.code')
            if [[ "$code" == "200" ]]; then
                status="✅ 通过"
                PASSED=$((PASSED + 1))
            else
                status="⚠️ 需要数据"
                NEED_DATA=$((NEED_DATA + 1))
            fi
            format_ok=true
        else
            status="❌ 格式错误"
            FAILED=$((FAILED + 1))
        fi
    else
        # 非2xx状态码，检查是否返回了标准错误格式
        if check_response_format "$response"; then
            code=$(echo "$response" | jq -r '.code')
            if [[ "$code" == "401" || "$code" == "403" ]]; then
                status="⚠️ 需要认证"
                NEED_DATA=$((NEED_DATA + 1))
                format_ok=true
            elif [[ "$code" == "404" ]]; then
                status="⚠️ 需要数据"
                NEED_DATA=$((NEED_DATA + 1))
                format_ok=true
            else
                status="❌ 失败($code)"
                FAILED=$((FAILED + 1))
            fi
        else
            status="❌ HTTP $http_code"
            FAILED=$((FAILED + 1))
        fi
    fi
    
    # 记录结果
    printf "%-3s %-50s %s\n" "#$num" "$name" "$status" | tee -a /tmp/api_test_results.txt
    
    # 详细错误信息（调试用）
    if [[ "$status" == *"❌"* ]]; then
        echo "    HTTP: $http_code, Response: $(echo "$response" | head -c 200)" >> /tmp/api_test_details.txt
    fi
}

# 清空结果文件
> /tmp/api_test_results.txt
> /tmp/api_test_details.txt

echo "========================================"
echo "能力开放平台接口验证 - 共 58 个接口"
echo "========================================"
echo ""

echo "=== 1. 分类管理 (1-8) ==="
test_api 1 "GET /api/v1/categories" "GET" "$API_SERVER/api/v1/categories" "" "api" "true"
test_api 2 "GET /api/v1/categories/:id" "GET" "$API_SERVER/api/v1/categories/1" "" "api" "true"
test_api 3 "POST /api/v1/categories" "POST" "$API_SERVER/api/v1/categories" '{"categoryAlias":"test_alias","nameCn":"测试分类","nameEn":"Test Category"}' "api" "true"
test_api 4 "PUT /api/v1/categories/:id" "PUT" "$API_SERVER/api/v1/categories/1" '{"nameCn":"更新分类","nameEn":"Updated Category"}' "api" "true"
test_api 5 "DELETE /api/v1/categories/:id" "DELETE" "$API_SERVER/api/v1/categories/999" "" "api" "true"
test_api 6 "POST /api/v1/categories/:id/owners" "POST" "$API_SERVER/api/v1/categories/1/owners" '{"userId":"user001","userName":"测试用户"}' "api" "true"
test_api 7 "GET /api/v1/categories/:id/owners" "GET" "$API_SERVER/api/v1/categories/1/owners" "" "api" "true"
test_api 8 "DELETE /api/v1/categories/:id/owners/:userId" "DELETE" "$API_SERVER/api/v1/categories/1/owners/user001" "" "api" "true"

echo ""
echo "=== 2. API 管理 (9-14) ==="
test_api 9 "GET /api/v1/apis" "GET" "$API_SERVER/api/v1/apis" "" "api" "true"
test_api 10 "GET /api/v1/apis/:id" "GET" "$API_SERVER/api/v1/apis/1" "" "api" "true"
test_api 11 "POST /api/v1/apis" "POST" "$API_SERVER/api/v1/apis" '{"nameCn":"测试API","nameEn":"Test API","path":"/api/v1/test","method":"GET","categoryId":"1","permission":{"nameCn":"测试权限","nameEn":"Test Permission","scope":"api:test:read"}}' "api" "true"
test_api 12 "PUT /api/v1/apis/:id" "PUT" "$API_SERVER/api/v1/apis/1" '{"nameCn":"更新API","nameEn":"Updated API"}' "api" "true"
test_api 13 "DELETE /api/v1/apis/:id" "DELETE" "$API_SERVER/api/v1/apis/999" "" "api" "true"
test_api 14 "POST /api/v1/apis/:id/withdraw" "POST" "$API_SERVER/api/v1/apis/1/withdraw" '' "api" "true"

echo ""
echo "=== 3. 事件管理 (15-20) ==="
test_api 15 "GET /api/v1/events" "GET" "$API_SERVER/api/v1/events" "" "api" "true"
test_api 16 "GET /api/v1/events/:id" "GET" "$API_SERVER/api/v1/events/1" "" "api" "true"
test_api 17 "POST /api/v1/events" "POST" "$API_SERVER/api/v1/events" '{"nameCn":"测试事件","nameEn":"Test Event","topic":"test.event.occurred","categoryId":"1","permission":{"nameCn":"测试事件权限","nameEn":"Test Event Permission","scope":"event:test:occurred"}}' "api" "true"
test_api 18 "PUT /api/v1/events/:id" "PUT" "$API_SERVER/api/v1/events/1" '{"nameCn":"更新事件","nameEn":"Updated Event"}' "api" "true"
test_api 19 "DELETE /api/v1/events/:id" "DELETE" "$API_SERVER/api/v1/events/999" "" "api" "true"
test_api 20 "POST /api/v1/events/:id/withdraw" "POST" "$API_SERVER/api/v1/events/1/withdraw" '' "api" "true"

echo ""
echo "=== 4. 回调管理 (21-26) ==="
test_api 21 "GET /api/v1/callbacks" "GET" "$API_SERVER/api/v1/callbacks" "" "api" "true"
test_api 22 "GET /api/v1/callbacks/:id" "GET" "$API_SERVER/api/v1/callbacks/1" "" "api" "true"
test_api 23 "POST /api/v1/callbacks" "POST" "$API_SERVER/api/v1/callbacks" '{"nameCn":"测试回调","nameEn":"Test Callback","categoryId":"1","permission":{"nameCn":"测试回调权限","nameEn":"Test Callback Permission","scope":"callback:test:completed"}}' "api" "true"
test_api 24 "PUT /api/v1/callbacks/:id" "PUT" "$API_SERVER/api/v1/callbacks/1" '{"nameCn":"更新回调","nameEn":"Updated Callback"}' "api" "true"
test_api 25 "DELETE /api/v1/callbacks/:id" "DELETE" "$API_SERVER/api/v1/callbacks/999" "" "api" "true"
test_api 26 "POST /api/v1/callbacks/:id/withdraw" "POST" "$API_SERVER/api/v1/callbacks/1/withdraw" '' "api" "true"

echo ""
echo "=== 5. API 权限管理 (27-30) ==="
test_api 27 "GET /api/v1/apps/:appId/apis" "GET" "$API_SERVER/api/v1/apps/$TEST_APP_ID/apis" "" "api" "true"
test_api 28 "GET /api/v1/categories/:id/apis" "GET" "$API_SERVER/api/v1/categories/1/apis" "" "api" "true"
test_api 29 "POST /api/v1/apps/:appId/apis/subscribe" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/apis/subscribe" '{"permissionIds":["1"]}' "api" "true"
test_api 30 "POST /api/v1/apps/:appId/apis/:id/withdraw" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/apis/1/withdraw" '' "api" "true"

echo ""
echo "=== 6. 事件权限管理 (31-35) ==="
test_api 31 "GET /api/v1/apps/:appId/events" "GET" "$API_SERVER/api/v1/apps/$TEST_APP_ID/events" "" "api" "true"
test_api 32 "GET /api/v1/categories/:id/events" "GET" "$API_SERVER/api/v1/categories/1/events" "" "api" "true"
test_api 33 "POST /api/v1/apps/:appId/events/subscribe" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/events/subscribe" '{"permissionIds":["1"]}' "api" "true"
test_api 34 "PUT /api/v1/apps/:appId/events/:id/config" "PUT" "$API_SERVER/api/v1/apps/$TEST_APP_ID/events/1/config" '{"channelType":1,"channelAddress":"https://test.com/webhook","authType":0}' "api" "true"
test_api 35 "POST /api/v1/apps/:appId/events/:id/withdraw" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/events/1/withdraw" '' "api" "true"

echo ""
echo "=== 7. 回调权限管理 (36-40) ==="
test_api 36 "GET /api/v1/apps/:appId/callbacks" "GET" "$API_SERVER/api/v1/apps/$TEST_APP_ID/callbacks" "" "api" "true"
test_api 37 "GET /api/v1/categories/:id/callbacks" "GET" "$API_SERVER/api/v1/categories/1/callbacks" "" "api" "true"
test_api 38 "POST /api/v1/apps/:appId/callbacks/subscribe" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/callbacks/subscribe" '{"permissionIds":["1"]}' "api" "true"
test_api 39 "PUT /api/v1/apps/:appId/callbacks/:id/config" "PUT" "$API_SERVER/api/v1/apps/$TEST_APP_ID/callbacks/1/config" '{"channelType":0,"channelAddress":"https://test.com/callback","authType":0}' "api" "true"
test_api 40 "POST /api/v1/apps/:appId/callbacks/:id/withdraw" "POST" "$API_SERVER/api/v1/apps/$TEST_APP_ID/callbacks/1/withdraw" '' "api" "true"

echo ""
echo "=== 8. 审批管理 (41-51) ==="
test_api 41 "GET /api/v1/approval-flows" "GET" "$API_SERVER/api/v1/approval-flows" "" "api" "true"
test_api 42 "GET /api/v1/approval-flows/:id" "GET" "$API_SERVER/api/v1/approval-flows/1" "" "api" "true"
test_api 43 "POST /api/v1/approval-flows" "POST" "$API_SERVER/api/v1/approval-flows" '{"nameCn":"测试审批流","nameEn":"Test Flow","code":"test_flow","nodes":[{"type":"approver","userId":"user001","order":1}]}' "api" "true"
test_api 44 "PUT /api/v1/approval-flows/:id" "PUT" "$API_SERVER/api/v1/approval-flows/1" '{"nameCn":"更新审批流"}' "api" "true"
test_api 45 "GET /api/v1/approvals/pending" "GET" "$API_SERVER/api/v1/approvals/pending" "" "api" "true"
test_api 46 "GET /api/v1/approvals/:id" "GET" "$API_SERVER/api/v1/approvals/1" "" "api" "true"
test_api 47 "POST /api/v1/approvals/:id/approve" "POST" "$API_SERVER/api/v1/approvals/1/approve" '{"comment":"同意"}' "api" "true"
test_api 48 "POST /api/v1/approvals/:id/reject" "POST" "$API_SERVER/api/v1/approvals/1/reject" '{"reason":"不符合要求"}' "api" "true"
test_api 49 "POST /api/v1/approvals/:id/cancel" "POST" "$API_SERVER/api/v1/approvals/1/cancel" '' "api" "true"
test_api 50 "POST /api/v1/approvals/batch-approve" "POST" "$API_SERVER/api/v1/approvals/batch-approve" '{"approvalIds":["1","2"],"comment":"批量通过"}' "api" "true"
test_api 51 "POST /api/v1/approvals/batch-reject" "POST" "$API_SERVER/api/v1/approvals/batch-reject" '{"approvalIds":["1","2"],"reason":"批量驳回"}' "api" "true"

echo ""
echo "=== 9. Scope 授权管理 (52-54) ==="
test_api 52 "GET /api/v1/user-authorizations" "GET" "$API_SERVER/api/v1/user-authorizations" "" "api" "true"
test_api 53 "POST /api/v1/user-authorizations" "POST" "$API_SERVER/api/v1/user-authorizations" '{"userId":"user001","appId":"10","scopes":["api:test:read"]}' "api" "true"
test_api 54 "DELETE /api/v1/user-authorizations/:id" "DELETE" "$API_SERVER/api/v1/user-authorizations/1" '' "api" "true"

echo ""
echo "=== 10. 消费网关 (55-58) ==="
test_api 55 "GET /gateway/api/*" "GET" "$OPEN_SERVER/gateway/api/v1/test" "" "open" "true"
test_api 56 "POST /gateway/events/publish" "POST" "$EVENT_SERVER/gateway/events/publish" '{"topic":"test.event","payload":{"data":"test"}}' "event" "true"
test_api 57 "POST /gateway/callbacks/invoke" "POST" "$EVENT_SERVER/gateway/callbacks/invoke" '{"callbackScope":"callback:test:completed","payload":{"data":"test"}}' "event" "true"
test_api 58 "GET /gateway/permissions/check" "GET" "$EVENT_SERVER/gateway/permissions/check?appId=10&scope=api:test:read" "" "event" "true"

echo ""
echo "========================================"
echo "验证完成"
echo "========================================"
echo ""
echo "统计结果："
echo "  总接口数: $TOTAL"
echo "  ✅ 通过: $PASSED"
echo "  ⚠️ 需要数据: $NEED_DATA"
echo "  ❌ 失败: $FAILED"
echo ""

# 计算通过率
PASS_RATE=$(echo "scale=2; $PASSED * 100 / $TOTAL" | bc)
echo "通过率: ${PASS_RATE}%"
echo ""

# 输出详细失败信息
if [[ $FAILED -gt 0 ]]; then
    echo "失败详情:"
    cat /tmp/api_test_details.txt
fi
