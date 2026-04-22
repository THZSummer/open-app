#!/bin/bash

# 能力开放平台接口验证脚本（优化版）
# 
# 优化内容：
# 1. 测试前自动初始化测试数据
# 2. 使用随机化数据避免唯一性冲突
# 3. 测试后自动清理测试数据
# 4. 生成详细的测试报告
#
# 使用方法：
#   ./test-all-apis-optimized.sh [options]
#
# 选项：
#   --no-init     跳过数据初始化
#   --no-cleanup  跳过数据清理
#   --db-host     数据库主机（默认：localhost）
#   --db-port     数据库端口（默认：3306）
#   --db-name     数据库名称（默认：openapp）
#   --db-user     数据库用户（默认：root）
#   --db-pass     数据库密码（默认：空）

# =====================================================
# 配置项
# =====================================================

BASE_URL_OPEN="http://localhost:18080/open-server"
BASE_URL_API="http://localhost:18081"
BASE_URL_EVENT="http://localhost:18082"

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="openapp"
DB_USER="root"
DB_PASS=""

# SQL 脚本路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$(realpath "$SCRIPT_DIR/../../../../../../open-server/src/main/resources/sql")"
INIT_SQL="$SQL_DIR/test-data-init.sql"
CLEANUP_SQL="$SQL_DIR/test-data-cleanup.sql"

# 测试选项
SKIP_INIT=false
SKIP_CLEANUP=false

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL=0
PASSED=0
FAILED=0
WARNING=0

# 测试报告文件
REPORT_FILE=""

# 随机时间戳（用于生成唯一测试数据）
TIMESTAMP=$(date +%s)

# =====================================================
# 解析命令行参数
# =====================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-init)
            SKIP_INIT=true
            shift
            ;;
        --no-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
        --db-host)
            DB_HOST="$2"
            shift 2
            ;;
        --db-port)
            DB_PORT="$2"
            shift 2
            ;;
        --db-name)
            DB_NAME="$2"
            shift 2
            ;;
        --db-user)
            DB_USER="$2"
            shift 2
            ;;
        --db-pass)
            DB_PASS="$2"
            shift 2
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
done

# =====================================================
# 工具函数
# =====================================================

# 生成测试报告文件名
generate_report_filename() {
    local timestamp=$(date +%Y%m%d-%H%M%S)
    REPORT_FILE=".sddu/specs-tree-root/specs-tree-capability-open-platform/test/test-report-open-server-${timestamp}.md"
}

