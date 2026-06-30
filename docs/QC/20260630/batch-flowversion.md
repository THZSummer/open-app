# 批 flowversion：连接流版本管理

> 7 文件全部逐行读（FlowVersionService 917行 + Controller 212行 + Entity 102行 + Mapper 68行 + DTO3）。

## 文件覆盖表（7/7）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/FlowVersionController.java(212) | ✅ | 1 |
| dto/FlowVersionDetailResponse.java(73) | ✅ | 0 |
| dto/FlowVersionListResponse.java(37) | ✅ | 0 |
| dto/FlowVersionSaveRequest.java(18) | ✅ | 1 |
| entity/FlowVersion.java(102) | ✅ | 1 |
| mapper/OpFlowVersionMapper.java(68) | ✅ | 1 |
| service/FlowVersionService.java(917) | ✅ | 1 |

## 意见

### 意见 1
- 大类：基本代码问题 / 子类：性能和效率问题 / 级别：一般
- 问题原因：entity/FlowVersion.java:73,94 getOrchestrationConfigObj/setOrchestrationConfigObj 每次调 new ObjectMapper()（同 ConnectorVersion entity）
- 修改建议：static final ObjectMapper 复用

### 意见 2
- 大类：编程规范 / 子类：其他编程规范问题 / 级别：建议
- 问题原因：controller/FlowVersionController.java:93 `request.getOrchestrationConfig().toString()`（同 ConnectorVersionController）
- 修改建议：用 ObjectMapper 序列化

### 意见 3
- 大类：安全编码 / 子类：错误消息中暴露信息 / 级别：建议
- 问题原因：service/FlowVersionService.java:306,378,775 e.getMessage() 返客户端（updateDraft/publish/urgeApproval 多处）
- 修改建议：通用错误消息

### 意见 4
- 大类：软件结构 / 子类：其他软件结构问题 / 级别：建议
- 问题原因：mapper/OpFlowVersionMapper.java Op 前缀命名（同 Op*Mapper 系列）
- 修改建议：重命名 OpFlowVersionMapper→FlowVersionMapper

### 意见 5
- 大类：编程规范 / 子类：参数/返回值/操作优先级 / 级别：建议
- 问题原因：dto/FlowVersionSaveRequest.java:17 orchestrationConfig 无 @NotNull（Controller L93 有 null 检查兜底）
- 修改建议：加 @NotNull

## 结论
**有条件通过**（仅一般/建议级）。FlowVersionService 质量极高——应用隔离（每方法 requireInternalAppId + appId.equals 双校验）；7 状态流转（DRAFT→PENDING_APPROVAL→PUBLISHED/REJECTED/WITHDRAWN→INVALIDATED→DELETED）用 FlowVersionStatus.isValidTransition；发布 9 项校验完整（validateFlowVersionFields + validateOrchestration(publishValidator) + validateConnectorRefs + validateRateLimits）；审批联动（submitApproval/cancelApproval/urgeApproval）；syncConnectorVersionRefs 维护引用中间表（先删后批量插）；缓存失效（evictFlowConfigCache）；审计日志；@Transactional 事务完整。
