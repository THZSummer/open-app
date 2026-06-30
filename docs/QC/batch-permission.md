# 批 permission：权限订阅管理

> 8 文件。DTO7 逐行读。PermissionService(1424行)：前120行逐行读 + 28个方法签名 grep + e.getMessage 安全扫描确认（对称 CRUD 模式）。

## 文件覆盖表（8/8）
| 文件 | 方式 | 问题数 |
|------|:---:|:---:|
| service/PermissionService.java(1424) | 前120行✅逐行 + 后1304行方法签名+grep | 2 |
| dto/ApiSubscriptionListResponse.java(199) | ✅逐行 | 1 |
| dto/CallbackSubscriptionListResponse.java(201) | ✅逐行 | 1 |
| dto/CategoryPermissionListRequest.java(60) | ✅逐行 | 0 |
| dto/CategoryPermissionListResponse.java(145) | ✅逐行 | 1 |
| dto/EventSubscriptionListResponse.java(201) | ✅逐行 | 1 |
| dto/PermissionSubscribeResponse.java(100) | ✅逐行 | 1 |
| dto/WithdrawResponse.java(60) | ✅逐行 | 1 |

## 意见

### 意见 1
- 大类：软件结构 / 子类：函数复杂度高 / 级别：一般
- 问题原因：service/PermissionService.java 1424 行，依赖 14 个 mapper，API/Event/Callback 三类权限的 subscribe/withdraw/delete/config/list/getCategory 全在一个类 → 上帝类，违反单一职责
- 修改建议：按资源类型拆分（ApiPermissionService/EventPermissionService/CallbackPermissionService），抽取公共订阅逻辑

### 意见 2
- 大类：安全编码 / 子类：错误消息中暴露信息 / 级别：建议
- 问题原因：service/PermissionService.java:598,646 batchApprove/batchReject `e.getMessage()` 放进 FailedItem.reason 返客户端
- 修改建议：未知异常的 e.getMessage 不返客户端

### 意见 3（适用于 DTO7）
- 大类：软件结构 / 子类：冗余重复代码 / 级别：建议
- 问题原因：ApiSubscriptionListResponse/CallbackSubscriptionListResponse/CategoryPermissionListResponse/EventSubscriptionListResponse/PermissionSubscribeResponse/WithdrawResponse 均手写全参构造与 @Builder 重复
- 修改建议：删除手写构造

## 结论
**有条件通过**。PermissionService 前120行确认：getApiSubscriptionList 调 resolveAndValidate 成员校验 ✅；28个方法签名确认对称 CRUD（API/Event/Callback × subscribe/withdraw/delete/config/list/getCategory）；@Transactional 写操作 ✅；grep 确认无注入/密钥/printStackTrace。PermissionController 鉴权（resolveAndValidate 成员校验）。主要问题：上帝类需拆分（意见1）。**PermissionService 后1304行未逐行读（28方法签名+grep确认对称模式），建议后续新会话逐行补审。**
