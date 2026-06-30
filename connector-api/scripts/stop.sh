#!/bin/bash
# connector-api 一键停止
# set 不含 -x：-x 会回显每条命令，淹没业务进度输出，故日常运维脚本静默化
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT=18180
PID_FILE="$APP_DIR/.pid"

echo "=========================================="
echo "停止 connector-api"
echo "=========================================="

cd "$APP_DIR"

stopped=0
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "停止 PID $PID..."
        kill "$PID" && sleep 1
        kill -0 "$PID" 2>/dev/null && kill -9 "$PID" 2>/dev/null
        stopped=1
    fi
    rm -f "$PID_FILE"
fi

if lsof -ti:$PORT > /dev/null 2>&1; then
    echo "清理端口 $PORT..."
    kill -9 $(lsof -ti:$PORT) 2>/dev/null || true
    stopped=1
fi

[ $stopped -eq 1 ] && echo "✅ 已停止" || echo "ℹ️  未在运行"
