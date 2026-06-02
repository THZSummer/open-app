#!/bin/bash
set -x
# 回调管理模块测试脚本
# 测试接口 #21 ~ #26

BASE_URL="http://localhost:18080/open-server"
API_PREFIX="/api/v1/callbacks"

echo "========================================="
echo "回调管理模块接口测试"
echo "========================================="
echo ""

# 等待服务启动
echo "等待服务启动..."
sleep 2

# #23 注册回调
echo "1. 测试 #23 POST ${API_PREFIX} - 注册回调"
RESPONSE=$(curl -s -X POST "${BASE_URL}${API_PREFIX}" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "审批完成回调",
    "nameEn": "Approval Completed Callback",
    "categoryId": "1",
    "permission": {
      "nameCn": "审批完成回调权限",
      "nameEn": "Approval Completed Callback Permission",
      "scope": "callback:approval:completed"
    },
    "properties": [
      {"propertyName": "descriptionCn", "propertyValue": "审批完成后的回调通知"},
      {"propertyName": "docUrl", "propertyValue": "https://docs.example.com/callback/approval-completed"}
    ]
  }')

echo "响应: $RESPONSE"
CALLBACK_ID=$(echo $RESPONSE | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "回调ID: $CALLBACK_ID"
echo ""

# #21 获取回调列表
echo "2. 测试 #21 GET ${API_PREFIX} - 获取回调列表"
curl -s -X GET "${BASE_URL}${API_PREFIX}" | jq .
echo ""

# #22 获取回调详情
echo "3. 测试 #22 GET ${API_PREFIX}/{id} - 获取回调详情"
curl -s -X GET "${BASE_URL}${API_PREFIX}/${CALLBACK_ID}" | jq .
echo ""

# #24 更新回调
echo "4. 测试 #24 PUT ${API_PREFIX}/{id} - 更新回调"
curl -s -X PUT "${BASE_URL}${API_PREFIX}/${CALLBACK_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "nameCn": "审批完成回调V2",
    "nameEn": "Approval Completed Callback V2",
    "permission": {
      "nameCn": "审批完成回调权限V2",
      "nameEn": "Approval Completed Callback Permission V2"
    }
  }' | jq .
echo ""

# #26 撤回回调
echo "5. 测试 #26 POST ${API_PREFIX}/{id}/withdraw - 撤回回调"
curl -s -X POST "${BASE_URL}${API_PREFIX}/${CALLBACK_ID}/withdraw" | jq .
echo ""

# #25 删除回调
echo "6. 测试 #25 DELETE ${API_PREFIX}/{id} - 删除回调"
curl -s -X DELETE "${BASE_URL}${API_PREFIX}/${CALLBACK_ID}" | jq .
echo ""

echo "========================================="
echo "测试完成"
echo "========================================="
