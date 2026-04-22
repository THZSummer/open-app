# api-server 测试脚本

本目录包含 api-server 服务的测试脚本。

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
cd /home/usb/workspace/wks-open-app/open-app/api-server/test/scripts
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
| Scope 授权管理 | 3 | 用户授权列表、创建授权、取消授权 |
| 消费网关 | 2 | API代理鉴权、权限校验 |
| **总计** | **5** | |

## 测试用例

详细的测试用例请查看 [../cases/test-case-api-server.md](../cases/test-case-api-server.md)

## 测试报告

测试报告保存在 `../reports/` 目录，格式为：
- 文件名：`test-report-api-server-YYYYMMDD-HHMMSS.md`

## 前置条件

1. api-server 服务正在运行（端口 18081）
2. MySQL 数据库可访问
3. 已安装 jq 工具（用于 JSON 解析）

## 相关文档

- [测试用例文档](../cases/test-case-api-server.md)
- [测试报告目录](../reports/)

---
*最后更新: 2026-04-22*
