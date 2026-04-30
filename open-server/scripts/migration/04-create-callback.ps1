#!/usr/bin/env pwsh
#Requires -Version 5.1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# ==================== 创建回调 ====================
# 接口：POST /service/open/v2/callbacks

# ==================== 配置 ====================
$BASE_URL = "http://localhost:8080"
$SESSION_ID = "your-session-id-here"

# ==================== 数据定义（批量） ====================
$CALLBACKS = @(
    @{
        nameCn = "订单状态变更回调"
        nameEn = "Order Status Change Callback"
        categoryId = 2
        permission = @{
            nameCn = "接收订单状态变更权限"
            nameEn = "Receive Order Status Change Permission"
            scope = "callback:order:status"
            needApproval = 1
            resourceNodes = $null
        }
        properties = @(
            @{ propertyName = "doc_url"; propertyValue = "https://doc.example.com/callback/order-status" }
            @{ propertyName = "timeout"; propertyValue = "5000" }
        )
    },
    @{
        nameCn = "用户登录通知回调"
        nameEn = "User Login Notification Callback"
        categoryId = 1
        permission = @{
            nameCn = "接收用户登录通知权限"
            nameEn = "Receive User Login Notification Permission"
            scope = "callback:user:login"
            needApproval = 1
            resourceNodes = $null
        }
        properties = @(
            @{ propertyName = "doc_url"; propertyValue = "https://doc.example.com/callback/user-login" }
            @{ propertyName = "timeout"; propertyValue = "3000" }
        )
    },
    @{
        nameCn = "消息推送回调"
        nameEn = "Message Push Callback"
        categoryId = 2
        permission = @{
            nameCn = "接收消息推送权限"
            nameEn = "Receive Message Push Permission"
            scope = "callback:message:push"
            needApproval = 0
            resourceNodes = $null
        }
        properties = @(
            @{ propertyName = "doc_url"; propertyValue = "https://doc.example.com/callback/message-push" }
            @{ propertyName = "timeout"; propertyValue = "10000" }
        )
    }
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称
# - categoryId (必填): 分类ID
# - permission.nameCn (必填): 权限中文名称
# - permission.nameEn (必填): 权限英文名称
# - permission.scope (必填): Scope标识，格式 callback:{模块}:{资源}
# - permission.needApproval (可选): 是否需要审批，默认1
# - permission.resourceNodes (可选): 资源级审批节点配置，JSON字符串
# - properties (可选): 扩展属性列表

# ==================== 批量执行 ====================
Write-Host "开始创建回调，共 $($CALLBACKS.Count) 条"
Write-Host "======================================"

$SUCCESS = 0
$FAILED = 0

for ($i = 0; $i -lt $CALLBACKS.Count; $i++) {
    $data = $CALLBACKS[$i]
    $name = $data.nameCn
    
    Write-Host ""
    Write-Host "[$($i+1)/$($CALLBACKS.Count)] 创建回调: $name"
    Write-Host "--------------------------------------"
    
    $jsonBody = $data | ConvertTo-Json -Depth 10
    $headers = @{
        "Content-Type" = "application/json"
        "Cookie" = "SESSIONID=$SESSION_ID"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/service/open/v2/callbacks" `
            -Method POST `
            -Headers $headers `
            -Body $jsonBody `
            -ContentType "application/json; charset=utf-8"
        
        Write-Host "✅ 创建成功"
        $response | ConvertTo-Json -Depth 10
        $SUCCESS++
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        
        Write-Host "❌ 创建失败 (HTTP $statusCode)"
        try {
            $errorJson = $responseBody | ConvertFrom-Json
            $errorJson | ConvertTo-Json -Depth 10
        }
        catch {
            Write-Host $responseBody
        }
        $FAILED++
    }
}

Write-Host ""
Write-Host "======================================"
Write-Host "执行完成: 成功 $SUCCESS 条，失败 $FAILED 条"