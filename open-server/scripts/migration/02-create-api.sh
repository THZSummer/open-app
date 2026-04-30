#!/bin/bash
set -e

# ==================== 创建 API ====================
# 接口：POST /service/open/v2/apis

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
declare -a NAMES=("获取用户信息" "发送消息" "上传文件")

DATA_1='{
  "nameCn": "获取用户信息",
  "nameEn": "Get User Info",
  "path": "/api/v1/user/info",
  "method": "GET",
  "authType": 1,
  "categoryId": 1,
  "permission": {
    "nameCn": "查询用户信息权限",
    "nameEn": "Query User Info Permission",
    "scope": "api:user:query-info",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/api/user-info"
    },
    {
      "propertyName": "rate_limit",
      "propertyValue": "100/min"
    }
  ]
}'

DATA_2='{
  "nameCn": "发送消息",
  "nameEn": "Send Message",
  "path": "/api/v1/message/send",
  "method": "POST",
  "authType": 1,
  "categoryId": 2,
  "permission": {
    "nameCn": "发送消息权限",
    "nameEn": "Send Message Permission",
    "scope": "api:message:send",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/api/message-send"
    },
    {
      "propertyName": "rate_limit",
      "propertyValue": "50/min"
    }
  ]
}'

DATA_3='{
  "nameCn": "上传文件",
  "nameEn": "Upload File",
  "path": "/api/v1/file/upload",
  "method": "POST",
  "authType": 1,
  "categoryId": 3,
  "permission": {
    "nameCn": "上传文件权限",
    "nameEn": "Upload File Permission",
    "scope": "api:file:upload",
    "needApproval": 1,
    "resourceNodes": null
  },
  "properties": [
    {
      "propertyName": "doc_url",
      "propertyValue": "https://doc.example.com/api/file-upload"
    },
    {
      "propertyName": "max_size",
      "propertyValue": "100MB"
    }
  ]
}'

declare -a DATA=("$DATA_1" "$DATA_2" "$DATA_3")

# ==================== 批量执行 ====================
echo "开始创建 API，共 ${#DATA[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0
RESPONSE_FILE="/tmp/api_response.json"

for i in "${!DATA[@]}"; do
  echo ""
  echo "[$((i+1))/${#DATA[@]}] 创建 API: ${NAMES[$i]}"
  echo "--------------------------------------"
  
  http_code=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" -X POST \
    "${BASE_URL}/service/open/v2/apis" \
    -H "Content-Type: application/json" \
    -H "Cookie: SESSIONID=${SESSION_ID}" \
    -d "${DATA[$i]}")
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo "✅ 创建成功"
    cat "$RESPONSE_FILE"
    ((SUCCESS++))
  else
    echo "❌ 创建失败 (HTTP $http_code)"
    cat "$RESPONSE_FILE"
    ((FAILED++))
  fi
done

echo ""
echo "======================================"
echo "执行完成: 成功 $SUCCESS 条，失败 $FAILED 条"
