#!/bin/bash
# open-server 一键重启 — 先停止再启动
# set 不含 -x：-x 会回显每条命令，淹没业务进度输出，故日常运维脚本静默化
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "重启 open-server"
echo "=========================================="

# 先停止
echo ""
echo ">>> 停止旧进程..."
bash "$SCRIPT_DIR/stop.sh"
echo ""

# 再启动
echo ">>> 启动新进程..."
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PORT=18080
CTX="/open-server"
LOG="$APP_DIR/logs/open-server.log"
PID_FILE="$APP_DIR/.pid"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "启动 open-server (管理服务)  端口: $PORT  环境: $PROFILE"
echo "=========================================="

cd "$APP_DIR"

# 端口检查（stop.sh 已清理，这里做二次确认）
if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 仍被占用，请手动检查"
    exit 1
fi

# 启动
echo "🔨 编译启动中..."
mkdir -p logs
nohup mvn spring-boot:run -Dspring-boot.run.profiles="$PROFILE" > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID: $(cat $PID_FILE)"

# 等待就绪 (2s 间隔)
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
