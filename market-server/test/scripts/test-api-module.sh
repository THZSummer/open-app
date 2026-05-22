#!/bin/bash
set -x
# TASK-005 验证脚本：API 管理模块
# 使用方法：bash test-api-module.sh

BASE_URL="http://localhost:18080/open-server"
CATEGORY_ID=""
API_ID=""

echo "=========================================="
echo "TASK-005 验证脚本：API 管理模块"
echo "=========================================="
echo ""

# 1. 创建测试分类
echo "【步骤 1】创建测试分类..."
RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{
    "categoryAlias": "test_api_module",
    "nameCn": "API模块测试分类",
    "nameEn": "API Module Test Category"
  }')

echo "响应：${RESPONSE}"
CATEGORY_ID=$(echo ${RESPONSE} | jq -r '.data.id')
echo "分类ID: ${CATEGORY_ID}"
echo ""

# 2. 注册 API
echo "【步骤 2】注册 API..."
RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/apis \
  -H "Content-Type: application/json" \
  -d "{
    \"nameCn\": \"发送消息\",
    \"nameEn\": \"Send Message\",
    \"path\": \"/api/v1/messages\",
    \"method\": \"POST\",
    \"categoryId\": \"${CATEGORY_ID}\",
    \"permission\": {
      \"nameCn\": \"发送消息权限\",
      \"nameEn\": \"Send Message Permission\",
      \"scope\": \"api:im:send-message\"
    },
    \"properties\": [
      {\"propertyName\": \"descriptionCn\", \"propertyValue\": \"发送消息API的中文描述\"},
      {\"propertyName\": \"docUrl\", \"propertyValue\": \"https://docs.example.com/api/send-message\"}
    ]
  }")

echo "响应：${RESPONSE}"
API_ID=$(echo ${RESPONSE} | jq -r '.data.id')
echo "API ID: ${API_ID}"
echo ""

# 3. 获取 API 列表
echo "【步骤 3】获取 API 列表..."
RESPONSE=$(curl -s "${BASE_URL}/api/v1/apis?categoryId=${CATEGORY_ID}&curPage=1&pageSize=20")
echo "响应：${RESPONSE}"
echo ""

# 4. 获取 API 详情
echo "【步骤 4】获取 API 详情..."
RESPONSE=$(curl -s "${BASE_URL}/api/v1/apis/${API_ID}")
echo "响应：${RESPONSE}"
echo ""

# 5. 更新 API
echo "【步骤 5】更新 API..."
RESPONSE=$(curl -s -X PUT "${BASE_URL}/api/v1/apis/${API_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"nameCn\": \"发送消息V2\",
    \"nameEn\": \"Send Message V2\",
    \"categoryId\": \"${CATEGORY_ID}\",
    \"permission\": {
      \"nameCn\": \"发送消息权限V2\",
      \"nameEn\": \"Send Message Permission V2\"
    }
  }")

echo "响应：${RESPONSE}"
echo ""

# 6. 撤回 API
echo "【步骤 6】撤回 API..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/apis/${API_ID}/withdraw")
echo "响应：${RESPONSE}"
echo ""

# 7. 删除 API
echo "【步骤 7】删除 API..."
RESPONSE=$(curl -s -X DELETE "${BASE_URL}/api/v1/apis/${API_ID}")
echo "响应：${RESPONSE}"
echo ""

# 8. 清理测试分类
echo "【步骤 8】清理测试分类..."
RESPONSE=$(curl -s -X DELETE "${BASE_URL}/api/v1/categories/${CATEGORY_ID}")
echo "响应：${RESPONSE}"
echo ""

echo "=========================================="
echo "验证完成"
echo "=========================================="
