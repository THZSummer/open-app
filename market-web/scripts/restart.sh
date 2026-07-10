#!/bin/bash
# market-web 一键重启 — 先停止再启动
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "重启 market-web"
echo "=========================================="

# 先停止
echo ""
echo ">>> 停止旧进程..."
bash "$SCRIPT_DIR/stop.sh"
echo ""

# 再启动
echo ">>> 启动新进程..."
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PORT=13000
LOG="$APP_DIR/logs/market-web.log"
PID_FILE="$APP_DIR/.pid"

echo "启动 market-web (前端)  端口: $PORT"
echo "=========================================="

cd "$APP_DIR"

if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 仍被占用，请手动检查"
    exit 1
fi

if [ ! -d "node_modules" ]; then
    echo "📦 安装依赖..."
    npm install || { echo "❌ 依赖安装失败"; exit 1; }
fi

mkdir -p logs
nohup npm run dev > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID: $(cat $PID_FILE)"

echo "⏳ 等待就绪..."
for i in $(seq 1 30); do
    sleep 2
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/market-web/" 2>&1)
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "304" ]; then
            echo "  [$i] ✅ HTTP $HTTP_CODE"
            echo "✅ 就绪! http://localhost:$PORT/market-web/"
            exit 0
    fi
    echo "  [$i] ⏳ HTTP $HTTP_CODE"
done
echo ""
echo "⚠️  超时 (60s)，检查日志: tail -f $LOG"
exit 1
