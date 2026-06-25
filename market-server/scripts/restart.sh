#!/bin/bash
# market-server 一键重启 — 先停止再启动
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "重启 market-server"
echo "=========================================="

# 先停止
echo ""
echo ">>> 停止旧进程..."
bash "$SCRIPT_DIR/stop.sh"
echo ""

# 再启动
echo ">>> 启动新进程..."
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PORT=18083
CTX="/market-server"
LOG="$APP_DIR/logs/market-server.log"
PID_FILE="$APP_DIR/.pid"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "启动 market-server (应用市场)  端口: $PORT  环境: $PROFILE"
echo "=========================================="

cd "$APP_DIR"

if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 仍被占用，请手动检查"
    exit 1
fi

echo "🔨 编译启动中..."
mkdir -p logs
nohup mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE" > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID: $(cat $PID_FILE)"

echo "⏳ 等待就绪..."
for i in $(seq 1 30); do
    sleep 2
    RESULT=$(curl -s "http://localhost:$PORT$CTX/actuator/health" 2>&1)
    if echo "$RESULT" | grep -q '"status":"UP"'; then
        echo "  [$i] ✅ $RESULT"
        echo "✅ 就绪! http://localhost:$PORT$CTX"
        exit 0
    fi
    echo "  [$i] ⏳ $RESULT"
done
echo ""
echo "⚠️  超时 (60s)，检查日志: tail -f $LOG"
exit 1
