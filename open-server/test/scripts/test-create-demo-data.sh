#!/bin/bash

# ============================================================================
# 综合测试数据生成脚本
# 功能：创建API、事件、回调注册数据，测试审批流程
# ============================================================================

BASE_URL="http://localhost:18080/open-server"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 随机时间戳（用于生成唯一测试数据）
TIMESTAMP=$(date +%s)

# 存储创建的资源ID
CATEGORY_ID=""
declare -a API_IDS
declare -a EVENT_IDS
declare -a CALLBACK_IDS
declare -a APPROVAL_IDS

# 统计数据
TOTAL_CREATED=0
TOTAL_FAILED=0

echo "========================================"
echo "综合测试数据生成"
echo "========================================"
echo ""

# ============================================================================
# 获取现有分类ID
# ============================================================================

get_existing_category() {
    echo -e "${YELLOW}【准备】查询现有分类...${NC}"
    
    local response=$(curl -s "$BASE_URL/api/v1/categories")
    CATEGORY_ID=$(echo "$response" | jq -r '.data[0].id // empty')
    
    if [ -z "$CATEGORY_ID" ] || [ "$CATEGORY_ID" = "null" ]; then
        echo -e "${RED}错误：未找到现有分类，请先创建分类${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ 使用分类ID: ${CATEGORY_ID}${NC}"
    echo ""
}

# ============================================================================
# 工具函数
# ============================================================================

# 打印分隔线
print_separator() {
    echo "----------------------------------------"
}

# 打印成功消息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 打印失败消息
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 打印信息
print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# 提取JSON中的ID
extract_id() {
    local json="$1"
    echo "$json" | jq -r '.data.id // empty'
}

# 检查命令是否成功
check_success() {
    local response="$1"
    local code=$(echo "$response" | jq -r '.code // empty')
    if [ "$code" = "200" ]; then
        return 0
    else
        return 1
    fi
}

# ============================================================================
# 步骤 1: 创建API注册数据
# ============================================================================

create_apis() {
    echo -e "${YELLOW}【步骤 1】创建API注册数据${NC}"
    print_separator
    
    if [ -z "$CATEGORY_ID" ]; then
        print_error "没有可用的分类ID，跳过API创建"
        return
    fi
    
    local category_id="$CATEGORY_ID"
    
    # API数据数组 (带审批配置)
    # 格式: nameCn|path|method|needApproval|description
    local apis=(
        "发送消息API|/api/v1/messages|POST|1|IM消息发送接口"
        "获取用户信息API|/api/v1/users/:id|GET|0|获取用户基本信息"
        "创建会议API|/api/v1/meetings|POST|1|创建即时会议或预约会议"
        "上传文件API|/api/v1/files/upload|POST|1|文件上传到云盘"
        "查询订单API|/api/v1/orders/:id|GET|0|查询订单详情"
        "支付API|/api/v1/payments|POST|1|在线支付接口"
        "发送邮件API|/api/v1/emails|POST|0|发送邮件通知"
        "查询日程API|/api/v1/schedules|GET|0|查询日程安排"
    )
    
    for api in "${apis[@]}"; do
        IFS='|' read -r nameCn path method needApproval description <<< "$api"
        
        print_info "创建API: $nameCn"
        
        # 根据needApproval配置resourceNodes
        local resource_nodes_json="[]"
        if [ "$needApproval" = "1" ]; then
            resource_nodes_json="[{\"type\":\"approver\",\"userId\":\"api_approver_${TIMESTAMP}\",\"userName\":\"API审批负责人\",\"order\":1}]"
        fi
        
        # 生成符合格式的scope (api:{module}:{identifier})
        # API scope格式: api:{小写字母数字}:{小写字母数字连字符}
        local scope_module="test"
        local scope_identifier="api$(printf '%05d' $RANDOM)"
        
        local response=$(curl -s -X POST "$BASE_URL/api/v1/apis" \
            -H "Content-Type: application/json" \
            -d "{
                \"nameCn\": \"$nameCn\",
                \"nameEn\": \"${nameCn} API\",
                \"path\": \"$path\",
                \"method\": \"$method\",
                \"categoryId\": \"$category_id\",
                \"permission\": {
                    \"nameCn\": \"${nameCn}权限\",
                    \"nameEn\": \"${nameCn} Permission\",
                    \"scope\": \"api:${scope_module}:${scope_identifier}\",
                    \"needApproval\": $needApproval,
                    \"resourceNodes\": \"$resource_nodes_json\"
                },
                \"properties\": [
                    {\"propertyName\": \"descriptionCn\", \"propertyValue\": \"$description\"},
                    {\"propertyName\": \"docUrl\", \"propertyValue\": \"https://docs.example.com/api/${TIMESTAMP}\"}
                ]
            }")
        
        local api_id=$(extract_id "$response")
        
        if [ -n "$api_id" ] && check_success "$response"; then
            API_IDS+=("$api_id")
            print_success "API创建成功 (ID: $api_id, 需审批: $needApproval)"
            TOTAL_CREATED=$((TOTAL_CREATED + 1))
        else
            print_error "API创建失败: $nameCn"
            echo "$response" | jq .
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
        
        sleep 0.2
    done
    
    echo ""
    print_info "已创建 ${#API_IDS[@]} 个API"
    echo ""
}

