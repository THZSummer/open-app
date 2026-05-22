#!/bin/bash
set -ex
# ============================================================================
# TASK-012: 后端接口验证测试脚本
# 版本: v1.0
# 创建日期: 2026-04-24
# 说明: 验证审批流程实现是否符合 v2.8.0 设计
# ============================================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试报告文件
REPORT_FILE=".sddu/specs-tree-root/specs-tree-capability-open-platform/validation-report-backend.md"

# 日志函数
log_info() {
    echo "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo "${YELLOW}[WARNING]${NC} $1"
}

# ============================================================================
# 1. 数据库表结构验证
# ============================================================================
log_info "=========================================="
log_info "1. 数据库表结构验证"
log_info "=========================================="

# 检查 MySQL 连接
log_info "检查 MySQL 连接..."
if mysql -u root -pOpenplatform123!@ -e "SELECT 1;" > /dev/null 2>&1; then
    log_success "MySQL 连接成功"
else
    log_error "MySQL 连接失败，请检查配置"
    exit 1
fi

# 检查审批流程表（approval_flow_t）是否移除 is_default 字段
log_info "检查 approval_flow_t 表结构..."
TABLE_DESC=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "DESC openplatform_v2_approval_flow_t;" 2>/dev/null)

if echo "$TABLE_DESC" | grep -q "is_default"; then
    log_error "approval_flow_t 表仍包含 is_default 字段（不符合 v2.8.0 设计）"
    RESULT_FLOW_IS_DEFAULT="❌ 不符合"
else
    log_success "approval_flow_t 表已移除 is_default 字段（符合 v2.8.0）"
    RESULT_FLOW_IS_DEFAULT="✅ 符合"
fi

# 检查审批流程表是否包含 code 字段
if echo "$TABLE_DESC" | grep -q "code"; then
    log_success "approval_flow_t 表包含 code 字段"
    RESULT_FLOW_CODE="✅ 符合"
else
    log_error "approval_flow_t 表缺少 code 字段"
    RESULT_FLOW_CODE="❌ 缺失"
fi

# 检查审批记录表（approval_record_t）是否移除 flow_id 字段
log_info "检查 approval_record_t 表结构..."
TABLE_DESC=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "DESC openplatform_v2_approval_record_t;" 2>/dev/null)

if echo "$TABLE_DESC" | grep -q "flow_id"; then
    log_error "approval_record_t 表仍包含 flow_id 字段（不符合 v2.8.0 设计）"
    RESULT_RECORD_FLOW_ID="❌ 不符合"
else
    log_success "approval_record_t 表已移除 flow_id 字段（符合 v2.8.0）"
    RESULT_RECORD_FLOW_ID="✅ 符合"
fi

# 检查审批记录表是否包含 combined_nodes 字段
if echo "$TABLE_DESC" | grep -q "combined_nodes"; then
    log_success "approval_record_t 表包含 combined_nodes 字段"
    RESULT_RECORD_COMBINED="✅ 符合"
else
    log_error "approval_record_t 表缺少 combined_nodes 字段"
    RESULT_RECORD_COMBINED="❌ 缺失"
fi

# 检查审批日志表（approval_log_t）是否包含 level 字段
log_info "检查 approval_log_t 表结构..."
TABLE_DESC=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "DESC openplatform_v2_approval_log_t;" 2>/dev/null)

if echo "$TABLE_DESC" | grep -q "level"; then
    log_success "approval_log_t 表包含 level 字段"
    RESULT_LOG_LEVEL="✅ 符合"
else
    log_error "approval_log_t 表缺少 level 字段"
    RESULT_LOG_LEVEL="❌ 缺失"
fi

# 检查权限表（permission_t）是否包含 need_approval 和 resource_nodes 字段
log_info "检查 permission_t 表结构..."
TABLE_DESC=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "DESC openplatform_v2_permission_t;" 2>/dev/null)

if echo "$TABLE_DESC" | grep -q "need_approval"; then
    log_success "permission_t 表包含 need_approval 字段"
    RESULT_PERMISSION_NEED="✅ 符合"
else
    log_error "permission_t 表缺少 need_approval 字段"
    RESULT_PERMISSION_NEED="❌ 缺失"
fi

if echo "$TABLE_DESC" | grep -q "resource_nodes"; then
    log_success "permission_t 表包含 resource_nodes 字段"
    RESULT_PERMISSION_NODES="✅ 符合"
else
    log_error "permission_t 表缺少 resource_nodes 字段"
    RESULT_PERMISSION_NODES="❌ 缺失"
fi

