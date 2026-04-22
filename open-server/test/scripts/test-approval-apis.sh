#!/bin/bash

# ============================================================================
# 审批管理模块接口测试脚本
# 功能：测试 TASK-009 实现的 11 个接口
# ============================================================================

BASE_URL="http://localhost:18080/open-server"
TOKEN="test-token"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "审批管理模块接口测试"
echo "========================================"
echo ""

# #41 GET /api/v1/approval-flows - 获取审批流程模板列表
echo -e "${YELLOW}测试 #41: 获取审批流程模板列表${NC}"
echo "请求: GET $BASE_URL/api/v1/approval-flows"
curl -s -X GET "$BASE_URL/api/v1/approval-flows" \
  -H "Content-Type: application/json" | jq .
echo ""
echo "----------------------------------------"

# #42 GET /api/v1/approval-flows/:id - 获取审批流程模板详情
echo -e "${YELLOW}测试 #42: 获取审批流程模板详情${NC}"
echo "请求: GET $BASE_URL/api/v1/approval-flows/1"
curl -s -X GET "$BASE_URL/api/v1/approval-flows/1" \
  -H "Content-Type: application/json" | jq .
echo ""
echo "----------------------------------------"

# #43 POST /api/v1/approval-flows - 创建审批流程模板
echo -e "${YELLOW}测试 #43: 创建审批流程模板${NC}"
echo "请求: POST $BASE_URL/api/v1/approval-flows"
curl -s -X POST "$BASE_URL/api/v1/approval-flows" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "API注册审批流",
    "nameEn": "API Registration Approval Flow",
    "code": "api_register",
    "isDefault": 0,
    "nodes": [
      {"type": "approver", "userId": "user001", "userName": "张三", "order": 1},
      {"type": "approver", "userId": "user002", "userName": "李四", "order": 2}
    ]
  }' | jq .
echo ""
echo "----------------------------------------"

# #44 PUT /api/v1/approval-flows/:id - 更新审批流程模板
echo -e "${YELLOW}测试 #44: 更新审批流程模板${NC}"
echo "请求: PUT $BASE_URL/api/v1/approval-flows/2"
curl -s -X PUT "$BASE_URL/api/v1/approval-flows/2" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "API注册审批流V2",
    "nameEn": "API Registration Approval Flow V2",
    "isDefault": 0,
    "nodes": [
      {"type": "approver", "userId": "user003", "userName": "王五", "order": 1}
    ]
  }' | jq .
echo ""
echo "----------------------------------------"

# #45 GET /api/v1/approvals/pending - 获取待审批列表
echo -e "${YELLOW}测试 #45: 获取待审批列表${NC}"
echo "请求: GET $BASE_URL/api/v1/approvals/pending"
curl -s -X GET "$BASE_URL/api/v1/approvals/pending" \
  -H "Content-Type: application/json" | jq .
echo ""
echo "----------------------------------------"

# 注意：以下测试需要先有审批记录数据
# 在实际测试中，需要先通过权限申请模块创建审批记录

# #46 GET /api/v1/approvals/:id - 获取审批详情（需要先创建审批记录）
echo -e "${YELLOW}测试 #46: 获取审批详情（需要审批记录ID）${NC}"
echo "请求: GET $BASE_URL/api/v1/approvals/500"
curl -s -X GET "$BASE_URL/api/v1/approvals/500" \
  -H "Content-Type: application/json" | jq .
echo ""
echo "----------------------------------------"

# #47 POST /api/v1/approvals/:id/approve - 同意审批
echo -e "${YELLOW}测试 #47: 同意审批${NC}"
echo "请求: POST $BASE_URL/api/v1/approvals/500/approve"
curl -s -X POST "$BASE_URL/api/v1/approvals/500/approve" \
  -H "Content-Type: application/json" \
  -d '{"comment": "API 设计合理，同意上架"}' | jq .
echo ""
echo "----------------------------------------"

# #48 POST /api/v1/approvals/:id/reject - 驳回审批
echo -e "${YELLOW}测试 #48: 驳回审批${NC}"
echo "请求: POST $BASE_URL/api/v1/approvals/501/reject"
curl -s -X POST "$BASE_URL/api/v1/approvals/501/reject" \
  -H "Content-Type: application/json" \
  -d '{"reason": "API 文档缺失，请补充后重新提交"}' | jq .
echo ""
echo "----------------------------------------"

# #49 POST /api/v1/approvals/:id/cancel - 撤销审批
echo -e "${YELLOW}测试 #49: 撤销审批${NC}"
echo "请求: POST $BASE_URL/api/v1/approvals/502/cancel"
curl -s -X POST "$BASE_URL/api/v1/approvals/502/cancel" \
  -H "Content-Type: application/json" | jq .
echo ""
echo "----------------------------------------"

# #50 POST /api/v1/approvals/batch-approve - 批量同意审批
echo -e "${YELLOW}测试 #50: 批量同意审批${NC}"
echo "请求: POST $BASE_URL/api/v1/approvals/batch-approve"
curl -s -X POST "$BASE_URL/api/v1/approvals/batch-approve" \
  -H "Content-Type: application/json" \
  -d '{
    "approvalIds": ["500", "501", "502"],
    "comment": "批量审批通过"
  }' | jq .
echo ""
echo "----------------------------------------"

# #51 POST /api/v1/approvals/batch-reject - 批量驳回审批
echo -e "${YELLOW}测试 #51: 批量驳回审批${NC}"
echo "请求: POST $BASE_URL/api/v1/approvals/batch-reject"
curl -s -X POST "$BASE_URL/api/v1/approvals/batch-reject" \
  -H "Content-Type: application/json" \
  -d '{
    "approvalIds": ["503", "504", "505"],
    "reason": "文档不完整，请补充后重新提交"
  }' | jq .
echo ""
echo "----------------------------------------"

echo -e "${GREEN}========================================"
echo "测试完成！"
echo "========================================${NC}"
