# Event-Server 测试文档

本目录包含 event-server 服务的测试用例和测试脚本。

## 目录结构

```
test/
├── README.md           # 测试说明
├── cases/              # 测试用例
│   └── test-case-event-server.md
├── scripts/            # 测试脚本
│   ├── README.md       # 脚本说明
│   ├── init-test-data.sh   # 测试数据初始化
│   └── test-all-apis.sh    # 接口测试主脚本
└── reports/            # 测试报告
    └── test-report-event-server-*.md
```

## 快速开始

### 1. 执行接口测试

```bash
cd /home/usb/workspace/wks-open-app/open-app/event-server/test/scripts
./test-all-apis.sh
```

### 2. 初始化测试数据

```bash
./init-test-data.sh
```

## 测试覆盖

- **接口总数**: 2 个
- **测试用例数**: 11 个
- **覆盖率**: 100%

## 功能模块

- **事件发布**: POST /gateway/events/publish
- **回调触发**: POST /gateway/callbacks/invoke

## 测试数据

测试数据包含：
- 事件数据：4 条
- 回调数据：3 条
- 权限数据：4 条
- 订阅数据：9 条

## 测试用例

详细的测试用例请查看 [cases/test-case-event-server.md](./cases/test-case-event-server.md)

## 测试脚本

测试脚本说明请查看 [scripts/README.md](./scripts/README.md)

## 相关文档

- [接口设计文档](../../plan-api.md)
- [数据库设计文档](../../plan-db.md)

---
*最后更新: 2026-04-22*