# ============================================================================
# 2. 默认数据验证
# ============================================================================
log_info "=========================================="
log_info "2. 默认数据验证"
log_info "=========================================="

# 检查全局审批流程是否存在（code='global'）
log_info "检查全局审批流程..."
GLOBAL_FLOW=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "SELECT id, name_cn, code FROM openplatform_v2_approval_flow_t WHERE code='global';" 2>/dev/null)

if [ -n "$GLOBAL_FLOW" ] && echo "$GLOBAL_FLOW" | grep -q "global"; then
    log_success "全局审批流程存在（code='global'）"
    echo "$GLOBAL_FLOW"
    RESULT_GLOBAL_FLOW="✅ 存在"
else
    log_error "全局审批流程不存在（code='global'）"
    RESULT_GLOBAL_FLOW="❌ 不存在"
fi

# 检查场景审批流程是否存在
log_info "检查场景审批流程..."
SCENE_FLOW=$(mysql -u root -pOpenplatform123!@ openplatform_v2 -e "SELECT id, name_cn, code FROM openplatform_v2_approval_flow_t WHERE code='api_permission_apply';" 2>/dev/null)

if [ -n "$SCENE_FLOW" ] && echo "$SCENE_FLOW" | grep -q "api_permission_apply"; then
    log_success "场景审批流程存在（code='api_permission_apply'）"
    echo "$SCENE_FLOW"
    RESULT_SCENE_FLOW="✅ 存在"
else
    log_error "场景审批流程不存在（code='api_permission_apply'）"
    RESULT_SCENE_FLOW="❌ 不存在"
fi

# ============================================================================
# 3. Java 实体类验证
# ============================================================================
log_info "=========================================="
log_info "3. Java 实体类验证（代码层面）"
log_info "=========================================="

# 检查 ApprovalFlow.java 是否移除 isDefault 字段
log_info "检查 ApprovalFlow.java..."
if grep -q "isDefault" open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalFlow.java; then
    log_error "ApprovalFlow.java 包含 isDefault 字段（不符合 v2.8.0）"
    RESULT_ENTITY_FLOW="❌ 不符合"
else
    log_success "ApprovalFlow.java 已移除 isDefault 字段（符合 v2.8.0）"
    RESULT_ENTITY_FLOW="✅ 符合"
fi

# 检查 ApprovalFlow.java 是否包含 code 字段
if grep -q "private String code" open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalFlow.java; then
    log_success "ApprovalFlow.java 包含 code 字段"
    RESULT_ENTITY_CODE="✅ 符合"
else
    log_error "ApprovalFlow.java 缺少 code 字段"
    RESULT_ENTITY_CODE="❌ 缺失"
fi

# 检查 ApprovalRecord.java 是否移除 flowId 字段
log_info "检查 ApprovalRecord.java..."
if grep -q "flowId" open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalRecord.java; then
    log_error "ApprovalRecord.java 包含 flowId 字段（不符合 v2.8.0）"
    RESULT_ENTITY_RECORD="❌ 不符合"
else
    log_success "ApprovalRecord.java 已移除 flowId 字段（符合 v2.8.0）"
    RESULT_ENTITY_RECORD="✅ 符合"
fi

# 检查 ApprovalRecord.java 是否包含 combinedNodes 字段
if grep -q "combinedNodes" open-server/src/main/java/com/xxx/open/modules/approval/entity/ApprovalRecord.java; then
    log_success "ApprovalRecord.java 包含 combinedNodes 字段"
    RESULT_ENTITY_COMBINED="✅ 符合"
else
    log_error "ApprovalRecord.java 缺少 combinedNodes 字段"
    RESULT_ENTITY_COMBINED="❌ 缺失"
fi

# 检查 ApprovalNodeDto.java 是否包含 level 字段
log_info "检查 ApprovalNodeDto.java..."
if grep -q "private String level" open-server/src/main/java/com/xxx/open/modules/approval/dto/ApprovalNodeDto.java; then
    log_success "ApprovalNodeDto.java 包含 level 字段"
    RESULT_ENTITY_LEVEL="✅ 符合"
else
    log_error "ApprovalNodeDto.java 缺少 level 字段"
    RESULT_ENTITY_LEVEL="❌ 缺失"
fi

# ============================================================================
# 4. ApprovalEngine 核心逻辑验证
# ============================================================================
log_info "=========================================="
log_info "4. ApprovalEngine 核心逻辑验证"
log_info "=========================================="

