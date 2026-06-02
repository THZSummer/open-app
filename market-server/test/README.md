# Open-Server 测试文档

本目录包含 open-server 服务的测试用例、测试报告和测试脚本。

## 目录结构

```
test/
├── README.md           # 测试说明
├── cases/              # 测试用例
│   └── test-case-open-server.md
├── reports/            # 测试报告
│   ├── test-report-open-server-20260422-094752.md
│   └── test-report-open-server-20260422-110035.md
└── scripts/            # 测试脚本
    ├── README.md
    ├── test-all-apis.sh
    ├── test-all-apis-optimized.sh
    ├── test_all_apis.sh
    ├── test-api-module.sh
    ├── test-callback-api.sh
    ├── test-permission-api.sh
    ├── test-approval-apis.sh
    ├── verify-task-011.sh
    └── verify-test-data.sh
```

## 测试覆盖

- **接口总数**: 51 个
- **测试用例数**: 51 个
- **覆盖率**: 100%

## 功能模块

- 分类管理
- API管理
- 事件管理
- 回调管理
- 权限管理
- 审批管理

## 执行测试

### 快速执行

```bash
cd open-server/test/scripts
./test-all-apis-optimized.sh
```

### 查看测试报告

测试报告位于 `reports/` 目录，按时间戳命名。

## 相关文档

- [接口设计文档](../../plan-api.md)
- [数据库设计文档](../../plan-db.md)

---
*最后更新: 2026-04-22*
