# open-app 安全模型 — 安全策略文档

> **文档定位**: sddu-docs-security — 安全策略文档 — 认证流程、授权矩阵、安全边界  
> **输出文件名**: security.md  
> **数据来源**: 代码扫描生成 — 各服务配置/代码 + specs-tree 基线  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 认证流程

### 1.1 管理面认证（open-server / market-server）

| 方式 | 说明 |
|------|------|
| 企业内部认证 | Cookie / SSO 登录（platform 平台） |
| 内部凭证 | internal.auth（api-server 配置 bypass=false，白名单 allowed-accounts） |

### 1.2 数据面认证（消费网关）

| 方式 | 认证头 | 说明 |
|------|--------|------|
| SOA | X-SOA-TOKEN | 平台 SOA 凭证 |
| APIG | X-APIG-APPID / X-APIG-APPKEY | API 网关凭证 |
| AKSK | X-AKSK-TOKEN | AK/SK 派生 Token |
| CLITOKEN | — | 命令行凭证（v2_api_t auth_type=6） |
| 免认证 | — | auth_type=4 公共资源 |

### 1.3 授权模型

| 模型 | 说明 |
|------|------|
| 资源注册审批 | API/事件/回调注册需审批（approval_flow_t 模板） |
| 订阅授权 | 消费方订阅需审批（subscription_t 状态机） |
| 用户授权 | OAuth 风格用户主动授权（user_authorization_t + ScopeController） |
| 成员权限 | 应用成员 Owner/管理员/开发者角色矩阵 |

## 2. 授权矩阵

### 2.1 应用成员角色（open-server）

| 角色 | 能力 |
|------|------|
| Owner | 全部权限 + 转移 Owner |
| 管理员 | 管理 + 成员增删 |
| 开发者 | 查看 + 提交 |
| 非成员 | 无权限 |

### 2.2 资源审批状态机

```
API/事件/回调资源: 草稿(0) → 待审(1) → 已发布(2) → 已下线(3)
订阅关系: 待审(0) → 已授权(1) → 已拒绝(2) → 已取消(3)
审批记录: 待审(0) → 已通过(1) → 已拒绝(2) → 已撤销(3)
```

## 3. 安全边界

| 边界 | 机制 |
|------|------|
| 连接器执行 | 仅执行已发布版本（deployed_version_id 指针） |
| 脚本执行 | GraalJS 沙箱（IO/线程/进程/Native/环境变量关闭，statementLimit=10000） |
| 限流 | Redis 令牌桶，入站 429 |
| 缓存 | 全流/节点级缓存，防重放 |
| 操作审计 | operate_log_t（操作人/IP/前后数据） |
| 密钥管理 | app_identity_t（AK/SK + key_version 轮换） |

## 4. 安全风险提示（代码扫描发现）

| 风险 | 位置 | 建议 |
|------|------|------|
| 应用私钥明文存储 | openplatform_app_identity_t.private_key | 评估加密/密钥服务 |
| dev 环境凭证硬编码 | application-dev.yml（openapp/openapp） | 生产已环境变量化，dev 可接受 |
| wecontact.token 占位 | market-server application.yml | 生产需注入 |
| CORS/CSRF | 未发现全局 CORS 配置 | 前端同源部署或显式配置 |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
