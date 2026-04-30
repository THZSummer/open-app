#!/usr/bin/env pwsh
#Requires -Version 5.1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# ==================== 创建分类 ====================
# 接口：POST /service/open/v2/categories

# ==================== 配置 ====================
$BASE_URL = "http://localhost:8080"
$SESSION_ID = "your-session-id-here"

# ==================== 数据定义（批量） ====================
$CATEGORIES = @(
    @{
        nameCn = "用户管理"
        nameEn = "User Management"
        categoryAlias = "user_mgmt"
        parentId = $null
        sortOrder = 1
    },
    @{
        nameCn = "消息管理"
        nameEn = "Message Management"
        categoryAlias = "msg_mgmt"
        parentId = $null
        sortOrder = 2
    },
    @{
        nameCn = "文件管理"
        nameEn = "File Management"
        categoryAlias = "file_mgmt"
        parentId = $null
        sortOrder = 3
    }
)

# 参数说明：
# - nameCn (必填): 中文名称
# - nameEn (必填): 英文名称
# - categoryAlias (可选): 分类别名，建议使用英文
# - parentId (可选): 父分类ID，顶级分类为 null
# - sortOrder (可选): 排序值，默认0，值小优先

# ==================== 批量执行 ====================
Write-Host "开始创建分类，共 $($CATEGORIES.Count) 条"
Write-Host "======================================"

$SUCCESS = 0
$FAILED = 0

for ($i = 0; $i -lt $CATEGORIES.Count; $i++) {
    $data = $CATEGORIES[$i]
    $name = $data.nameCn
    
    Write-Host ""
    Write-Host "[$($i+1)/$($CATEGORIES.Count)] 创建分类: $name"
    Write-Host "--------------------------------------"
    
    $jsonBody = $data | ConvertTo-Json -Depth 10
    $headers = @{
        "Content-Type" = "application/json"
        "Cookie" = "SESSIONID=$SESSION_ID"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/service/open/v2/categories" `
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