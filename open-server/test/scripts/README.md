# 测试脚本目录

本目录包含能力开放平台的接口测试脚本。

## 脚本列表

| 脚本 | 说明 | 接口范围 |
|------|------|---------|
| `test_all_apis.sh` | 全量接口测试脚本 | 58个接口 |
| `test-all-apis.sh` | 全量接口测试脚本(v2) | 58个接口 |
| `test-callback-api.sh` | 回调管理模块测试 | #21-26 |
| `test-permission-api.sh` | 权限管理模块测试 | #27-40 |
| `test-approval-apis.sh` | 审批管理模块测试 | #41-51 |
| `test-api-module.sh` | API模块测试 | #9-14 |
| `verify-task-011.sh` | event-server验证脚本 | #56-57 |

## 使用方法

```bash
# 运行全量测试
./scripts/test_all_apis.sh

# 运行单个模块测试
./scripts/test-callback-api.sh
./scripts/test-permission-api.sh
./scripts/test-approval-apis.sh
```

## 前置条件

- 服务已启动 (open-server:18080, api-server:18081, event-server:18082)
- 数据库已初始化测试数据
- 已安装 jq 命令 (用于JSON格式化)

---

*最后更新: 2026-04-22*
