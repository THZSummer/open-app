# event-server 测试脚本

本目录包含 event-server 服务的测试脚本。

## 目录结构

```
scripts/
├── README.md              # 脚本说明
├── init-test-data.sh      # 测试数据初始化
└── test-all-apis.sh       # 接口测试主脚本
```

## 使用方法

### 1. 初始化测试数据

```bash
cd /home/usb/workspace/wks-open-app/open-app/event-server/test/scripts
chmod +x init-test-data.sh
./init-test-data.sh
```

### 2. 执行接口测试

```bash
chmod +x test-all-apis.sh
./test-all-apis.sh
```

## 测试覆盖

| 模块 | 接口数 | 说明 |
|-----|-------|------|
| 事件发布 | 1 | 事件分发、WebHook、消息队列 |
| 回调触发 | 1 | 回调触发、WebHook、SSE、WebSocket |
| **总计** | **2** | |

## 测试数据

测试数据包含：
- 事件数据：4 条（消息接收、消息已读、用户上线、会议开始）
- 回调数据：3 条（审批完成、文件上传、订单状态变更）
- 权限数据：4 条（事件权限 2 条、回调权限 2 条）
- 订阅数据：9 条（事件订阅、回调订阅、待审订阅）

## 测试用例

详细的测试用例请查看 [../cases/test-case-event-server.md](../cases/test-case-event-server.md)

## 测试报告

测试报告保存在 `../reports/` 目录，格式为：
- 文件名：`test-report-event-server-YYYYMMDD-HHMMSS.md`

## 前置条件

1. event-server 服务正在运行（端口 18082）
2. MySQL 数据库可访问（数据库名：openplatform_v2）
3. 已安装 jq 工具（用于 JSON 解析）

## 相关文档

- [测试用例文档](../cases/test-case-event-server.md)
- [测试报告目录](../reports/)

---
*最后更新: 2026-04-22*
