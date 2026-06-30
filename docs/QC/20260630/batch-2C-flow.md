# 批 2-C：open-server / flow 审查报告

> 21 文件（17 现存逐行读 + 4 已删除确认：OpFlowController/OpFlowService/FlowVersion/OpFlowVersionMapper）。意见按 §2.2 格式。

## 文件覆盖表（21/21）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/FlowController.java | ✅ | 1 |
| controller/OpFlowController.java | ✅(已删除) | 0 |
| dto/FlowConfigUpdateRequest.java | ✅ | 0 |
| dto/FlowCopyResponse.java | ✅ | 0 |
| dto/FlowCreateResponse.java | ✅ | 0 |
| dto/FlowDeployRequest.java | ✅ | 0 |
| dto/FlowDeployResponse.java | ✅ | 0 |
| dto/FlowDetailResponse.java | ✅ | 0 |
| dto/FlowLifecycleResponse.java | ✅ | 0 |
| dto/FlowListResponse.java | ✅ | 0 |
| dto/FlowPublishResponse.java | ✅ | 0 |
| entity/Flow.java | ✅ | 0 |
| entity/FlowVersion.java | ✅(已删除) | 0 |
| mapper/OpFlowMapper.java | ✅ | 1 |
| mapper/OpFlowVersionMapper.java | ✅(已删除) | 0 |
| service/FlowCopyService.java | ✅ | 1 |
| service/FlowDeployService.java | ✅ | 0 |
| service/FlowService.java | ✅ | 1 |
| service/OpFlowService.java | ✅(已删除) | 0 |
| snapshot/FlowSnapshotLoader.java | ✅ | 0 |
| validator/FlowPublishValidator.java | ✅ | 1 |

## QC 意见（4 条）

### 意见 1
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：建议
- 问题原因：validator/FlowPublishValidator.java:73 `"编排配置 JSON 格式无效：" + e.getMessage()`，Jackson 解析异常的 message 返客户端，可能含 JSON 内部结构细节
- 修改建议：返回通用"编排配置 JSON 格式无效"，e.getMessage() 仅写日志

### 意见 2
- 大类：基本代码问题
- 子类：代码逻辑错误
- 级别：一般
- 问题原因：service/FlowCopyService.java:138-152 generateCopyName 重试循环(L141 for 5次)每次生成新名但**未查 DB 碰撞**（L145 注释"不做严格碰撞检测"），循环形同虚设，最终返回最后一次随机名。复制名仍可能碰撞
- 修改建议：循环内查 flowMapper 名称是否存在，碰撞才重试；或 DB 加唯一约束

### 意见 3
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：一般
- 问题原因：service/FlowService.java:136,182 getFlowList/getFlowDetail 循环内 `flowVersionMapper.selectListByFlowId` 逐个查版本（appId 已下推 SQL 隔离，但版本查询未批量）
- 修改建议：列表场景批量查版本（按 flowIds）

### 意见 4
- 大类：软件结构
- 子类：其他软件结构问题
- 级别：建议
- 问题原因：mapper/OpFlowMapper.java（及 OpFlowVersionMapper）保留 Op 前缀命名，与已删除的 OpFlowService/OpFlowController 不一致（那两者已删，mapper 未改名）。命名混乱
- 修改建议：重命名 OpFlowMapper→FlowMapper，与重构风格对齐

## 批次结论

- 一般：2（意见 2,3）
- 建议：2（意见 1,4）

**亮点**：FlowService/FlowDeployService/FlowCopyService 应用隔离到位（每个方法 AppContextHolder.requireInternalAppId() + appId.equals(flow.getAppId()) 双校验，与 connector 同为标杆）；生命周期状态机用 FlowLifecycleStatus.isValidTransition()；FlowPublishValidator 9 项发布校验完整（配置大小/节点/并行分支/缓存TTL/限流/超时/脚本长度/GraalVM JS 语法预检/连接器版本引用），GraalVM JS 沙箱安全（statementLimit+无 HostAccess）；缓存失效（evictFlowConfigCache 部署/删除时清 Redis）；FlowCopyService 复制不继承部署状态；@Transactional 事务完整；FlowDeployRequest @NotNull 校验。

**有条件通过**：仅一般/建议级问题，无严重。flow 模块质量高（隔离+状态机+校验器均规范）。
