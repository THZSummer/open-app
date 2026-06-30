# 批 1：market-server 审查报告

> 49 文件全部逐行读完。意见按 plan §2.2 格式（大类/子类/级别/问题原因/修改建议），分类按 §2.1。

## 文件覆盖表（49/49，逐行读）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| approval/constant/ApprovalConstants.java | ✅ | 0 |
| approval/constant/AppVersionStatusEnum.java | ✅ | 0 |
| approval/controller/ApprovalController.java | ✅ | 1 |
| approval/dto/ApprovalListRequest.java | ✅ | 0 |
| approval/dto/ApprovalNodeDto.java | ✅ | 0 |
| approval/dto/ApprovalProcessRequest.java | ✅ | 0 |
| approval/engine/ApprovalEngine.java | ✅ | 2 |
| approval/entity/AbilityEntity.java | ✅ | 0 |
| approval/entity/AppEntity.java | ✅ | 0 |
| approval/entity/AppVersionEntity.java | ✅ | 0 |
| approval/entity/ApprovalFlow.java | ✅ | 0 |
| approval/entity/ApprovalLog.java | ✅ | 0 |
| approval/entity/ApprovalRecord.java | ✅ | 1 |
| approval/handler/ApprovalHandler.java | ✅ | 0 |
| approval/handler/ApprovalHandlerFactory.java | ✅ | 0 |
| approval/handler/AppVersionPublishHandler.java | ✅ | 1 |
| approval/mapper/AbilityMapper.java | ✅ | 0 |
| approval/mapper/AppMapper.java | ✅ | 0 |
| approval/mapper/ApprovalFlowMapper.java | ✅ | 0 |
| approval/mapper/ApprovalLogMapper.java | ✅ | 0 |
| approval/mapper/ApprovalRecordMapper.java | ✅ | 0 |
| approval/mapper/AppVersionMapper.java | ✅ | 0 |
| approval/service/ApprovalService.java | ✅ | 0 |
| approval/service/impl/ApprovalServiceImpl.java | ✅ | 3 |
| approval/vo/ApprovalListVo.java | ✅ | 0 |
| chatbotbindtab/client/WeContactClient.java | ✅ | 1 |
| chatbotbindtab/controller/ChatbotBindController.java | ✅ | 0 |
| chatbotbindtab/dto/ChatbotBindRequest.java | ✅ | 0 |
| chatbotbindtab/dto/WeContactRequest.java | ✅ | 0 |
| chatbotbindtab/dto/WeContactResponse.java | ✅ | 0 |
| chatbotbindtab/entity/AppPropertyEntity.java | ✅ | 0 |
| chatbotbindtab/mapper/ChatbotBindMapper.java | ✅ | 0 |
| chatbotbindtab/service/ChatbotBindService.java | ✅ | 0 |
| chatbotbindtab/service/impl/ChatbotBindServiceImpl.java | ✅ | 3 |
| chatbotbindtab/vo/ChatbotAccountVO.java | ✅ | 0 |
| dictionary/dto/DictionaryCreateDTO.java | ✅ | 0 |
| dictionary/service/DictionaryServiceImpl.java | ✅ | 1 |
| dictionary/vo/DictionaryListVO.java | ✅ | 0 |
| dictionary/vo/DictionaryVO.java | ✅ | 0 |
| lookup/dto/classify/ClassifyCreateDTO.java | ✅ | 0 |
| lookup/dto/item/ItemCreateDTO.java | ✅ | 0 |
| lookup/service/ClassifyServiceImpl.java | ✅ | 2 |
| lookup/service/LookUpItemServiceImpl.java | ✅ | 2 |
| lookup/vo/classify/ClassifyListVO.java | ✅ | 0 |
| lookup/vo/classify/ClassifyVO.java | ✅ | 0 |
| lookup/vo/item/ItemDetailVO.java | ✅ | 0 |
| lookup/vo/item/ItemListVO.java | ✅ | 0 |
| common/config/JacksonConfig.java | ✅ | 0 |
| common/config/RestTemplateConfig.java | ✅ | 1 |

## QC 意见（16 条）

### 意见 1
- 大类：基本代码问题
- 子类：多线程问题
- 级别：严重
- 问题原因：ApprovalEngine.java:54-114 `process()` 先 `selectById`(L56) 后 `update`(L95/102/109)，中间无乐观锁/SELECT FOR UPDATE。并发审批（双击/重试）两线程都过 status=PENDING 校验→重复审批、节点跳2步、log插2条
- 修改建议：update SQL 带 `WHERE id=? AND status=? AND current_node=?`，根据 affectedRows 判断是否抢占失败；或对记录加行锁

### 意见 2
- 大类：基本代码问题
- 子类：代码逻辑错误
- 级别：严重
- 问题原因：ApprovalEngine.java:69-70 `parseNodes` 后直接 `nodes.get(record.getCurrentNode())`，未校验 nodes 非空、currentNode 未越界。combinedNodes JSON 异常或 currentNode 错位→IndexOutOfBoundsException
- 修改建议：get 前校验 `!nodes.isEmpty() && record.getCurrentNode()>=0 && record.getCurrentNode()<nodes.size()`，否则抛业务异常

### 意见 3
- 大类：编程规范
- 子类：异常处理
- 级别：验证
- 问题原因：ApprovalServiceImpl.java:50-107、111-163 整方法包 `catch(Exception)`，单条 record 的 businessId 坏→整页 500，非跳过坏数据返回部分结果
- 修改建议：循环内单条 try-catch 跳过坏数据，记 warn 日志，返回有效部分

