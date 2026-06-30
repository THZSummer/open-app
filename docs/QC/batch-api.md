# 批 api：API 注册管理

> 5 文件全部逐行读（ApiService 636行 + DTO4）。与 CallbackService 同构模式。

## 文件覆盖表（5/5）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| service/ApiService.java(636) | ✅ | 3 |
| dto/ApiDetailResponse.java(121) | ✅ | 1 |
| dto/ApiListResponse.java(113) | ✅ | 1 |
| dto/PermissionDto.java(87) | ✅ | 1 |
| dto/PropertyDto.java(42) | ✅ | 1 |

## 意见

### 意见 1
- 大类：编程规范 / 子类：异常处理 / 级别：严重
- 问题原因：service/ApiService.java:247-249 createApi 审批创建 catch(Exception) **吞异常不 rethrow**（同 CallbackService 意见 1）。API 卡待审无审批单
- 修改建议：rethrow 或补偿

### 意见 2
- 大类：基本代码问题 / 子类：性能和效率问题 / 级别：一般
- 问题原因：service/ApiService.java:537-540 convertToListResponse 循环内 selectByResource N+1（同 CallbackService 意见 2）
- 修改建议：批量查权限

### 意见 3
- 大类：编程规范 / 子类：其他编程规范问题 / 级别：建议
- 问题原因：service/ApiService.java:76 pageSize 默认 20 但无 Math.min 上限（CallbackService 有 Math.min(100)）
- 修改建议：加 Math.min(pageSize, 100)

### 意见 4（适用于 DTO4）
- 大类：软件结构 / 子类：冗余重复代码 / 级别：建议
- 问题原因：ApiDetailResponse/ApiListResponse/PermissionDto/PropertyDto 均手写全参构造与 @Builder + @NoArgsConstructor 冗余
- 修改建议：删除手写构造

## 结论
ApiController @PlatformAdminPermission（6 接口，C1 空校验）；SCOPE_PATTERN 正则校验 ✅；属性批量查询 ✅；删除校验订阅关系 ✅；@Transactional 事务完整。**意见 1（审批创建吞异常）为上线阻断项**（与 callback 同类）。
