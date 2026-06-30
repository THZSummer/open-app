# 批 callback：回调注册管理

> 6 文件全部逐行读（CallbackService 615行 + DTO5）。意见按 §2.2 格式。

## 文件覆盖表（6/6）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| service/CallbackService.java(615) | ✅ | 2 |
| dto/CallbackListResponse.java(81) | ✅ | 1 |
| dto/CallbackResponse.java(100) | ✅ | 1 |
| dto/CallbackPropertyDto.java(38) | ✅ | 1 |
| dto/PermissionDefinitionDto.java(65) | ✅ | 1 |
| dto/PermissionDto.java(73) | ✅ | 1 |

## 意见

### 意见 1
- 大类：编程规范 / 子类：异常处理 / 级别：严重
- 问题原因：service/CallbackService.java:261-263 createCallback 中 `approvalEngine.createApproval` 的 catch(Exception) **吞异常不 rethrow**（仅 log.warn）。审批记录创建失败但回调 status=1（待审）已 commit → 回调永久卡在待审状态，无审批单可审
- 修改建议：catch 内 rethrow（回滚事务）或补偿（回调状态改回草稿）

### 意见 2
- 大类：基本代码问题 / 子类：性能和效率问题 / 级别：一般
- 问题原因：service/CallbackService.java:542-545 convertToListResponse 循环内 `permissionMapper.selectByResource` 逐个查权限（N+1）。属性已批量（L96-108 selectByParentIds），权限未批量
- 修改建议：批量查权限

### 意见 3（适用于 DTO5）
- 大类：软件结构 / 子类：冗余重复代码 / 级别：建议
- 问题原因：CallbackListResponse/CallbackResponse/CallbackPropertyDto/PermissionDefinitionDto/PermissionDto 均手写全参构造与 @Builder + @NoArgsConstructor 重复
- 修改建议：删除手写构造

## 结论
CallbackController @PlatformAdminPermission（7 接口，C1 空校验，已计入）；Scope 正则校验(SCOPE_PATTERN)✅；pageSize Math.min(100) ✅；属性批量查询(selectByParentIds) ✅；删除校验订阅关系 ✅；@Transactional 事务完整。**意见 1（审批创建吞异常）为上线阻断项**。