### 意见 4
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：严重
- 问题原因：ApprovalServiceImpl.java:59-93、120-149 列表循环内逐条 selectById(version/app)+selectVersionAbilityIds+selectByIds(ability)。pageSize=10 时约 40 次查询
- 修改建议：先收集所有 versionId/abilityIds，批量 selectByIds，内存整合

### 意见 5
- 大类：安全编码
- 子类：错误消息中暴露信息
- 级别：严重
- 问题原因：ApprovalServiceImpl.java:177 `"审批操作失败："+e.getMessage()`，未知异常 message 直接返客户端，可能含 SQL/内部路径/库细节
- 修改建议：未知异常只返通用"审批操作失败"，e.getMessage() 仅写日志

### 意见 6
- 大类：业务功能
- 子类：功能需求没有正确实现
- 级别：一般
- 问题原因：entity/ApprovalRecord.java:22 `businessId` 为 String，而 open-server 同表 entity 为 Long（spec §1.3 声称字段同构）。导致 ServiceImpl/handler 内 3 处 `Long.parseLong(record.getBusinessId())` 手动转换
- 修改建议：market ApprovalRecord.businessId 改 Long 与 open 同构，移除手动 parseLong

### 意见 7
- 大类：软件结构
- 子类：冗余重复代码
- 级别：一般
- 问题原因：AppVersionPublishHandler.java:31,39 `Long.parseLong(record.getBusinessId())` 重复解析，与 ApprovalServiceImpl:68 重复
- 修改建议：统一在 ApprovalRecord 或调用入口解析一次为 Long

### 意见 8
- 大类：编程规范
- 子类：其他编程规范问题
- 级别：建议
- 问题原因：ApprovalController.java:36-37,49-50 pageSize 无上限校验，可传大值致大查询
- 修改建议：加 @Max(50) 或服务层 clamp

### 意见 9
- 大类：基本代码问题
- 子类：多线程问题
- 级别：严重
- 问题原因：ChatbotBindServiceImpl.java:98-101 `countBoundAccounts >= maxCount` 校验与 insert(L121) 之间无锁，并发绑定可超额
- 修改建议：加唯一约束(appPkId+propertyName+accountId)或行锁，或 DB 层计数+事务

### 意见 10
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：严重
- 问题原因：ChatbotBindServiceImpl.java:119 `entity.setTenantId("")`，注释自标 #ASSUMED。若系统多租户→租户数据隔离失效
- 修改建议：从应用记录继承真实 tenantId；确认系统是否多租户，若是必须修

### 意见 11
- 大类：基本代码问题
- 子类：资源使用问题
- 级别：一般
- 问题原因：ChatbotBindServiceImpl.java:147 unbind 物理删除(deleteByAppPkIdAndAccountId)，无软删除，审计/误删难追溯
- 修改建议：改软删除(status=0)或保留删除但补审计日志表

### 意见 12
- 大类：安全编码
- 子类：硬编码安全相关的常量
- 级别：验证
- 问题原因：WeContactClient.java:60 token 作 Authorization 调外部 API；ChatbotBindController.java:50-51 token 从 yml `wecontact.token` 注入。token 是长期凭证配置在文件
- 修改建议：确认 token 管理流程；生产应通过凭证管理服务动态获取，非明文配置；yml 中 token 需加密/环境变量

### 意见 13
- 大类：基本代码问题
- 子类：资源使用问题
- 级别：一般
- 问题原因：ClassifyServiceImpl.java:188 `deleteClassify` 调 `lookUpItemMapper.deleteByClassifyId` 级联硬删除分类下所有 item，无确认/无软删，误删风险
- 修改建议：改级联软删除，或要求分类下无 item 才允许删

### 意见 14
- 大类：软件结构
- 子类：冗余重复代码
- 级别：一般
- 问题原因：dictionary/DictionaryServiceImpl 与 lookup/ClassifyServiceImpl、LookUpItemServiceImpl 的 CRUD 结构 + convertToListVO/convertToVO 几乎一致（分页/模糊拼接/状态校验/缓存清理模板相同）
- 修改建议：抽取 AbstractCrudService 基类或通用转换工具，减少重复

### 意见 15
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：一般
- 问题原因：LookUpItemServiceImpl.java:101,139 createItem 内 classifyMapper.selectById 查两次（校验 L101 + 缓存清理前 L139）
- 修改建议：复用第一次查询结果，移除重复 selectById

### 意见 16
- 大类：基本代码问题
- 子类：性能和效率问题
- 级别：一般
- 问题原因：RestTemplateConfig.java:21 用 SimpleClientHttpRequestFactory，不支持连接池，高并发外部调用（WeContactClient）性能差且可能耗尽句柄
- 修改建议：换 OkHttpClientHttpRequestFactory 或 Apache HttpComponents，配置连接池

## 批次结论

- 严重：6（意见 1,2,4,5,9,10）
- 验证：2（意见 3,12）
- 一般：7（意见 6,7,11,13,14,15,16）
- 建议：1（意见 8）

**亮点**：DTO/VO/entity/constant 规范（@NotBlank/@Size/@Pattern 校验完整、@JsonIgnoreProperties 容错、mapper 注释明确禁 SELECT *、ApprovalHandlerFactory 策略模式 @PostConstruct 自动注册、ApprovalProcessRequest @NotNull/@NotBlank）。问题集中在 approval 引擎的并发/健壮性与 chatbotbindtab 的多租户/竞态。

**不放行**：意见 1（审批并发无锁）、意见 9（绑定竞态）、意见 10（tenantId 空）为上线阻断项，需修复。
