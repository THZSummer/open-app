@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ========== 配置（修改一次） ==========
set URL=http://localhost:18080/open-server/service/open/v2/sync/subscription/migrate
set METHOD=POST

set HEADER_1=Content-Type: application/json
set HEADER_2=X-Session-Id: your-session-id
set HEADER_3=X-Request-Id: test-001

set HEADERS=-H "%HEADER_1%" -H "%HEADER_2%" -H "%HEADER_3%"

REM ========== Body参数（数量不固定） ==========
set BODY_1={"ids": [6001]}
set BODY_2={"ids": [6002]}
set BODY_3={"ids": [6001, 6002]}
set BODY_4={"ids": null}
set BODY_5={"ids": []}
set BODY_COUNT=5

REM ========== 循环执行 ==========
echo 接口批量执行工具
echo URL: %URL%
echo Method: %METHOD%
echo Body数量: %BODY_COUNT%
echo.

for /L %%i in (1,1,%BODY_COUNT%) do (
    echo 执行 #%%i: !BODY_%%i!
    curl -s -X %METHOD% "%URL%" %HEADERS% -d "!BODY_%%i!"
    echo.
)

echo 执行完成
pause
