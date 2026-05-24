#!/bin/bash
set -x
# connector-api 一键启动脚本
# 连接器平台运行时服务 (Spring WebFlux + R2DBC)

echo "=========================================="
echo "启动 connector-api (运行时服务)"
echo "端口: 18180"
echo "=========================================="

cd "$(dirname "$0")/.."

# 检查服务是否已启动
if lsof -i:18180 > /dev/null 2>&1; then
    echo "⚠️  端口 18180 已被占用，服务可能已在运行"
    echo "请先执行 ./scripts/stop.sh 停止服务"
    exit 1
fi

# 启动服务（后台运行）
echo "正在启动服务..."
nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > logs/connector-api.log 2>&1 &

# 记录进程ID
PID=$!
echo $PID > .pid

echo "✅ 服务启动成功!"
echo "   PID: $PID"
echo "   日志: logs/connector-api.log"
echo ""
echo "等待服务启动完成..."

# 等待服务启动
for i in {1..30}; do
    if curl -s http://localhost:18180/actuator/health > /dev/null 2>&1; then
        echo "✅ 服务已就绪! 访问地址: http://localhost:18180"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo ""
echo "⚠️  服务启动超时，请检查日志: logs/connector-api.log"
exit 1