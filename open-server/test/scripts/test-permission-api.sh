#!/bin/bash

# TASK-008 权限管理模块接口测试脚本

BASE_URL="http://localhost:18080/open-server"
TOKEN="test-token"

echo "=========================================="
echo "TASK-008 权限管理模块接口测试"
echo "=========================================="
echo ""

# 测试健康检查
echo "1. 测试健康检查..."
curl -s "$BASE_URL/actuator/health" | jq '.' 2>/dev/null || echo "健康检查接口未启动"
echo ""

# 测试 #27 获取应用 API 权限列表
echo "2. 测试 #27 获取应用 API 权限列表..."
curl -s -X GET "$BASE_URL/api/v1/apps/10/apis?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #28 获取分类下 API 权限列表
echo "3. 测试 #28 获取分类下 API 权限列表（权限树懒加载）..."
curl -s -X GET "$BASE_URL/api/v1/categories/2/apis?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #29 申请 API 权限
echo "4. 测试 #29 申请 API 权限（批量）..."
curl -s -X POST "$BASE_URL/api/v1/apps/10/apis/subscribe" \
  -H "Content-Type: application/json" \
  -d '{"permissionIds":["200","201"]}' | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #31 获取应用事件订阅列表
echo "5. 测试 #31 获取应用事件订阅列表..."
curl -s -X GET "$BASE_URL/api/v1/apps/10/events?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #32 获取分类下事件权限列表
echo "6. 测试 #32 获取分类下事件权限列表..."
curl -s -X GET "$BASE_URL/api/v1/categories/2/events?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #33 申请事件权限
echo "7. 测试 #33 申请事件权限（批量）..."
curl -s -X POST "$BASE_URL/api/v1/apps/10/events/subscribe" \
  -H "Content-Type: application/json" \
  -d '{"permissionIds":["201","202"]}' | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #34 配置事件消费参数
echo "8. 测试 #34 配置事件消费参数..."
curl -s -X PUT "$BASE_URL/api/v1/apps/10/events/300/config" \
  -H "Content-Type: application/json" \
  -d '{"channelType":1,"channelAddress":"https://webhook.example.com/events","authType":0}' | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #36 获取应用回调订阅列表
echo "9. 测试 #36 获取应用回调订阅列表..."
curl -s -X GET "$BASE_URL/api/v1/apps/10/callbacks?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #37 获取分类下回调权限列表
echo "10. 测试 #37 获取分类下回调权限列表..."
curl -s -X GET "$BASE_URL/api/v1/categories/3/callbacks?curPage=1&pageSize=10" \
  -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #38 申请回调权限
echo "11. 测试 #38 申请回调权限（批量）..."
curl -s -X POST "$BASE_URL/api/v1/apps/10/callbacks/subscribe" \
  -H "Content-Type: application/json" \
  -d '{"permissionIds":["202","203"]}' | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

# 测试 #39 配置回调消费参数
echo "12. 测试 #39 配置回调消费参数..."
curl -s -X PUT "$BASE_URL/api/v1/apps/10/callbacks/400/config" \
  -H "Content-Type: application/json" \
  -d '{"channelType":0,"channelAddress":"https://webhook.example.com/callbacks","authType":0}' | jq '.' 2>/dev/null || echo "接口调用失败"
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
