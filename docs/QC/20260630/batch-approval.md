# 批 approval：open-server / approval 审查报告

> 13 文件全部逐行读（ApprovalService 962行 + ApprovalEngine 855行 + Controller 355行 + 其余）。意见按 §2.2 格式。

## 文件覆盖表（13/13）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/ApprovalController.java(355) | ✅ | 2 |
| dto/ApprovalActionResponse.java(42) | ✅ | 1 |
| dto/ApprovalFlowCreateRequest.java(52) | ✅ | 1 |
| dto/ApprovalFlowDetailResponse.java(57) | ✅ | 0 |
| dto/ApprovalFlowListResponse.java(58) | ✅ | 0 |
| dto/ApprovalFlowUpdateRequest.java(43) | ✅ | 0 |
| dto/ApprovalNodeDto.java(94) | ✅ | 0 |
| dto/BatchApprovalResponse.java(70) | ✅ | 1 |
| engine/ApprovalEngine.java(855) | ✅ | 3 |
| entity/ApprovalFlow.java(108) | ✅ | 0 |
| FlowVersionApprovalService.java(135) | ✅ | 0 |
| mapper/ApprovalFlowMapper.java(96) | ✅ | 0 |
| service/ApprovalService.java(962) | ✅ | 0 |

## QC 意见（8 条）

### 意见 1
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：controller/ApprovalController.java:169-354 #46-53（getPendingList/getApprovalDetail/approve/reject/cancel/batchApprove/batchReject/urge）**无任何鉴权注解**。任意登录用户可：查看任意审批列表/详情、同意/驳回/撤销任意审批、批量审批、催办 → 审批结果可被任意用户操纵
- 修改建议：审批操作接口加鉴权（校验当前用户是否为该审批节点的审批人；模板管理 #41-45 已有 @PlatformAdminPermission 但空校验见 C1）

### 意见 2
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：controller/ApprovalController.java:59,93,108,126,144 #41-45 审批流程模板管理用 @PlatformAdminPermission（C1 空校验）→ 审批模板 CRUD 对任意用户开放
- 修改建议：见 C1 修复（实现 PlatformAdminPermissionAspect）

### 意见 3
- 大类：基本代码问题
- 子类：多线程问题
- 级别：严重
- 问题原因：engine/ApprovalEngine.java:465,566 approve/reject 先 selectById 后 update 无锁。并发审批两线程都过 status=PENDING 校验 → 重复审批、节点跳步。与 market ApprovalEngine 同类问题
- 修改建议：乐观锁（update 带 WHERE id=? AND status=? AND current_node=?）

### 意见 4
- 大类：编程规范
- 子类：异常处理
- 级别：严重
- 问题原因：engine/ApprovalEngine.java:762 updateResourceStatus `catch(Exception e) { log.error(...) }` **吞异常不 rethrow**。审批 status 已 commit（L520 update）但资源状态更新失败时静默忽略 → 审批通过但资源未更新（数据不一致）
- 修改建议：catch 内 rethrow 或补偿（审批回滚）；不应静默吞

### 意见 5
- 大类：基本代码问题
- 子类：代码逻辑错误
- 级别：一般
- 问题原因：engine/ApprovalEngine.java:579 reject `combinedNodes.get(record.getCurrentNode())` 无越界检查（approve 有 L486 越界校验，reject 没有）
- 修改建议：reject 也加越界检查

### 意见 6
- 大类：编程规范
- 子类：参数/返回值/操作优先级
- 级别：建议
- 问题原因：dto/ApprovalFlowCreateRequest.java:22,27,36 nameCn/nameEn/code 无 @NotBlank/@Size 校验（其他模块 DTO 普遍有校验）
- 修改建议：加 @NotBlank/@Size

### 意见 7
- 大类：软件结构
- 子类：冗余重复代码
- 级别：建议
- 问题原因：dto/ApprovalActionResponse.java:37-41、BatchApprovalResponse.java:42-50 手写全参构造与 @Builder + @NoArgsConstructor 冗余
- 修改建议：删除手写构造

### 意见 8
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：建议
- 问题原因：service/ApprovalService.java:598,646 batchApprove/batchReject `e.getMessage()` 放进 FailedItem.reason 返客户端
- 修改建议：未知异常的 e.getMessage 不返客户端

## 批次结论

- 严重：4（意见 1,2,3,4）
- 一般：1（意见 5）
- 建议：3（意见 6,7,8）

**亮点**：ApprovalEngine 三级审批节点组合（resource→scene→global，L157-250）；ApprovalService 分批扫描审批人（APPROVER_FILTER_BATCH_SIZE=200，L319-355 避免 OOM）；batchApprove/batchReject 独立事务（单失败不影响其他）；urge 校验申请人身份（L681）；FlowVersionApprovalService 状态校验+调engine；ApprovalFlowMapper selectByCodeAndAppId 三级回退（应用级→平台级→全局）；ApprovalNodeDto 含 level/status/approveTime/comment/cardIds；@Transactional 事务完整。

**不放行**：意见 1（审批执行无鉴权）+ 意见 3（并发无锁）+ 意见 4（吞异常数据不一致）为上线阻断项。
