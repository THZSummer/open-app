#!/bin/bash
# event-server 一键启动脚本

echo "=========================================="
echo "启动 event-server (事件/回调网关服务)"
echo "端口: 18082"
echo "=========================================="

cd "$(dirname "$0")/.."

if lsof -i:18082 > /dev/null 2>&1; then
    echo "⚠️  端口 18082 已被占用，服务可能已在运行"
    echo "请先执行 ./scripts/stop.sh 停止服务"
    exit 1
fi

echo "正在启动服务..."
nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > logs/event-server.log 2>&1 &

PID=$!
echo $PID > .pid

echo "✅ 服务启动成功!"
echo "   PID: $PID"
echo "   日志: logs/event-server.log"
echo ""
echo "等待服务启动完成..."

for i in {1..30}; do
    if curl -s http://localhost:18082/event-server/actuator/health > /dev/null 2>&1; then
        echo "✅ 服务已就绪! 访问地址: http://localhost:18082/event-server"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo ""
echo "⚠️  服务启动超时，请检查日志: logs/event-server.log"
exit 1
