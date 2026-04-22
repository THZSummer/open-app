# Api-Server 测试文档

本目录包含 api-server 服务的测试用例和测试脚本。

## 目录结构

```
test/
├── README.md           # 测试说明
├── cases/              # 测试用例
│   └── test-case-api-server.md
├── scripts/            # 测试脚本
│   ├── README.md       # 脚本说明
│   ├── init-test-data.sh    # 测试数据初始化
│   └── test-all-apis.sh     # 接口测试主脚本
└── reports/            # 测试报告
    └── test-report-api-server-YYYYMMDD-HHMMSS.md
```

## 快速开始

### 1. 初始化测试数据

```bash
cd api-server/test/scripts
./init-test-data.sh
```

### 2. 执行接口测试

```bash
./test-all-apis.sh
```

### 3. 查看测试报告

测试报告保存在 `reports/` 目录下。

## 测试覆盖

| 模块 | 接口数 | 测试用例数 | 覆盖率 |
|-----|-------|----------|-------|
| Scope授权管理 | 3 | 3 | 100% |
| 消费网关 | 2 | 2 | 100% |
| **总计** | **5** | **5** | **100%** |

## 功能模块

- **Scope授权管理**：用户授权列表、创建授权、取消授权
- **消费网关**：API代理鉴权、权限校验

## 测试用例

详细的测试用例请查看 [cases/test-case-api-server.md](./cases/test-case-api-server.md)

## 测试脚本说明

### init-test-data.sh

初始化测试数据脚本，包含：
- 用户授权数据
- 应用数据
- 权限数据
- 订阅关系数据

**前置条件**：
- MySQL 数据库可访问
- 需要适当的数据库访问权限

### test-all-apis.sh

主测试脚本，执行以下测试：
1. TC-USER-AUTH-001: GET /api/v1/user-authorizations
2. TC-USER-AUTH-002: POST /api/v1/user-authorizations
3. TC-USER-AUTH-003: DELETE /api/v1/user-authorizations/:id
4. TC-GATEWAY-001: ANY /gateway/api/*
5. TC-GATEWAY-002: GET /gateway/permissions/check

## 测试报告格式

测试报告包含：
- 测试时间
- 测试数据初始化状态
- 各接口测试结果（✅ 通过 / ❌ 失败 / ⚠️ 需要数据）
- 测试统计（总数、通过、失败、通过率）

## 前置条件

1. api-server 服务正在运行（端口 18081）
2. MySQL 数据库可访问
3. 已安装 jq 工具（用于 JSON 解析）

## 相关文档

- [接口设计文档](../../plan-api.md)
- [数据库设计文档](../../plan-db.md)

---
*最后更新: 2026-04-22*
