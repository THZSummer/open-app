#!/bin/bash
set -x
# open-web 一键启动脚本

echo "=========================================="
echo "启动 open-web (前端应用)"
echo "端口: 13000"
echo "=========================================="

cd "$(dirname "$0")/.."

# 检查服务是否已启动
if lsof -i:13000 > /dev/null 2>&1; then
    echo "⚠️  端口 13000 已被占用，服务可能已在运行"
    echo "请先执行 ./scripts/stop.sh 停止服务"
    exit 1
fi

# 检查依赖是否已安装
if [ ! -d "node_modules" ]; then
    echo "正在安装依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败"
        exit 1
    fi
fi

# 启动服务（后台运行）
echo "正在启动前端服务..."
nohup npm run dev > logs/open-web.log 2>&1 &

PID=$!
echo $PID > .pid

echo "✅ 服务启动成功!"
echo "   PID: $PID"
echo "   日志: logs/open-web.log"
echo ""
echo "等待服务启动完成..."

for i in {1..30}; do
    if curl -s http://localhost:13000/open-web/ > /dev/null 2>&1; then
        echo ""
        echo "✅ 服务已就绪! 访问地址: http://localhost:13000/open-web/"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo ""
echo "⚠️  服务启动超时，请检查日志: logs/open-web.log"
exit 1
