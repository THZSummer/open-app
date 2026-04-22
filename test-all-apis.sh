#!/bin/bash

# 能力开放平台接口验证脚本
# 测试全部 58 个接口

BASE_URL_OPEN="http://localhost:18080/open-server"
BASE_URL_API="http://localhost:18081"
BASE_URL_EVENT="http://localhost:18082"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
WARNING=0

# 测试函数
test_api() {
    local id=$1
    local name=$2
    local method=$3
    local url=$4
    local data=$5
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "测试 #$id $name ... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null)
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null)
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null)
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$url" 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    # 检查 HTTP 状态码
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        # 检查业务状态码
        code=$(echo "$body" | jq -r '.code' 2>/dev/null)
        if [ "$code" = "200" ] || [ "$code" = "null" ]; then
            echo -e "${GREEN}✅ 通过${NC}"
            PASSED=$((PASSED + 1))
        else
            echo -e "${YELLOW}⚠️ 业务错误 (code: $code)${NC}"
            WARNING=$((WARNING + 1))
        fi
    elif [ "$http_code" = "404" ]; then
        echo -e "${RED}❌ 接口不存在 (404)${NC}"
        FAILED=$((FAILED + 1))
    elif [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        echo -e "${YELLOW}⚠️ 认证/权限问题 ($http_code)${NC}"
        WARNING=$((WARNING + 1))
    elif [ "$http_code" = "000" ]; then
        echo -e "${RED}❌ 服务未响应${NC}"
        FAILED=$((FAILED + 1))
    else
        echo -e "${YELLOW}⚠️ HTTP $http_code${NC}"
        WARNING=$((WARNING + 1))
    fi
}

echo "========================================"
echo "能力开放平台接口验证"
echo "========================================"
echo ""

# ==================== 分类管理 (#1-8) ====================
echo "【分类管理】#1-8"
test_api 1 "获取分类列表" "GET" "$BASE_URL_OPEN/api/v1/categories"
test_api 2 "获取分类详情" "GET" "$BASE_URL_OPEN/api/v1/categories/305091739989835776"
test_api 3 "创建分类" "POST" "$BASE_URL_OPEN/api/v1/categories" '{"nameCn":"测试分类","nameEn":"Test Category","parentId":"","sortOrder":1}'
test_api 4 "更新分类" "PUT" "$BASE_URL_OPEN/api/v1/categories/305091739989835776" '{"nameCn":"更新分类","nameEn":"Updated Category","sortOrder":1}'
test_api 5 "删除分类" "DELETE" "$BASE_URL_OPEN/api/v1/categories/999999999"
test_api 6 "添加分类责任人" "POST" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/owners" '{"userId":"test_user"}'
test_api 7 "获取分类责任人列表" "GET" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/owners"
test_api 8 "移除分类责任人" "DELETE" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/owners/test_user"
echo ""

# ==================== API 管理 (#9-14) ====================
echo "【API 管理】#9-14"
test_api 9 "获取 API 列表" "GET" "$BASE_URL_OPEN/api/v1/apis"
test_api 10 "获取 API 详情" "GET" "$BASE_URL_OPEN/api/v1/apis/305096820525105152"
test_api 11 "注册 API" "POST" "$BASE_URL_OPEN/api/v1/apis" '{"nameCn":"测试API","nameEn":"Test API","path":"/test/api","method":"GET","categoryId":"305091739989835776","permission":{"scope":"api:test:api","needApproval":1}}'
test_api 12 "更新 API" "PUT" "$BASE_URL_OPEN/api/v1/apis/305096820525105152" '{"nameCn":"更新API","nameEn":"Updated API"}'
test_api 13 "删除 API" "DELETE" "$BASE_URL_OPEN/api/v1/apis/999999999"
test_api 14 "撤回 API" "POST" "$BASE_URL_OPEN/api/v1/apis/999999999/withdraw" '{}'
echo ""

# ==================== 事件管理 (#15-20) ====================
echo "【事件管理】#15-20"
test_api 15 "获取事件列表" "GET" "$BASE_URL_OPEN/api/v1/events"
test_api 16 "获取事件详情" "GET" "$BASE_URL_OPEN/api/v1/events/1"
test_api 17 "注册事件" "POST" "$BASE_URL_OPEN/api/v1/events" '{"nameCn":"测试事件","nameEn":"Test Event","topic":"test.event","categoryId":"305091739989835776","permission":{"scope":"event:test:event","needApproval":1}}'
test_api 18 "更新事件" "PUT" "$BASE_URL_OPEN/api/v1/events/1" '{"nameCn":"更新事件","nameEn":"Updated Event"}'
test_api 19 "删除事件" "DELETE" "$BASE_URL_OPEN/api/v1/events/999999999"
test_api 20 "撤回事件" "POST" "$BASE_URL_OPEN/api/v1/events/999999999/withdraw" '{}'
echo ""

# ==================== 回调管理 (#21-26) ====================
echo "【回调管理】#21-26"
test_api 21 "获取回调列表" "GET" "$BASE_URL_OPEN/api/v1/callbacks"
test_api 22 "获取回调详情" "GET" "$BASE_URL_OPEN/api/v1/callbacks/1"
test_api 23 "注册回调" "POST" "$BASE_URL_OPEN/api/v1/callbacks" '{"nameCn":"测试回调","nameEn":"Test Callback","categoryId":"305091739989835776","permission":{"scope":"callback:test:callback","needApproval":1}}'
test_api 24 "更新回调" "PUT" "$BASE_URL_OPEN/api/v1/callbacks/1" '{"nameCn":"更新回调","nameEn":"Updated Callback"}'
test_api 25 "删除回调" "DELETE" "$BASE_URL_OPEN/api/v1/callbacks/999999999"
test_api 26 "撤回回调" "POST" "$BASE_URL_OPEN/api/v1/callbacks/999999999/withdraw" '{}'
echo ""