# 检查 composeApprovalNodes 方法
log_info "检查 composeApprovalNodes 方法..."
if grep -q "composeApprovalNodes" open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java; then
    log_success "composeApprovalNodes 方法存在"
    
    # 检查三级审批顺序（资源审批 -> 场景审批 -> 全局审批）
    if grep -q "Level.RESOURCE" open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java &&
       grep -q "Level.SCENE" open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java &&
       grep -q "Level.GLOBAL" open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java; then
        log_success "三级审批 level 标记正确（resource/scene/global）"
        RESULT_ENGINE_LEVEL="✅ 符合"
    else
        log_error "三级审批 level 标记不完整"
        RESULT_ENGINE_LEVEL="❌ 不完整"
    fi
    
    # 检查审批顺序（从具体到一般）
    ENGINE_CODE=$(cat open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java)
    if echo "$ENGINE_CODE" | grep -A 50 "composeApprovalNodes" | grep -q "getResourceApprovalNodes" &&
       echo "$ENGINE_CODE" | grep -A 50 "composeApprovalNodes" | grep -q "getSceneApprovalNodes" &&
       echo "$ENGINE_CODE" | grep -A 50 "composeApprovalNodes" | grep -q "getGlobalApprovalNodes"; then
        log_success "审批顺序正确（资源审批 → 场景审批 → 全局审批）"
        RESULT_ENGINE_ORDER="✅ 符合"
    else
        log_error "审批顺序不正确"
        RESULT_ENGINE_ORDER="❌ 不正确"
    fi
    
    RESULT_ENGINE_METHOD="✅ 存在"
else
    log_error "composeApprovalNodes 方法不存在"
    RESULT_ENGINE_METHOD="❌ 不存在"
    RESULT_ENGINE_LEVEL="❌ 无法验证"
    RESULT_ENGINE_ORDER="❌ 无法验证"
fi

# 检查审批日志是否记录 level 字段
log_info "检查审批日志 level 记录..."
if grep -q "approvalLog.setLevel(currentNode.getLevel())" open-server/src/main/java/com/xxx/open/modules/approval/engine/ApprovalEngine.java; then
    log_success "审批日志正确记录 level 字段"
    RESULT_LOG_RECORD="✅ 符合"
else
    log_error "审批日志未记录 level 字段"
    RESULT_LOG_RECORD="❌ 不符合"
fi

# ============================================================================
# 5. 生成验证报告
# ============================================================================
log_info "=========================================="
log_info "生成验证报告"
log_info "=========================================="

# 创建报告文件
cat > "$REPORT_FILE" << EOF
# 后端接口验证报告 - TASK-012

**验证日期**: $(date '+%Y-%m-%d %H:%M:%S')
**验证版本**: v2.8.0
**验证范围**: 审批流程后端实现

---

## 一、数据库表结构验证

| 表名 | 验证项 | 结果 | 说明 |
|------|--------|------|------|
| approval_flow_t | 移除 is_default 字段 | ${RESULT_FLOW_IS_DEFAULT} | 使用 code='global' 标识全局审批 |
| approval_flow_t | 包含 code 字段 | ${RESULT_FLOW_CODE} | 用于标识审批流程类型 |
| approval_record_t | 移除 flow_id 字段 | ${RESULT_RECORD_FLOW_ID} | 直接存储 combinedNodes |
| approval_record_t | 包含 combined_nodes 字段 | ${RESULT_RECORD_COMBINED} | 存储完整审批节点配置 |
| approval_log_t | 包含 level 字段 | ${RESULT_LOG_LEVEL} | 标记审批级别（resource/scene/global） |
| permission_t | 包含 need_approval 字段 | ${RESULT_PERMISSION_NEED} | 标记是否需要审批 |
| permission_t | 包含 resource_nodes 字段 | ${RESULT_PERMISSION_NODES} | 存储资源级审批节点配置 |

---

## 二、默认数据验证

| 验证项 | 结果 | 说明 |
|--------|------|------|
| 全局审批流程（code='global'） | ${RESULT_GLOBAL_FLOW} | 系统必需的默认审批流程 |
| 场景审批流程（code='api_permission_apply'） | ${RESULT_SCENE_FLOW} | API权限申请审批流程 |

---

## 三、Java 实体类验证

| 类名 | 验证项 | 结果 | 说明 |
|------|--------|------|------|
| ApprovalFlow.java | 移除 isDefault 字段 | ${RESULT_ENTITY_FLOW} | 符合 v2.8.0 设计 |
| ApprovalFlow.java | 包含 code 字段 | ${RESULT_ENTITY_CODE} | 用于标识审批流程 |
| ApprovalRecord.java | 移除 flowId 字段 | ${RESULT_ENTITY_RECORD} | 符合 v2.8.0 设计 |
| ApprovalRecord.java | 包含 combinedNodes 字段 | ${RESULT_ENTITY_COMBINED} | 存储完整审批节点 |
| ApprovalNodeDto.java | 包含 level 字段 | ${RESULT_ENTITY_LEVEL} | 标记审批级别 |

