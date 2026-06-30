# 批 connector：open-server / connector 审查报告

> 19 文件（17 现存逐行读 + OpConnectorController/OpConnectorService 已删除确认 + ConnectorVersion.java 已迁移到 connectorversion 模块）。意见按 §2.2 格式。

## 文件覆盖表（19/19）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/ConnectorController.java(127) | ✅ | 0 |
| controller/OpConnectorController.java | ✅(已删除) | 0 |
| dto/ConnectorConfigResponse.java(59) | ✅ | 0 |
| dto/ConnectorConfigUpdateRequest.java(20) | ✅ | 0 |
| dto/ConnectorCopyResponse.java(40) | ✅ | 1 |
| dto/ConnectorCreateRequest.java(39) | ✅ | 0 |
| dto/ConnectorCreateResponse.java(53) | ✅ | 1 |
| dto/ConnectorDetailResponse.java(51) | ✅ | 0 |
| dto/ConnectorListResponse.java(57) | ✅ | 0 |
| dto/ConnectorPublishResponse.java(40) | ✅ | 1 |
| dto/ConnectorUpdateRequest.java(26) | ✅ | 1 |
| entity/Connector.java(59) | ✅ | 0 |
| entity/ConnectorVersion.java | ✅(已迁移) | 0 |
| mapper/OpConnectorMapper.java(93) | ✅ | 1 |
| mapper/OpConnectorVersionMapper.java | ✅(属connectorversion模块) | 0 |
| model/ConnectionConfig.java(53) | ✅ | 0 |
| service/ConnectorService.java(329) | ✅ | 0 |
| service/OpConnectorService.java | ✅(已删除) | 0 |
| snapshot/ConnectorSnapshotLoader.java(60) | ✅ | 0 |

## QC 意见（4 条）

### 意见 1
- 大类：软件结构
- 子类：其他软件结构问题
- 级别：建议
- 问题原因：mapper/OpConnectorMapper.java:19 保留 Op 前缀命名（OpConnectorMapper），与已删除的 OpConnectorService/OpConnectorController 不一致（那两者已删）。6 个文件仍引用 Op*Mapper
- 修改建议：重命名 OpConnectorMapper→ConnectorMapper，与重构风格对齐

### 意见 2
- 大类：编程规范
- 子类：参数/返回值/操作优先级
- 级别：建议
- 问题原因：dto/ConnectorUpdateRequest.java:15-25 nameCn/nameEn/descriptionCn/descriptionEn 无 @Size 校验（ConnectorCreateRequest 有 @Size(max=64/128/512)），更新时可传超长值
- 修改建议：与 CreateRequest 一致加 @Size

### 意见 3
- 大类：软件结构
- 子类：冗余重复代码
- 级别：建议
- 问题原因：dto/ConnectorCreateResponse.java:43-52、ConnectorPublishResponse.java:33-39、ConnectorCopyResponse.java:33-39 手写全参构造方法，与 @Builder + @NoArgsConstructor(Lombok 已生成) 冗余
- 修改建议：删除手写构造，依赖 @Builder

### 意见 4
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：一般
- 问题原因：service/ConnectorService.java:137 getConnectorList 循环内 `connectorVersionMapper.selectListByConnectorId(c.getId())` 逐个查版本（N+1）。appId 已下推 SQL 隔离，但版本查询未批量
- 修改建议：批量查版本（按 connectorIds）

## 批次结论

- 一般：1（意见 4）
- 建议：3（意见 1,2,3）

**亮点**：ConnectorService 应用隔离标杆（每方法 requireInternalAppId() + appId.equals(connector.getAppId()) 代码级校验 + OpConnectorMapper selectAll/selectList/countList 全部含 appId 参数 SQL 层隔离）；状态机完整（invalidate 校验 isValidTransition + flow 引用校验、recover 根据已发布版本定状态、delete 要求 INVALIDATED）；ConnectorController 7 接口 @AuditLog 审计；ConnectorCreateRequest @NotBlank/@Size/@NotNull 校验完整；@Transactional 事务完整。

**有条件通过**：仅一般/建议级问题，无严重。connector 模块质量高（隔离 + 状态机 + 审计均规范）。
