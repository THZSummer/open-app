# 批 sync：数据同步

> 4 文件全部逐行读（sync 模块仅改动 4 个 DTO，SyncService 未在改动范围）。

## 文件覆盖表（4/4）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| dto/EmergencyDetail.java(48) | ✅ | 1 |
| dto/EmergencyResult.java(55) | ✅ | 1 |
| dto/SyncDetail.java(54) | ✅ | 1 |
| dto/SyncResult.java(49) | ✅ | 1 |

## 意见 1（适用于全部4文件）
- 大类：软件结构 / 子类：冗余重复代码 / 级别：建议
- 问题原因：EmergencyDetail/EmergencyResult/SyncDetail/SyncResult 均有 @Builder + @NoArgsConstructor + **手写全参构造**（L42-47/L48-54/L47-53/L43-48），与 Lombok 生成重复
- 修改建议：删除手写构造，依赖 @Builder

## 结论
✅ 纯 DTO 模块，无业务逻辑，无安全问题。仅手写构造冗余。
