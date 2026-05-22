#!/bin/bash
set -ex
# API 测试汇总执行脚本
# 按模块顺序执行所有接口测试

# 加载配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# 统计变量
TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILED_TESTS=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印分隔线
print_separator() {
    echo "========================================================================"
    echo "$1"
    echo "========================================================================"
}

# 执行单个测试
run_test() {
    local script=$1
    local script_name=$(basename "$script")
    
    echo ""
    echo ">>> 执行: $script_name"
    echo "----------------------------------------"
    
    if bash "$script"; then
        echo -e "${GREEN}[成功]${NC} $script_name"
        ((SUCCESS_TESTS++))
    else
        echo -e "${RED}[失败]${NC} $script_name"
        ((FAILED_TESTS++))
    fi
    
    ((TOTAL_TESTS++))
    echo ""
}

# 执行模块测试
run_module() {
    local module_name=$1
    local module_dir=$2
    
    print_separator "$module_name"
    
    for script in "$SCRIPT_DIR/$module_dir"/*.sh; do
        if [ -f "$script" ]; then
            run_test "$script"
        fi
    done
}

# 主流程
echo "开始执行所有 API 测试..."
echo "BASE_URL: $BASE_URL"
echo ""

# 1. 分类管理
run_module "模块1: 分类管理 (category)" "category"

# 2. API管理
run_module "模块2: API管理 (api)" "api"

# 3. 事件管理
run_module "模块3: 事件管理 (event)" "event"

# 4. 回调管理
run_module "模块4: 回调管理 (callback)" "callback"

# 5. API权限管理
run_module "模块5: API权限管理 (api-permission)" "api-permission"

# 6. 事件权限管理
run_module "模块6: 事件权限管理 (event-permission)" "event-permission"

# 7. 回调权限管理
run_module "模块7: 回调权限管理 (callback-permission)" "callback-permission"

# 8. 审批管理
run_module "模块8: 审批管理 (approval)" "approval"

# 9. 用户授权管理
run_module "模块9: 用户授权管理 (user-authorization)" "user-authorization"

# 10. 消费网关
run_module "模块10: 消费网关 (gateway)" "gateway"

# 打印统计结果
print_separator "测试执行结果统计"
echo -e "总测试数: ${YELLOW}$TOTAL_TESTS${NC}"
echo -e "成功数: ${GREEN}$SUCCESS_TESTS${NC}"
echo -e "失败数: ${RED}$FAILED_TESTS${NC}"
echo ""
echo "执行完成！"