# ==================== API 权限管理 (#27-30) ====================
echo "【API 权限管理】#27-30"
test_api 27 "获取应用 API 权限列表" "GET" "$BASE_URL_OPEN/api/v1/apps/test-app/apis"
test_api 28 "获取分类下 API 权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/apis"
test_api 29 "申请 API 权限" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/apis/subscribe" '{"permissionIds":["305096821477294080"]}'
test_api 30 "撤回 API 权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/apis/999999999/withdraw" '{}'
echo ""

# ==================== 事件权限管理 (#31-35) ====================
echo "【事件权限管理】#31-35"
test_api 31 "获取应用事件订阅列表" "GET" "$BASE_URL_OPEN/api/v1/apps/test-app/events"
test_api 32 "获取分类下事件权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/events"
test_api 33 "申请事件权限" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/events/subscribe" '{"permissionIds":["1"]}'
test_api 34 "配置事件消费参数" "PUT" "$BASE_URL_OPEN/api/v1/apps/test-app/events/999999999/config" '{"channelType":"webhook","channelAddress":"http://localhost/callback"}'
test_api 35 "撤回事件权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/events/999999999/withdraw" '{}'
echo ""

# ==================== 回调权限管理 (#36-40) ====================
echo "【回调权限管理】#36-40"
test_api 36 "获取应用回调订阅列表" "GET" "$BASE_URL_OPEN/api/v1/apps/test-app/callbacks"
test_api 37 "获取分类下回调权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/305091739989835776/callbacks"
test_api 38 "申请回调权限" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/callbacks/subscribe" '{"permissionIds":["1"]}'
test_api 39 "配置回调消费参数" "PUT" "$BASE_URL_OPEN/api/v1/apps/test-app/callbacks/999999999/config" '{"channelType":"webhook","channelAddress":"http://localhost/callback"}'
test_api 40 "撤回回调权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/test-app/callbacks/999999999/withdraw" '{}'
echo ""

# ==================== 审批管理 (#41-51) ====================
echo "【审批管理】#41-51"
test_api 41 "获取审批流程模板列表" "GET" "$BASE_URL_OPEN/api/v1/approval-flows"
test_api 42 "获取审批流程模板详情" "GET" "$BASE_URL_OPEN/api/v1/approval-flows/1"
test_api 43 "创建审批流程模板" "POST" "$BASE_URL_OPEN/api/v1/approval-flows" '{"code":"test-flow","nameCn":"测试流程","nameEn":"Test Flow","type":"api_subscribe","steps":[]}'
test_api 44 "更新审批流程模板" "PUT" "$BASE_URL_OPEN/api/v1/approval-flows/1" '{"nameCn":"更新流程","nameEn":"Updated Flow"}'
test_api 45 "获取待审批列表" "GET" "$BASE_URL_OPEN/api/v1/approvals/pending"
test_api 46 "获取审批详情" "GET" "$BASE_URL_OPEN/api/v1/approvals/1"
test_api 47 "同意审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/1/approve" '{"comment":"同意"}'
test_api 48 "驳回审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/1/reject" '{"comment":"驳回"}'
test_api 49 "撤销审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/1/cancel" '{}'
test_api 50 "批量同意审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/batch-approve" '{"approvalIds":[1,2,3]}'
test_api 51 "批量驳回审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/batch-reject" '{"approvalIds":[1,2,3]}'
echo ""

# ==================== Scope 授权管理 (#52-54) ====================
echo "【Scope 授权管理】#52-54"
test_api 52 "获取用户授权列表" "GET" "$BASE_URL_API/api/v1/user-authorizations"
test_api 53 "用户授权" "POST" "$BASE_URL_API/api/v1/user-authorizations" '{"userId":"test-user","appId":"test-app","scope":"api:test:read"}'
test_api 54 "取消授权" "DELETE" "$BASE_URL_API/api/v1/user-authorizations/1"
echo ""

# ==================== 消费网关 (#55-58) ====================
echo "【消费网关】#55-58"
test_api 55 "API 请求代理与鉴权" "GET" "$BASE_URL_API/gateway/api/test/path"
test_api 56 "事件发布接口" "POST" "$BASE_URL_EVENT/gateway/events/publish" '{"topic":"test.event","payload":{}}'
test_api 57 "回调触发接口" "POST" "$BASE_URL_EVENT/gateway/callbacks/invoke" '{"callbackScope":"callback:test:callback","payload":{}}'
test_api 58 "权限校验接口" "GET" "$BASE_URL_API/gateway/permissions/check?appId=test-app&scope=api:test:read"
echo ""

# ==================== 统计结果 ====================
echo "========================================"
echo "验证统计"
echo "========================================"
echo -e "总计: $TOTAL 个接口"
echo -e "通过: ${GREEN}$PASSED${NC} 个"
echo -e "失败: ${RED}$FAILED${NC} 个"
echo -e "警告: ${YELLOW}$WARNING${NC} 个"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有接口均已实现！${NC}"
    exit 0
else
    echo -e "${RED}❌ 存在接口未实现或服务异常${NC}"
    exit 1
fi
