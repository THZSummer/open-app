# 批 member：open-server / member 审查报告

> 12 文件全部逐行读。意见按 §2.2 格式。

## 文件覆盖表（12/12）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/MemberController.java(98) | ✅ | 0 |
| dto/AddMemberRequest.java(26) | ✅ | 0 |
| dto/TransferOwnerRequest.java(21) | ✅ | 0 |
| entity/AppMember.java(83) | ✅ | 0 |
| enums/MemberTypeEnum.java(55) | ✅ | 0 |
| mapper/AppMemberMapper.java(71) | ✅ | 0 |
| service/MemberService.java(28) | ✅ | 0 |
| service/impl/MemberServiceImpl.java(293) | ✅ | 0 |
| snapshot/MemberSnapshotLoader.java(67) | ✅ | 0 |
| util/MemberUtils.java(36) | ✅ | 0 |
| vo/MemberVO.java(55) | ✅ | 0 |
| vo/UserDataVO.java(42) | ✅ | 0 |

## QC 意见

**无意见。**

## 批次结论

**✅ 通过。** 无问题。

**亮点**：MemberServiceImpl 角色权限矩阵标杆——`validateMemberOperationPermission`（L241-258）矩阵清晰（Developer 无权操作成员 / Admin 仅操作 Developer / Owner 可操作 Developer+Admin / Owner 只能转移）；transferOwner（L181-203）事务完整（校验操作人是 Owner → 新增目标 Owner → 删原 Owner）；addMembers 批量校验角色重复 + insertBatch 避免 N+1；fillW3Accounts 批量查 employee；MemberUtils.getHighestRoleMember 取最高角色；MemberTypeEnum 含 priority（Owner 3 > Admin 2 > Developer 1）；MemberController 5 接口 @Min/@Max(100)/@NotBlank/@Validated 校验完整 + @AuditLog；AppMemberMapper 支持批量 selectByAppIdsAndMemberType/selectByAppIdAndAccountIdsAndType/insertBatch；MemberSnapshotLoader override loadAfterData 从参数提取（ADD/TRANSFER 无 resourceId 场景）；DTO @NotEmpty/@NotNull/@NotBlank 校验；@Transactional 事务完整。

member 与 version 并列本次 QC 质量最高模块。
