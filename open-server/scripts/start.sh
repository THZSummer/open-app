#!/bin/bash
# open-server 一键启动 — 优先 java -jar，无 jar 自动编译
set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT=18080
CTX="/open-server"
LOG="$APP_DIR/logs/open-server.log"
PID_FILE="$APP_DIR/.pid"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "=========================================="
echo "启动 open-server (管理服务)  端口: $PORT  环境: $PROFILE"
echo "=========================================="

cd "$APP_DIR"

# 端口检查
if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 已被占用，请先执行 ./scripts/stop.sh"
    exit 1
fi

# 检查/构建 jar
JAR=$(ls target/open-server-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "🔨 未找到 jar，正在编译..."
    mvn package -DskipTests -q || { echo "❌ 编译失败"; exit 1; }
    JAR=$(ls target/open-server-*.jar 2>/dev/null | head -1)
fi
echo "📦 $JAR"

# 启动
mkdir -p logs
nohup java -jar "$JAR" --spring.profiles.active="$PROFILE" > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID: $(cat $PID_FILE)"

# 等待就绪 (2s 间隔)
echo -n "⏳ 等待就绪"
for i in $(seq 1 30); do
    sleep 2
    if curl -sf "http://localhost:$PORT$CTX/actuator/health" | grep -q '"status":"UP"'; then
        echo ""
        echo "✅ 就绪! http://localhost:$PORT$CTX"
        exit 0
    fi
    echo -n "."
done
echo ""
echo "⚠️  超时 (60s)，检查日志: tail -f $LOG"
exit 1
