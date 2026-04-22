# 测试文档目录

本目录包含能力开放平台各服务的测试用例文档。

## 文档列表

| 文档 | 服务 | 接口数 | 说明 |
|------|------|--------|------|
| [test-case-open-server.md](./test-case-open-server.md) | open-server | 51 | 分类管理、API管理、事件管理、回调管理、权限管理、审批管理 |
| [test-case-api-server.md](./test-case-api-server.md) | api-server | 5 | Scope授权管理、消费网关、权限校验 |
| [test-case-event-server.md](./test-case-event-server.md) | event-server | 2 | 事件发布、回调触发 |

## 测试覆盖

- **接口总数**: 58 个
- **测试用例数**: 58 个
- **覆盖率**: 100%

## 文档结构

每个测试用例文档包含：
- 测试环境配置
- 测试数据准备 SQL 脚本
- 正常/异常/边界测试用例
- 请求示例和预期响应
- 状态码和枚举值说明

## 相关文档

- [接口设计文档](../plan-api.md)
- [数据库设计文档](../plan-db.md)
- [API验证报告](../api-validation-report.md)

---

*最后更新: 2026-04-22*