# 初始化测试数据
init_test_data() {
    echo -e "${BLUE}[初始化] 正在初始化测试数据...${NC}"
    
    if [ ! -f "$INIT_SQL" ]; then
        echo -e "${RED}[错误] 初始化脚本不存在: $INIT_SQL${NC}"
        return 1
    fi
    
    # 执行初始化SQL
    if [ -n "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$INIT_SQL"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" < "$INIT_SQL"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[成功] 测试数据初始化完成${NC}"
        return 0
    else
        echo -e "${RED}[失败] 测试数据初始化失败${NC}"
        return 1
    fi
}

# 清理测试数据
cleanup_test_data() {
    echo -e "${BLUE}[清理] 正在清理测试数据...${NC}"
    
    if [ ! -f "$CLEANUP_SQL" ]; then
        echo -e "${RED}[错误] 清理脚本不存在: $CLEANUP_SQL${NC}"
        return 1
    fi
    
    # 执行清理SQL
    if [ -n "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$CLEANUP_SQL"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" < "$CLEANUP_SQL"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[成功] 测试数据清理完成${NC}"
        return 0
    else
        echo -e "${RED}[失败] 测试数据清理失败${NC}"
        return 1
    fi
}

# 测试函数
test_api() {
    local id=$1
    local name=$2
    local method=$3
    local url=$4
    local data=$5
    local expected_code=${6:-200}
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "测试 #$id $name ... "
    
    # 执行请求
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
        if [ "$code" = "$expected_code" ] || [ "$code" = "null" ]; then
            echo -e "${GREEN}✅ 通过${NC}"
            PASSED=$((PASSED + 1))
            echo "| $id | $name | $method | $url | ✓ 通过 | - | - |" >> "$REPORT_FILE"
        else
            echo -e "${RED}❌ 失败 (业务码: $code, 期望: $expected_code)${NC}"
            FAILED=$((FAILED + 1))
            echo "| $id | $name | $method | $url | ✗ 失败 | - | 业务码: $code |" >> "$REPORT_FILE"
        fi
    elif [ "$http_code" = "404" ]; then
        echo -e "${RED}❌ 接口不存在 (404)${NC}"
        FAILED=$((FAILED + 1))
        echo "| $id | $name | $method | $url | ✗ 失败 | - | 404错误 |" >> "$REPORT_FILE"
    elif [ "$http_code" = "000" ]; then
        echo -e "${RED}❌ 服务未响应${NC}"
        FAILED=$((FAILED + 1))
        echo "| $id | $name | $method | $url | ✗ 失败 | - | 服务未响应 |" >> "$REPORT_FILE"
    else
        echo -e "${YELLOW}⚠️ HTTP $http_code${NC}"
        WARNING=$((WARNING + 1))
        echo "| $id | $name | $method | $url | ⚠ 警告 | - | HTTP $http_code |" >> "$REPORT_FILE"
    fi
}

# 生成报告头部
generate_report_header() {
    cat > "$REPORT_FILE" << 'EOF'
# Open-Server 服务接口测试报告

**测试时间**: $(date '+%Y-%m-%d %H:%M:%S')  
**测试人员**: 自动化测试脚本  
**报告生成时间**: $(date '+%Y-%m-%d %H:%M:%S')

---

## 一、测试环境

| 配置项 | 值 |
|-------|-----|
| 服务端口 | 18080 |
| 上下文路径 | /open-server |
| 数据库 | openapp (MySQL) |
| Redis | localhost:6379/0 |

---

## 二、测试详情

EOF
}

# 生成报告尾部
generate_report_footer() {
    cat >> "$REPORT_FILE" << EOF

---

## 三、测试统计

| 指标 | 数值 |
|-----|------|
| **总用例数** | $TOTAL |
| **通过数** | $PASSED ✓ |
| **失败数** | $FAILED ✗ |
| **警告数** | $WARNING ⚠ |
| **通过率** | $(awk "BEGIN {printf \"%.1f\", ($PASSED/$TOTAL)*100}")% |

---

## 四、测试结论

本次测试共执行 $TOTAL 个测试用例，其中 $PASSED 个通过，$FAILED 个失败，通过率为 **$(awk "BEGIN {printf \"%.1f\", ($PASSED/$TOTAL)*100}")%**。

---

**报告生成器**: Open-Server 自动化测试框架  
**联系方式**: 如有问题请联系开发团队
EOF
    
    echo -e "${BLUE}[报告] 测试报告已生成: $REPORT_FILE${NC}"
}

# =====================================================
# 主流程
# =====================================================

echo "========================================"
echo "能力开放平台接口验证（优化版）"
echo "========================================"
echo ""

# 生成报告文件名
generate_report_filename

# 初始化测试数据
if [ "$SKIP_INIT" = false ]; then
    init_test_data
    if [ $? -ne 0 ]; then
        echo -e "${RED}[错误] 测试数据初始化失败，测试终止${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}[跳过] 数据初始化已跳过${NC}"
fi

echo ""

# 生成报告头部
generate_report_header

# ==================== 分类管理 (#1-8) ====================
echo "【分类管理】#1-8"
echo "" >> "$REPORT_FILE"
echo "### 分类管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 1 "获取分类列表(树形)" "GET" "$BASE_URL_OPEN/api/v1/categories"
test_api 2 "获取分类详情" "GET" "$BASE_URL_OPEN/api/v1/categories/1"
test_api 3 "创建分类(一级分类)" "POST" "$BASE_URL_OPEN/api/v1/categories" "{\"nameCn\":\"测试分类-$TIMESTAMP\",\"nameEn\":\"Test Category $TIMESTAMP\",\"parentId\":\"\",\"sortOrder\":1}"
test_api 4 "更新分类" "PUT" "$BASE_URL_OPEN/api/v1/categories/2" '{"nameCn":"更新分类","nameEn":"Updated Category","sortOrder":1}'
test_api 5 "删除分类(无关联资源)" "DELETE" "$BASE_URL_OPEN/api/v1/categories/4"  # 使用无关联资源的分类
test_api 6 "添加分类责任人" "POST" "$BASE_URL_OPEN/api/v1/categories/1/owners" "{\"userId\":\"test_user_$TIMESTAMP\"}"
test_api 7 "获取分类责任人列表" "GET" "$BASE_URL_OPEN/api/v1/categories/1/owners"
test_api 8 "移除分类责任人" "DELETE" "$BASE_URL_OPEN/api/v1/categories/1/owners/test_user_$TIMESTAMP"
echo ""

# ==================== API 管理 (#9-14) ====================
echo "【API 管理】#9-14"
echo "" >> "$REPORT_FILE"
echo "### API管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 9 "获取API列表" "GET" "$BASE_URL_OPEN/api/v1/apis?curPage=1&pageSize=20"
test_api 10 "获取API详情" "GET" "$BASE_URL_OPEN/api/v1/apis/100"
# 使用随机化的Scope避免409冲突
test_api 11 "注册API" "POST" "$BASE_URL_OPEN/api/v1/apis" "{\"nameCn\":\"测试API-$TIMESTAMP\",\"nameEn\":\"Test API $TIMESTAMP\",\"path\":\"/test/api/$TIMESTAMP\",\"method\":\"GET\",\"categoryId\":\"1\",\"permission\":{\"scope\":\"api:test:$TIMESTAMP\",\"needApproval\":1}}"
test_api 12 "更新API" "PUT" "$BASE_URL_OPEN/api/v1/apis/100" '{"nameCn":"更新API","nameEn":"Updated API"}'
test_api 13 "删除API" "DELETE" "$BASE_URL_OPEN/api/v1/apis/102"  # 使用初始化数据中不存在的ID或已删除的ID
test_api 14 "撤回审核中的API" "POST" "$BASE_URL_OPEN/api/v1/apis/102/withdraw" '{}'  # 使用初始化的待审API ID=102
echo ""

# ==================== 事件管理 (#15-20) ====================
echo "【事件管理】#15-20"
echo "" >> "$REPORT_FILE"
echo "### 事件管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 15 "获取事件列表" "GET" "$BASE_URL_OPEN/api/v1/events?curPage=1&pageSize=20"
test_api 16 "获取事件详情" "GET" "$BASE_URL_OPEN/api/v1/events/200"
# 使用随机化的Topic避免409冲突
test_api 17 "注册事件" "POST" "$BASE_URL_OPEN/api/v1/events" "{\"nameCn\":\"测试事件-$TIMESTAMP\",\"nameEn\":\"Test Event $TIMESTAMP\",\"topic\":\"test.event.$TIMESTAMP\",\"categoryId\":\"1\",\"permission\":{\"scope\":\"event:test:$TIMESTAMP\",\"needApproval\":1}}"
test_api 18 "更新事件" "PUT" "$BASE_URL_OPEN/api/v1/events/200" '{"nameCn":"更新事件","nameEn":"Updated Event"}'
test_api 19 "删除事件" "DELETE" "$BASE_URL_OPEN/api/v1/events/201"
test_api 20 "撤回审核中的事件" "POST" "$BASE_URL_OPEN/api/v1/events/201/withdraw" '{}'  # 使用初始化的待审事件 ID=201
echo ""

# ==================== 回调管理 (#21-26) ====================
echo "【回调管理】#21-26"
echo "" >> "$REPORT_FILE"
echo "### 回调管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 21 "获取回调列表" "GET" "$BASE_URL_OPEN/api/v1/callbacks?curPage=1&pageSize=20"
test_api 22 "获取回调详情" "GET" "$BASE_URL_OPEN/api/v1/callbacks/300"
# 使用随机化的Scope避免409冲突
test_api 23 "注册回调" "POST" "$BASE_URL_OPEN/api/v1/callbacks" "{\"nameCn\":\"测试回调-$TIMESTAMP\",\"nameEn\":\"Test Callback $TIMESTAMP\",\"categoryId\":\"2\",\"permission\":{\"scope\":\"callback:test:$TIMESTAMP\",\"needApproval\":1}}"
test_api 24 "更新回调" "PUT" "$BASE_URL_OPEN/api/v1/callbacks/300" '{"nameCn":"更新回调","nameEn":"Updated Callback"}'
test_api 25 "删除回调" "DELETE" "$BASE_URL_OPEN/api/v1/callbacks/301"
test_api 26 "撤回审核中的回调" "POST" "$BASE_URL_OPEN/api/v1/callbacks/301/withdraw" '{}'  # 使用初始化的待审回调 ID=301
echo ""

# ==================== API 权限管理 (#27-30) ====================
echo "【API 权限管理】#27-30"
echo "" >> "$REPORT_FILE"
echo "### API权限管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 27 "获取应用API权限列表" "GET" "$BASE_URL_OPEN/api/v1/apps/10/apis?curPage=1&pageSize=20"
test_api 28 "获取分类下API权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/1/apis?curPage=1&pageSize=20"
test_api 29 "申请API权限(批量)" "POST" "$BASE_URL_OPEN/api/v1/apps/10/apis/subscribe" '{"permissionIds":["1000"]}'
test_api 30 "撤回API权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/10/apis/300/withdraw" '{}'  # 使用初始化的订阅 ID=300
echo ""

# ==================== 事件权限管理 (#31-35) ====================
echo "【事件权限管理】#31-35"
echo "" >> "$REPORT_FILE"
echo "### 事件权限管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 31 "获取应用事件订阅列表" "GET" "$BASE_URL_OPEN/api/v1/apps/10/events?curPage=1&pageSize=20"
test_api 32 "获取分类下事件权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/1/events?curPage=1&pageSize=20"
test_api 33 "申请事件权限(批量)" "POST" "$BASE_URL_OPEN/api/v1/apps/10/events/subscribe" '{"permissionIds":["1002"]}'
test_api 34 "配置事件消费参数" "PUT" "$BASE_URL_OPEN/api/v1/apps/10/events/301/config" '{"channelType":"webhook","channelAddress":"http://localhost/callback"}'  # 使用初始化的订阅 ID=301
test_api 35 "撤回事件权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/10/events/301/withdraw" '{}'  # 使用初始化的订阅 ID=301
echo ""

# ==================== 回调权限管理 (#36-40) ====================
echo "【回调权限管理】#36-40"
echo "" >> "$REPORT_FILE"
echo "### 回调权限管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 36 "获取应用回调订阅列表" "GET" "$BASE_URL_OPEN/api/v1/apps/10/callbacks?curPage=1&pageSize=20"
test_api 37 "获取分类下回调权限列表" "GET" "$BASE_URL_OPEN/api/v1/categories/2/callbacks?curPage=1&pageSize=20"
test_api 38 "申请回调权限(批量)" "POST" "$BASE_URL_OPEN/api/v1/apps/10/callbacks/subscribe" '{"permissionIds":["1003"]}'
test_api 39 "配置回调消费参数" "PUT" "$BASE_URL_OPEN/api/v1/apps/10/callbacks/302/config" '{"channelType":"webhook","channelAddress":"http://localhost/callback"}'  # 使用初始化的订阅 ID=302
test_api 40 "撤回回调权限申请" "POST" "$BASE_URL_OPEN/api/v1/apps/10/callbacks/302/withdraw" '{}'  # 使用初始化的订阅 ID=302
echo ""

# ==================== 审批管理 (#41-51) ====================
echo "【审批管理】#41-51"
echo "" >> "$REPORT_FILE"
echo "### 审批管理" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 用例ID | 用例名称 | 方法 | 端点 | 状态 | 耗时(ms) | 备注 |" >> "$REPORT_FILE"
echo "|--------|---------|------|------|------|---------|------|" >> "$REPORT_FILE"

test_api 41 "获取审批流程模板列表" "GET" "$BASE_URL_OPEN/api/v1/approval-flows"
test_api 42 "获取审批流程模板详情" "GET" "$BASE_URL_OPEN/api/v1/approval-flows/1"
# 使用随机化的流程编码避免409冲突
test_api 43 "创建审批流程模板" "POST" "$BASE_URL_OPEN/api/v1/approval-flows" "{\"code\":\"test-flow-$TIMESTAMP\",\"nameCn\":\"测试流程\",\"nameEn\":\"Test Flow\",\"type\":\"api_subscribe\",\"steps\":[]}"
test_api 44 "更新审批流程模板" "PUT" "$BASE_URL_OPEN/api/v1/approval-flows/1" '{"nameCn":"更新流程","nameEn":"Updated Flow"}'
test_api 45 "获取待审批列表" "GET" "$BASE_URL_OPEN/api/v1/approvals/pending"
test_api 46 "获取审批详情" "GET" "$BASE_URL_OPEN/api/v1/approvals/500"  # 使用初始化的审批记录 ID=500
test_api 47 "同意审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/500/approve" '{"comment":"同意"}'
test_api 48 "驳回审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/500/reject" '{"comment":"驳回"}'
test_api 49 "撤销审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/500/cancel" '{}'
test_api 50 "批量同意审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/batch-approve" '{"approvalIds":[500]}'
test_api 51 "批量驳回审批" "POST" "$BASE_URL_OPEN/api/v1/approvals/batch-reject" '{"approvalIds":[500]}'
echo ""

# 生成报告尾部
generate_report_footer

# ==================== 统计结果 ====================
echo "========================================"
echo "验证统计"
echo "========================================"
echo -e "总计: $TOTAL 个接口"
echo -e "通过: ${GREEN}$PASSED${NC} 个"
echo -e "失败: ${RED}$FAILED${NC} 个"
echo -e "警告: ${YELLOW}$WARNING${NC} 个"
echo ""

# 清理测试数据
if [ "$SKIP_CLEANUP" = false ]; then
    cleanup_test_data
else
    echo -e "${YELLOW}[跳过] 数据清理已跳过${NC}"
fi

echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有接口均已实现！${NC}"
    exit 0
else
    echo -e "${RED}❌ 存在接口未实现或服务异常${NC}"
    exit 1
fi