# ============================================================================
# 步骤 2: 创建事件注册数据
# ============================================================================

create_events() {
    echo -e "${YELLOW}【步骤 2】创建事件注册数据${NC}"
    print_separator
    
    if [ -z "$CATEGORY_ID" ]; then
        print_error "没有可用的分类ID，跳过事件创建"
        return
    fi
    
    local category_id="$CATEGORY_ID"
    
    # 事件数据数组
    # 格式: nameCn|needApproval|description
    local events=(
        "用户登录事件|1|用户成功登录系统时触发"
        "消息接收事件|1|接收到新消息时触发"
        "会议开始事件|1|会议开始时触发"
        "文件上传完成事件|0|文件上传完成时触发"
        "订单创建事件|1|订单创建成功时触发"
        "支付完成事件|1|支付成功时触发"
        "审批完成事件|0|审批流程完成时触发"
    )
    
    for event in "${events[@]}"; do
        IFS='|' read -r nameCn needApproval description <<< "$event"
        
        print_info "创建事件: $nameCn"
        
        # 生成唯一的topic
        local topic="test.event.${TIMESTAMP}.${RANDOM}"
        
        # 根据needApproval配置resourceNodes
        local resource_nodes_json="[]"
        if [ "$needApproval" = "1" ]; then
            resource_nodes_json="[{\"type\":\"approver\",\"userId\":\"event_approver_${TIMESTAMP}\",\"userName\":\"事件审批负责人\",\"order\":1}]"
        fi
        
        # 生成符合格式的scope (event:{module}:{identifier})
        # Event scope格式: event:{小写字母}:{小写字母数字连字符}
        local scope_module="test"
        local scope_identifier="event$(printf '%05d' $RANDOM)"
        
        local response=$(curl -s -X POST "$BASE_URL/api/v1/events" \
            -H "Content-Type: application/json" \
            -d "{
                \"nameCn\": \"$nameCn\",
                \"nameEn\": \"${nameCn}\",
                \"topic\": \"$topic\",
                \"categoryId\": \"$category_id\",
                \"permission\": {
                    \"nameCn\": \"${nameCn}权限\",
                    \"nameEn\": \"${nameCn} Permission\",
                    \"scope\": \"event:${scope_module}:${scope_identifier}\",
                    \"needApproval\": $needApproval,
                    \"resourceNodes\": \"$resource_nodes_json\"
                },
                \"properties\": [
                    {\"propertyName\": \"descriptionCn\", \"propertyValue\": \"$description\"}
                ]
            }")
        
        local event_id=$(extract_id "$response")
        
        if [ -n "$event_id" ] && check_success "$response"; then
            EVENT_IDS+=("$event_id")
            print_success "事件创建成功 (ID: $event_id, 需审批: $needApproval)"
            TOTAL_CREATED=$((TOTAL_CREATED + 1))
        else
            print_error "事件创建失败: $nameCn"
            echo "$response" | jq .
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
        
        sleep 0.2
    done
    
    echo ""
    print_info "已创建 ${#EVENT_IDS[@]} 个事件"
    echo ""
}

# ============================================================================
# 步骤 3: 创建回调注册数据
# ============================================================================

