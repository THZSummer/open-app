@echo off
chcp 65001 >nul

REM ========== 配置（修改一次） ==========
set URL=http://localhost:18080/open-server/service/open/v2/sync/subscription/migrate
set METHOD=POST
set HEADERS=-H "Content-Type: application/json"

REM ========== Body参数（添加多条） ==========
set BODY_1={"ids": [6001]}
set BODY_2={"ids": [6002]}
set BODY_3={"ids": [6001, 6002]}
set BODY_4={"ids": null}
set BODY_5={"ids": []}

REM ========== 执行所有Body ==========
echo 接口批量执行工具
echo URL: %URL%
echo Method: %METHOD%
echo.

echo 执行 #1: %BODY_1%
curl -s -X %METHOD% "%URL%" %HEADERS% -d "%BODY_1%"
echo.

echo 执行 #2: %BODY_2%
curl -s -X %METHOD% "%URL%" %HEADERS% -d "%BODY_2%"
echo.

echo 执行 #3: %BODY_3%
curl -s -X %METHOD% "%URL%" %HEADERS% -d "%BODY_3%"
echo.

echo 执行 #4: %BODY_4%
curl -s -X %METHOD% "%URL%" %HEADERS% -d "%BODY_4%"
echo.

echo 执行 #5: %BODY_5%
curl -s -X %METHOD% "%URL%" %HEADERS% -d "%BODY_5%"
echo.

echo 执行完成
pause
