#!/bin/bash
# connector-api 一键启动 — WebFlux 运行时服务
set -uo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT=18180
CTX=""   # connector-api 无 context-path
LOG="$APP_DIR/logs/connector-api.log"
PID_FILE="$APP_DIR/.pid"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "=========================================="
echo "启动 connector-api (运行时)  端口: $PORT  环境: $PROFILE"
echo "=========================================="

cd "$APP_DIR"

if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  端口 $PORT 已被占用，请先执行 ./scripts/stop.sh"
    exit 1
fi

JAR=$(ls target/connector-api-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "🔨 未找到 jar，正在编译..."
    mvn package -DskipTests -q || { echo "❌ 编译失败"; exit 1; }
    JAR=$(ls target/connector-api-*.jar 2>/dev/null | head -1)
fi
echo "📦 $JAR"

mkdir -p logs
nohup java -jar "$JAR" --spring.profiles.active="$PROFILE" > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID: $(cat $PID_FILE)"

echo "⏳ 等待就绪..."
for i in $(seq 1 30); do
    sleep 2
    RESULT=$(curl -s "http://localhost:$PORT/actuator/health" 2>&1)
    if echo "$RESULT" | grep -q '"status":"UP"'; then
        echo "  [$i] ✅ $RESULT"
        echo "✅ 就绪! http://localhost:$PORT"
        exit 0
    fi
    echo "  [$i] ⏳ $RESULT"
done
echo ""
echo "⚠️  超时 (60s)，检查日志: tail -f $LOG"
exit 1