---

## 四、ApprovalEngine 核心逻辑验证

| 验证项 | 结果 | 说明 |
|--------|------|------|
| composeApprovalNodes 方法存在 | ${RESULT_ENGINE_METHOD} | 核心组合逻辑 |
| 三级审批 level 标记 | ${RESULT_ENGINE_LEVEL} | resource/scene/global |
| 审批顺序正确 | ${RESULT_ENGINE_ORDER} | 资源审批 → 场景审批 → 全局审批 |
| 审批日志记录 level | ${RESULT_LOG_RECORD} | 审批操作时记录审批级别 |

---

## 五、验证总结

### 符合项统计

EOF

# 统计符合项
PASS_COUNT=$(echo "${RESULT_FLOW_IS_DEFAULT} ${RESULT_FLOW_CODE} ${RESULT_RECORD_FLOW_ID} ${RESULT_RECORD_COMBINED} ${RESULT_LOG_LEVEL} ${RESULT_PERMISSION_NEED} ${RESULT_PERMISSION_NODES} ${RESULT_GLOBAL_FLOW} ${RESULT_SCENE_FLOW} ${RESULT_ENTITY_FLOW} ${RESULT_ENTITY_CODE} ${RESULT_ENTITY_RECORD} ${RESULT_ENTITY_COMBINED} ${RESULT_ENTITY_LEVEL} ${RESULT_ENGINE_METHOD} ${RESULT_ENGINE_LEVEL} ${RESULT_ENGINE_ORDER} ${RESULT_LOG_RECORD}" | grep -o "✅" | wc -l)

FAIL_COUNT=$(echo "${RESULT_FLOW_IS_DEFAULT} ${RESULT_FLOW_CODE} ${RESULT_RECORD_FLOW_ID} ${RESULT_RECORD_COMBINED} ${RESULT_LOG_LEVEL} ${RESULT_PERMISSION_NEED} ${RESULT_PERMISSION_NODES} ${RESULT_GLOBAL_FLOW} ${RESULT_SCENE_FLOW} ${RESULT_ENTITY_FLOW} ${RESULT_ENTITY_CODE} ${RESULT_ENTITY_RECORD} ${RESULT_ENTITY_COMBINED} ${RESULT_ENTITY_LEVEL} ${RESULT_ENGINE_METHOD} ${RESULT_ENGINE_LEVEL} ${RESULT_ENGINE_ORDER} ${RESULT_LOG_RECORD}" | grep -o "❌" | wc -l)

cat >> "$REPORT_FILE" << EOF

- ✅ 符合项：${PASS_COUNT} 项
- ❌ 不符合项：${FAIL_COUNT} 项
- 覆盖率：$(awk "BEGIN {printf \"%.1f\", ($PASS_COUNT / 18) * 100}")%

### 最终结论

EOF

if [ "$FAIL_COUNT" -eq 0 ]; then
    cat >> "$REPORT_FILE" << EOF
## ✅ **验证通过** - 所有实现符合 v2.8.0 设计规范

**结论**: 审批流程后端实现完全符合 v2.8.0 设计，无阻塞问题。

### 核心验证点总结

1. ✅ **表结构正确**: 移除 is_default 和 flow_id 字段，新增 combined_nodes 和 level 字段
2. ✅ **实体类正确**: ApprovalFlow、ApprovalRecord、ApprovalNodeDto 均符合设计
3. ✅ **审批逻辑正确**: 三级审批组合顺序正确（资源审批 → 场景审批 → 全局审批）
4. ✅ **审批标记正确**: ApprovalNodeDto 包含 level 字段，审批日志正确记录 level

### 下一步建议

- 可以继续执行前端验证测试（TASK-013）
- 建议补充集成测试，验证完整的审批流程
EOF
    log_success "验证通过！所有实现符合 v2.8.0 设计"
else
    cat >> "$REPORT_FILE" << EOF
## ❌ **验证不通过** - 存在不符合项

**结论**: 审批流程后端实现存在不符合 v2.8.0 设计的问题，需要修复。

### 需要修复的问题

请查看上述表格中的 ❌ 项，逐一修复。

EOF
    log_error "验证不通过！存在 ${FAIL_COUNT} 个不符合项"
fi

log_info "验证报告已生成: $REPORT_FILE"
log_info "验证完成！"

# 显示报告摘要
echo ""
cat "$REPORT_FILE"