#!/bin/bash
# wecodesite 一键启动 (Vite React 前端)
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT=5173
LOG="$APP_DIR/logs/wecodesite.log"
PID_FILE="$APP_DIR/.pid"

echo "=========================================="
echo "启动 wecodesite (前端)  端口: $PORT"
echo "=========================================="

cd "$APP_DIR"

if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 已被占用，请先执行 ./scripts/stop.sh"
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
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/" 2>&1)
    if [ "$HTTP_CODE" = "200" ]; then
        echo "  [$i] ✅ HTTP $HTTP_CODE"
        echo "✅ 就绪! http://localhost:$PORT"
        exit 0
    fi
    echo "  [$i] ⏳ HTTP $HTTP_CODE"
done
echo ""
echo "⚠️  超时 (60s)，检查日志: tail -f $LOG"
exit 1