create_callbacks() {
    echo -e "${YELLOW}【步骤 3】创建回调注册数据${NC}"
    print_separator
    
    if [ -z "$CATEGORY_ID" ]; then
        print_error "没有可用的分类ID，跳过回调创建"
        return
    fi
    
    local category_id="$CATEGORY_ID"
    
    # 回调数据数组
    # 格式: nameCn|needApproval|description
    local callbacks=(
        "审批完成回调|0|审批完成后通知第三方系统"
        "消息推送回调|1|消息推送结果通知"
        "订单状态变更回调|1|订单状态变更时通知"
        "支付结果通知回调|0|支付完成后通知商户"
    )
    
    for callback in "${callbacks[@]}"; do
        IFS='|' read -r nameCn needApproval description <<< "$callback"
        
        print_info "创建回调: $nameCn"
        
        # 生成唯一的URL
        local callback_url="https://example.com/callbacks/${TIMESTAMP}/${RANDOM}"
        
        # 根据needApproval配置resourceNodes
        local resource_nodes_json="[]"
        if [ "$needApproval" = "1" ]; then
            resource_nodes_json="[{\"type\":\"approver\",\"userId\":\"callback_approver_${TIMESTAMP}\",\"userName\":\"回调审批负责人\",\"order\":1}]"
        fi
        
        # 生成符合格式的scope (callback:{module}:{identifier})
        # scope格式要求: callback:{小写字母开头的module}:{小写字母开头的identifier}
        local scope_module="test"
        local scope_identifier="callback_${RANDOM}"
        
        local response=$(curl -s -X POST "$BASE_URL/api/v1/callbacks" \
            -H "Content-Type: application/json" \
            -d "{
                \"nameCn\": \"$nameCn\",
                \"nameEn\": \"${nameCn}\",
                \"url\": \"$callback_url\",
                \"categoryId\": \"$category_id\",
                \"permission\": {
                    \"nameCn\": \"${nameCn}权限\",
                    \"nameEn\": \"${nameCn} Permission\",
                    \"scope\": \"callback:${scope_module}:${scope_identifier}\",
                    \"needApproval\": $needApproval,
                    \"resourceNodes\": \"$resource_nodes_json\"
                },
                \"properties\": [
                    {\"propertyName\": \"descriptionCn\", \"propertyValue\": \"$description\"}
                ]
            }")
        
        local callback_id=$(extract_id "$response")
        
        if [ -n "$callback_id" ] && check_success "$response"; then
            CALLBACK_IDS+=("$callback_id")
            print_success "回调创建成功 (ID: $callback_id, 需审批: $needApproval)"
            TOTAL_CREATED=$((TOTAL_CREATED + 1))
        else
            print_error "回调创建失败: $nameCn"
            echo "$response" | jq .
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
        
        sleep 0.2
    done
    
    echo ""
    print_info "已创建 ${#CALLBACK_IDS[@]} 个回调"
    echo ""
}

# ============================================================================
# 步骤 4: 验证审批流程
# ============================================================================

