# 批 auditlog：审计日志

> 4 文件全部逐行读。

## 文件覆盖表（4/4）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/OperateLogController.java(116) | ✅ | 1 |
| mapper/OperateLogMapper.java(57) | ✅ | 0 |
| service/AuditLogService.java(59) | ✅ | 0 |
| vo/OperateLogVO.java(33) | ✅ | 1 |

## 意见 1
- 大类：安全编码 / 子类：关键资源权限分配不当 / 级别：一般
- 问题原因：controller/OperateLogController.java:38-81 getOperateLogList 无鉴权注解。审计日志含操作人/IP/操作描述等敏感信息，任意登录用户可查询
- 修改建议：加 @PlatformAdminPermission（实现后）或登录态校验

## 意见 2
- 大类：编程规范 / 子类：其他编程规范问题 / 级别：建议
- 问题原因：vo/OperateLogVO.java:18 `private Long id`，其他 VO 统一用 String（防 JS 精度丢失），此处不一致
- 修改建议：改为 String

## 结论
AuditLogService @Async + REQUIRES_NEW 独立事务 ✅（审计失败不影响主业务）；catch 异常仅 log.error（设计合理）。OperateLogController 分页查询+筛选选项。无严重问题。
