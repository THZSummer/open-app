#!/bin/bash
set -ex

# ==================== 数据迁移配置 ====================

# 服务器配置
export BASE_URL="http://localhost:8080"
export API_PREFIX="/service/open/v2"

# 认证配置（根据实际情况修改）
export COOKIE_NAME="SESSIONID"
export COOKIE_VALUE="your-session-id-here"

# 请求超时时间（秒）
export TIMEOUT=30

# 输出目录
export OUTPUT_DIR="./migration-output"

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 公共请求函数
# 参数：
#   $1 - method: HTTP方法 (GET, POST, PUT, DELETE)
#   $2 - endpoint: API端点路径
#   $3 - data: 请求数据 (JSON格式，可选)
#   $4 - output_file: 输出文件名
# 返回：
#   响应内容保存到 $OUTPUT_DIR/$output_file
#   在标准输出打印 HTTP 状态码
request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local output_file=$4
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $method ${BASE_URL}${API_PREFIX}${endpoint}"
    
    curl -s -X "$method" \
        "${BASE_URL}${API_PREFIX}${endpoint}" \
        -H "Content-Type: application/json" \
        -H "Cookie: ${COOKIE_NAME}=${COOKIE_VALUE}" \
        --connect-timeout "$TIMEOUT" \
        ${data:+-d "$data"} \
        -o "$OUTPUT_DIR/${output_file}" \
        -w "\nHTTP_STATUS:%{http_code}"
}

# 解析响应状态码
# 参数：
#   $1 - curl 输出字符串 (包含 HTTP_STATUS:xxx)
# 返回：
#   HTTP 状态码
get_http_status() {
    local response=$1
    echo "$response" | grep -oP 'HTTP_STATUS:\K\d+'
}

# 成功判断函数
# 参数：
#   $1 - HTTP 状态码
# 返回：
#   0 表示成功，1 表示失败
is_success() {
    local status=$1
    if [[ "$status" =~ ^2[0-9][0-9]$ ]]; then
        return 0
    else
        return 1
    fi
}

# 打印结果
# 参数：
#   $1 - 输出文件名
#   $2 - HTTP 状态码
print_result() {
    local output_file=$1
    local status=$2
    
    echo "======================================"
    echo "HTTP Status: $status"
    echo "Response Body:"
    cat "$OUTPUT_DIR/$output_file" | jq '.' 2>/dev/null || cat "$OUTPUT_DIR/$output_file"
    echo ""
    echo "======================================"
}