# wecodesite 前端 E2E 测试

参照 `open-server/src/test/python/` 目录结构。

## 目录结构

```
tests/
├── README.md                    # 本文档
├── pytest.ini                   # pytest 配置
├── conftest.py                  # 共享 fixture: base_url, 页面 URL
├── common/
│   ├── __init__.py
│   └── client.py                # 公共模块: Playwright DOM 操作封装
└── e2e/
    ├── __init__.py
    ├── test_single_node.md      # 单节点: trigger → connector → exit 设计文档
    ├── test_single_node.py      # 单节点测试脚本
    ├── test_serial.md           # 串行: 5 节点串行拓扑 设计文档
    ├── test_serial.py           # 串行测试脚本
    ├── test_parallel.md         # 并行: 6 节点并行拓扑 设计文档
    └── test_parallel.py         # 并行测试脚本
```

## 安装依赖

```bash
pip install playwright pytest pytest-playwright
playwright install chromium
```

### 系统库依赖（如 Chromium 启动报 libgbm.so.1 缺失）

```bash
# 方式 1: 系统安装 (需 sudo)
sudo apt-get install -y libgbm1

# 方式 2: 手动提取 (无需 sudo)
mkdir -p /tmp/playwright-libs && cd /tmp/playwright-libs
apt download libgbm1 mesa-libgallium libx11-xcb1 libxcb-dri3-0 \
    libxcb-present0 libxcb-sync1 libxshmfence1
for f in *.deb; do dpkg -x "$f" .; done
```

## 运行

```bash
# 前置条件: 确保以下服务已启动
#   - wecodesite Vite dev server (192.168.3.110:5173)
#   - open-server (localhost:18080)
#   - connector-api (localhost:18180)

# 设置 Library Path (如手动安装了 libgbm)
export LD_LIBRARY_PATH=/tmp/playwright-libs/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH

# 全部 E2E
cd wecodesite/tests && pytest

# 仅 L1
pytest -m L1

# 单文件（详细输出）
pytest e2e/test_single_node.py -s -v

# 单文件（含失败截图）
pytest e2e/test_single_node.py -s -v --screenshot=on
```

## 环境依赖

| 组件 | 端口 | 启动方式 | 说明 |
|------|------|---------|------|
| wecodesite (Vite dev) | 5173 | `npm run dev` | 前端页面 |
| open-server | 18080 | `scripts/restart.sh` | 管理面 API |
| connector-api | 18180 | `scripts/restart.sh` | 运行时 |
| MySQL | 192.168.3.155 | 已有 | 测试数据库 |
| Redis | 192.168.3.201-205 | 已有 | 缓存/限流 |
| Mock Server | 18999 | 测试内启动 | 模拟下游 API |

## 前端页面路径

> **注意**: 所有页面 URL 均需带 `appId` 参数才能正常访问。

| 功能 | Hash 路由 |
|------|-----------|
| 连接器列表 | `#/connectorList?appId={appId}` |
| 连接器编辑 | `#/connectorEditor?appId={appId}&id={id}` |
| 连接流列表 | `#/flowList?appId={appId}` |
| 连接流编辑 V2 | `#/flowEditor?appId={appId}&id={id}` |

> **重要**: ConnectorEditor 和 FlowEditor 均需**先点击"创建草稿"**才能进入配置/编排界面。

## 三种流模式

### 单节点 (Single)
**拓扑**: `trigger → connector → exit`
**验证**: 调试 + 调用返回正确

### 串行 (Serial)
**拓扑**: `trigger → script₁ → connector → script₂ → exit`
**验证**: steps 顺序正确, 节点间数据传递

### 并行 (Parallel)
**拓扑**: `trigger → script → [parallel] → connₐ / conn_b → merge → exit`
**验证**: 两分支均成功, 合并结果含双方数据

## 设计原则

- **不调 API / 不操作 DB** — 所有数据通过前端 UI 创建
- **`common/client.py`** — 统一的 DOM 操作封装, 对应 open-server 的 `common/client.py`
- **等待策略** — `wait_for_table_ready` / `wait_for_modal_ready` / `wait_for_message_success` 处理 Ant Design 异步渲染