verify_approval_flow() {
    echo -e "${YELLOW}【步骤 4】验证审批流程${NC}"
    print_separator
    
    # 查询审批记录列表
    print_info "查询审批记录列表"
    local response=$(curl -s -X GET "$BASE_URL/api/v1/approvals/pending?curPage=1&pageSize=20")
    
    if check_success "$response"; then
        local total=$(echo "$response" | jq -r '.data.page.total // 0')
        local records=$(echo "$response" | jq -r '.data.data | length')
        
        print_success "查询成功，共 $total 条审批记录"
        
        if [ "$records" -gt 0 ]; then
            echo ""
            print_info "审批记录列表："
            echo "$response" | jq -r '.data.data[] | "  - ID: \(.id) | 类型: \(.approvalType) | 状态: \(.status)"' | head -10
            
            # 提取审批ID用于测试
            local first_approval_id=$(echo "$response" | jq -r '.data.data[0].id // empty')
            if [ -n "$first_approval_id" ]; then
                APPROVAL_IDS+=("$first_approval_id")
            fi
        fi
    else
        print_error "查询审批记录失败"
        echo "$response" | jq .
    fi
    
    echo ""
    
    # 测试审批详情查询
    if [ ${#APPROVAL_IDS[@]} -gt 0 ]; then
        local approval_id="${APPROVAL_IDS[0]}"
        
        print_info "查询审批详情 (ID: $approval_id)"
        local detail_response=$(curl -s -X GET "$BASE_URL/api/v1/approvals/$approval_id")
        
        if check_success "$detail_response"; then
            print_success "审批详情查询成功"
            echo "$detail_response" | jq '.data | {id, approvalType, status, currentNode}'
        else
            print_error "审批详情查询失败"
            echo "$detail_response" | jq .
        fi
        
        echo ""
        
        # 测试审批操作（同意）
        print_info "测试审批操作 - 同意"
        local approve_response=$(curl -s -X POST "$BASE_URL/api/v1/approvals/$approval_id/approve" \
            -H "Content-Type: application/json" \
            -d '{"comment": "测试审批通过"}')
        
        if check_success "$approve_response"; then
            print_success "审批同意操作成功"
        else
            # 可能已经审批过了，不算失败
            local error_code=$(echo "$approve_response" | jq -r '.code')
            if [ "$error_code" = "400" ] || [ "$error_code" = "409" ]; then
                print_info "审批已处理或状态不允许操作"
            else
                print_error "审批同意操作失败"
                echo "$approve_response" | jq .
            fi
        fi
        
        echo ""
        
        # 测试批量审批
        print_info "测试批量审批操作"
        local batch_response=$(curl -s -X POST "$BASE_URL/api/v1/approvals/batch-approve" \
            -H "Content-Type: application/json" \
            -d "{\"approvalIds\": [\"$approval_id\"], \"comment\": \"批量审批测试\"}")
        
        if check_success "$batch_response"; then
            print_success "批量审批操作成功"
            echo "$batch_response" | jq '.data'
        else
            print_info "批量审批可能已处理过"
        fi
    else
        print_info "没有可用的审批记录进行测试"
    fi
    
    echo ""
}

# ============================================================================
# 步骤 5: 生成统计报告
# ============================================================================

generate_report() {
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}测试数据生成完成！${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo ""
    
    echo -e "${BLUE}📊 创建统计：${NC}"
    echo "  ✓ API数量:  ${#API_IDS[@]}"
    echo "  ✓ 事件数量: ${#EVENT_IDS[@]}"
    echo "  ✓ 回调数量: ${#CALLBACK_IDS[@]}"
    echo ""
    
    echo -e "${BLUE}📈 总体统计：${NC}"
    echo "  ✓ 成功创建: $TOTAL_CREATED"
    echo "  ✗ 创建失败: $TOTAL_FAILED"
    echo ""
    
    echo -e "${BLUE}📁 使用的分类ID：${NC}"
    echo "  - $CATEGORY_ID"
    echo ""
    
    if [ ${#API_IDS[@]} -gt 0 ]; then
        echo -e "${BLUE}🔌 API ID列表：${NC}"
        for id in "${API_IDS[@]}"; do
            echo "  - $id"
        done
        echo ""
    fi
    
    if [ ${#EVENT_IDS[@]} -gt 0 ]; then
        echo -e "${BLUE}📡 事件ID列表：${NC}"
        for id in "${EVENT_IDS[@]}"; do
            echo "  - $id"
        done
        echo ""
    fi
    
    if [ ${#CALLBACK_IDS[@]} -gt 0 ]; then
        echo -e "${BLUE}🔔 回调ID列表：${NC}"
        for id in "${CALLBACK_IDS[@]}"; do
            echo "  - $id"
        done
        echo ""
    fi
    
    echo -e "${GREEN}✅ 测试数据生成脚本执行完毕${NC}"
    echo ""
    echo -e "${YELLOW}💡 提示：${NC}"
    echo "  1. 可以使用以上ID进行后续测试"
    echo "  2. 查看审批记录：curl -s $BASE_URL/api/v1/approvals/pending | jq ."
    echo "  3. 查看API列表：curl -s $BASE_URL/api/v1/apis | jq ."
    echo "  4. 查看事件列表：curl -s $BASE_URL/api/v1/events | jq ."
    echo "  5. 查看回调列表：curl -s $BASE_URL/api/v1/callbacks | jq ."
    echo ""
}

# ============================================================================
# 主执行流程
# ============================================================================

main() {
    # 0. 获取现有分类ID
    get_existing_category
    
    # 1. 创建API
    create_apis
    
    # 2. 创建事件
    create_events
    
    # 3. 创建回调
    create_callbacks
    
    # 4. 验证审批流程
    verify_approval_flow
    
    # 5. 生成报告
    generate_report
}

# 执行主流程
main
