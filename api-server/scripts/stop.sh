#!/bin/bash
set -x
# api-server 一键停止脚本

echo "=========================================="
echo "停止 api-server (API网关服务)"
echo "=========================================="

cd "$(dirname "$0")/.."

if [ -f .pid ]; then
    PID=$(cat .pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo "正在停止服务 (PID: $PID)..."
        kill $PID
        sleep 3
        
        if ps -p $PID > /dev/null 2>&1; then
            echo "强制停止服务..."
            kill -9 $PID
        fi
        
        rm -f .pid
        echo "✅ 服务已停止"
    else
        echo "⚠️  进程不存在 (PID: $PID)"
        rm -f .pid
    fi
else
    echo "⚠️  未找到 .pid 文件"
fi

if lsof -i:18081 > /dev/null 2>&1; then
    echo "端口 18081 仍在使用，尝试强制清理..."
    PID=$(lsof -t -i:18081)
    kill -9 $PID 2>/dev/null
    echo "✅ 端口已清理"
fi

echo "=========================================="
