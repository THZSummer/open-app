# 批 connectorversion：open-server / connectorversion 审查报告

> 9 文件全部逐行读。意见按 §2.2 格式。

## 文件覆盖表（9/9）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/ConnectorVersionController.java(167) | ✅ | 1 |
| dto/ConnectorVersionDetailResponse.java(41) | ✅ | 0 |
| dto/ConnectorVersionListResponse.java(34) | ✅ | 0 |
| dto/ConnectorVersionSaveRequest.java(20) | ✅ | 0 |
| entity/ConnectorVersion.java(102) | ✅ | 1 |
| entity/ConnectorVersionRef.java(51) | ✅ | 0 |
| mapper/ConnectorVersionRefMapper.java(76) | ✅ | 0 |
| mapper/OpConnectorVersionMapper.java(68) | ✅ | 1 |
| service/ConnectorVersionService.java(648) | ✅ | 1 |

## QC 意见（4 条）

### 意见 1
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：一般
- 问题原因：entity/ConnectorVersion.java:73,94 getConnectionConfigObj/setConnectionConfigObj **每次调用 new ObjectMapper()**。ObjectMapper 创建开销大（线程安全应复用），每次序列化/反序列化都 new → 性能差
- 修改建议：ObjectMapper 声明为 static final 复用，或注入 Spring Bean

### 意见 2
- 大类：编程规范
- 子类：其他编程规范问题
- 级别：建议
- 问题原因：controller/ConnectorVersionController.java:88 `request.getConnectionConfig().toString()`。JsonNode.toString() 对 ObjectNode 通常输出 JSON，但非 API 契约保证。更稳妥用 objectMapper.writeValueAsString 或 treeToValue
- 修改建议：用 ObjectMapper 序列化 JsonNode 为标准 JSON 字符串

### 意见 3
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：建议
- 问题原因：service/ConnectorVersionService.java:214,270,566 `"连接配置 JSON 格式无效："+e.getMessage()` 返客户端（多处）。Jackson 异常细节暴露
- 修改建议：通用"JSON 格式无效"，e.getMessage 仅日志

### 意见 4
- 大类：软件结构
- 子类：其他软件结构问题
- 级别：建议
- 问题原因：mapper/OpConnectorVersionMapper.java:16 Op 前缀命名（同 OpConnectorMapper），与已删除的 Op*Service/Controller 不一致
- 修改建议：重命名 OpConnectorVersionMapper→ConnectorVersionMapper

## 批次结论

- 一般：1（意见 1）
- 建议：3（意见 2,3,4）

**亮点**：ConnectorVersionService 应用隔离完整（每方法 requireInternalAppId + appId.equals 双校验，同 connector 标杆）；版本状态机完整（createDraft 校验上限+已有草稿、publish 校验草稿+配置+JSON+大小+URL白名单+平台正则、invalidate 校验引用、recover/delete 校验状态）；版本号 selectMaxVersionNumberByConnectorId+1 有兜底；发布写审计日志(saveAsync)；URL 白名单 + 平台正则校验（防 SSRF）；ConnectorVersionSaveRequest @NotNull；ConnectorVersionRefMapper 支持批量 insertBatch；@Transactional 事务完整。

**有条件通过**：仅一般/建议级，无严重。质量高（隔离+状态机+发布校验+URL白名单）。
