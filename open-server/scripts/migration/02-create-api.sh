#!/bin/bash
set -x  # 只打印命令，不自动退出

# ==================== 创建 API ====================
# 接口：POST /service/open/v2/apis

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
SESSION_ID="your-session-id-here"

# ==================== 数据定义（批量） ====================
APIS=(
  '{
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
  '{
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
  '{
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
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称  
# - path (必填): API路径
# - method (必填): HTTP方法（GET/POST/PUT/DELETE）
# - authType (必填): 认证类型（0=Cookie, 1=SOA, 2=APIG, 3=IAM, 4=免认证, 5=AKSK, 6=CLITOKEN）
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 api:{模块}:{资源}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表
# - properties.propertyName (可选): 属性名称
# - properties.propertyValue (可选): 属性值

# ==================== 批量执行 ====================
echo "开始创建 API，共 ${#APIS[@]} 条"
echo "======================================"

SUCCESS=0
FAILED=0

for ((i=0; i<${#APIS[@]}; i++)); do
  data="${APIS[$i]}"
  name=$(echo "$data" | jq -r '.nameCn')
  
  echo ""
  echo "[$((i+1))/${#APIS[@]}] 创建 API: $name"
  echo "--------------------------------------"
  
  response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/service/open/v2/apis" \
    -H "Content-Type: application/json" \
    -H "Cookie: SESSIONID=${SESSION_ID}" \
    -d "$data")
  
  http_code=$(echo "$response" | tail -n 1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo "✅ 创建成功"
    echo "$body" | jq .
    ((SUCCESS++))
  else
    echo "❌ 创建失败 (HTTP $http_code)"
    echo "$body" | jq . 2>/dev/null || echo "$body"
    ((FAILED++))
  fi
done

echo ""
echo "======================================"
echo "执行完成: 成功 $SUCCESS 条，失败 $FAILED 条"