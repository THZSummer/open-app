# API 测试脚本说明

本目录包含能力开放平台所有接口的测试脚本，共 58 个接口。

## 目录结构

```
api-test-scripts/
├── README.md                          # 本说明文档
├── run-all.sh                         # 汇总执行脚本
├── config.sh                          # 配置文件（BASE_URL等）
├── category/                          # 分类管理 (8个接口)
├── api/                               # API管理 (6个接口)
├── event/                             # 事件管理 (6个接口)
├── callback/                          # 回调管理 (6个接口)
├── api-permission/                    # API权限管理 (4个接口)
├── event-permission/                  # 事件权限管理 (5个接口)
├── callback-permission/               # 回调权限管理 (5个接口)
├── approval/                          # 审批管理 (11个接口)
├── user-authorization/                # 用户授权管理 (3个接口)
└── gateway/                           # 消费网关 (4个接口)
```

## 使用方法

### 1. 修改配置

编辑 `config.sh`，根据实际环境修改：

```bash
export BASE_URL="http://localhost:18080/open-server"
export CATEGORY_ID="1"
export API_ID="100"
# ... 其他变量
```

### 2. 执行单个脚本

```bash
# 进入测试目录
cd open-server/src/test/api-test-scripts

# 加载配置
source config.sh

# 执行单个脚本
bash category/01-get-categories.sh
```

### 3. 执行所有脚本

```bash
# 执行汇总脚本
bash run-all.sh
```

## 模块说明

### 分类管理 (category/)
- 01-get-categories.sh - 获取分类列表（树形）
- 02-get-category-by-id.sh - 获取分类详情
- 03-create-category.sh - 创建分类
- 04-update-category.sh - 更新分类
- 05-delete-category.sh - 删除分类
- 06-add-category-owner.sh - 添加分类责任人
- 07-get-category-owners.sh - 获取分类责任人列表
- 08-remove-category-owner.sh - 移除分类责任人

### API管理 (api/)
- 09-get-apis.sh - 获取API列表
- 10-get-api-by-id.sh - 获取API详情
- 11-create-api.sh - 注册API
- 12-update-api.sh - 更新API
- 13-delete-api.sh - 删除API
- 14-withdraw-api.sh - 撤回审核中的API

### 事件管理 (event/)
- 15-get-events.sh - 获取事件列表
- 16-get-event-by-id.sh - 获取事件详情
- 17-create-event.sh - 注册事件
- 18-update-event.sh - 更新事件
- 19-delete-event.sh - 删除事件
- 20-withdraw-event.sh - 撤回审核中的事件

### 回调管理 (callback/)
- 21-get-callbacks.sh - 获取回调列表
- 22-get-callback-by-id.sh - 获取回调详情
- 23-create-callback.sh - 注册回调
- 24-update-callback.sh - 更新回调
- 25-delete-callback.sh - 删除回调
- 26-withdraw-callback.sh - 撤回审核中的回调

### API权限管理 (api-permission/)
- 27-get-app-apis.sh - 获取应用API权限列表
- 28-get-category-apis.sh - 获取分类下API权限列表
- 29-subscribe-apis.sh - 申请API权限
- 30-withdraw-api-subscription.sh - 撤回API权限申请

### 事件权限管理 (event-permission/)
- 31-get-app-events.sh - 获取应用事件订阅列表
- 32-get-category-events.sh - 获取分类下事件权限列表
- 33-subscribe-events.sh - 申请事件权限
- 34-config-event.sh - 配置事件消费参数
- 35-withdraw-event-subscription.sh - 撤回事件权限申请

### 回调权限管理 (callback-permission/)
- 36-get-app-callbacks.sh - 获取应用回调订阅列表
- 37-get-category-callbacks.sh - 获取分类下回调权限列表
- 38-subscribe-callbacks.sh - 申请回调权限
- 39-config-callback.sh - 配置回调消费参数
- 40-withdraw-callback-subscription.sh - 撤回回调权限申请

### 审批管理 (approval/)
- 41-get-approval-flows.sh - 获取审批流程模板列表
- 42-get-approval-flow-by-id.sh - 获取审批流程模板详情
- 43-create-approval-flow.sh - 创建审批流程模板
- 44-update-approval-flow.sh - 更新审批流程模板
- 45-get-pending-approvals.sh - 获取待审批列表
- 46-get-approval-by-id.sh - 获取审批详情
- 47-approve.sh - 同意审批
- 48-reject.sh - 驳回审批
- 49-cancel-approval.sh - 撤销审批
- 50-batch-approve.sh - 批量同意审批
- 51-batch-reject.sh - 批量驳回审批

### 用户授权管理 (user-authorization/)
- 52-get-user-authorizations.sh - 获取用户授权列表
- 53-create-user-authorization.sh - 用户授权
- 54-delete-user-authorization.sh - 取消授权

### 消费网关 (gateway/)
- 55-gateway-api.sh - API请求代理与鉴权
- 56-publish-event.sh - 事件发布接口
- 57-invoke-callback.sh - 回调触发接口
- 58-check-permission.sh - 权限校验接口

## 响应格式说明

所有接口统一响应格式：

```json
{
  "code": "200",
  "messageZh": "操作成功",
  "messageEn": "Success",
  "data": { ... },
  "page": { ... }  // 分页接口才有
}
```

## 注意事项

1. 脚本中的变量（如 `$CATEGORY_ID`）需要在 `config.sh` 中设置或手动替换
2. 需要认证的接口请配置正确的 `AUTH_HEADER`
3. 执行前确保服务已启动且可访问
4. 批量操作脚本请谨慎使用，避免误操作

## 接口文档参考

详细接口文档请参考：
`.sddu/specs-tree-root/specs-tree-capability-open-platform/plan-api.md`