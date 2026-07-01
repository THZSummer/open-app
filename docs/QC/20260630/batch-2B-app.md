# 批 2-B：open-server / app 审查报告

> 35 文件全部逐行读完。意见按 §2.2 格式，分类按 §2.1。

## 文件覆盖表（35/35）

| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| controller/AppController.java | ✅ | 0 |
| service/AppService.java | ✅ | 0 |
| service/impl/AppServiceImpl.java | ✅ | 6 |
| service/AppCommonService.java | ✅ | 1 |
| resolver/AppContextResolver.java | ✅ | 0 |
| resolver/AppContext.java | ✅ | 0 |
| resolver/AppAccessException.java | ✅ | 0 |
| resolver/impl/StandardAppContextResolver.java | ✅ | 0 |
| resolver/impl/DevAppContextResolver.java | ✅ | 1 |
| snapshot/AppSnapshotLoader.java | ✅ | 0 |
| constants/AppPropertyConstants.java | ✅ | 0 |
| dto/CreateAppRequest.java | ✅ | 0 |
| dto/UpdateAppRequest.java | ✅ | 0 |
| dto/UpdateVerifyTypeRequest.java | ✅ | 0 |
| dto/BindEamapRequest.java | ✅ | 0 |
| entity/App.java | ✅ | 0 |
| entity/AppIdentity.java | ✅ | 0 |
| entity/AppProperty.java | ✅ | 0 |
| entity/Eamap.java | ✅ | 0 |
| enums/AppTypeEnum.java | ✅ | 0 |
| enums/AppSubTypeEnum.java | ✅ | 0 |
| enums/VerifyTypeEnum.java | ✅ | 0 |
| mapper/AppMapper.java | ✅ | 0 |
| mapper/AppIdentityMapper.java | ✅ | 0 |
| mapper/AppPropertyMapper.java | ✅ | 0 |
| mapper/EamapMapper.java | ✅ | 0 |
| vo/AppBasicInfoVO.java | ✅ | 0 |
| vo/AppIdentityVO.java | ✅ | 0 |
| vo/AppListItemVO.java | ✅ | 0 |
| vo/AppVerifyTypeVO.java | ✅ | 0 |
| vo/BindEamapVO.java | ✅ | 0 |
| vo/CreateAppVO.java | ✅ | 0 |
| vo/CurrentRoleVO.java | ✅ | 0 |
| vo/EamapVO.java | ✅ | 0 |
| vo/EmployeeInfoVO.java | ✅ | 0 |

## QC 意见（8 条）

### 意见 1
- 大类：安全编码
- 子类：序列化类包含敏感数据
- 级别：严重
- 问题原因：service/impl/AppServiceImpl.java:711-727 `encryptApiSecret`/`decryptApiSecret`/`decryptSk` 全是空实现（`return plainText`/`return cipherText`）。apiSecret 与 sk(privateKey) **明文存储**到 openplatform_app_p_t / openplatform_app_identity_t，DB 泄露即明文暴露。方法名伪装成加密，实际未加密
- 修改建议：实现真实加密（AES-GCM + KMS 托管密钥），或至少不可逆存储（若业务允许）；移除伪装方法名避免误导

### 意见 2
- 大类：安全编码
- 子类：敏感信息释放前未清理
- 级别：严重
- 问题原因：service/impl/AppServiceImpl.java:459-462 getAppIdentity `vo.setSk(decryptSk(identity.getPrivateKey()))` 返回明文 sk 给前端（decryptSk 空实现=明文）。sk 是私钥，前端持有明文 → XSS/日志/缓存泄露面扩大
- 修改建议：sk 不明文返回（掩码或仅返回是否已设置）；如必须下发，限 owner/admin 角色且走专用凭证下载接口

### 意见 3
- 大类：安全编码
- 子类：敏感信息释放前未清理
- 级别：严重
- 问题原因：service/impl/AppServiceImpl.java:478,488-490 getVerifyType `vo.setApiSecret(decryptApiSecret(...))` 返回明文 apiSecret（decrypt 空实现=明文）
- 修改建议：同意见 2，apiSecret 不明文返回

### 意见 4
- 大类：安全编码
- 子类：关键资源权限分配不当
- 级别：一般
- 问题原因：service/impl/AppServiceImpl.java:447,467 getAppIdentity/getVerifyType 经 `resolveAndValidate` 成员校验，但权限粒度仅"app 成员"——任意成员（含普通 Developer）可获取明文 sk/apiSecret。凭证应限 Owner/Admin
- 修改建议：复用 member 角色矩阵（MemberUtils.getHighestRoleMember），仅 Owner/Admin 可读凭证

### 意见 5
- 大类：安全编码
- 子类：使用不充足随机数
- 级别：一般
- 问题原因：service/impl/AppServiceImpl.java:627-631 generateAppId `timestamp + new Random().nextInt(9000)` 仅 9000 种随机，高并发创建应用 appId 碰撞
- 修改建议：appId 用雪花 idGenerator.nextId() 或扩大随机空间 + 去重校验

### 意见 6
- 大类：安全编码
- 子类：使用不充足随机数
- 级别：一般
- 问题原因：service/impl/AppServiceImpl.java:636-638 saveAppIdentity `sk = SK_PREFIX + appId + "_" + System.currentTimeMillis()`，sk 由 appId+毫秒时间戳生成，可预测。结合意见1明文存储，sk 极易被推算
- 修改建议：sk 用 SecureRandom 生成足够熵的随机串

### 意见 7
- 大类：业务功能
- 子类：功能需求遗漏
- 级别：一般
- 问题原因：service/AppCommonService.java:24-27 notifyCardService TODO 空实现（仅 log.info），应用变更(CREATE/UPDATE/BIND_EAMAP/TRANSFER_OWNER)未真正通知卡片服务
- 修改建议：对接卡片服务实现事件发送；未实现前确认下游卡片服务无依赖

### 意见 8
- 大类：业务功能
- 子类：功能需求没有正确实现
- 级别：一般
- 问题原因：resolver/impl/DevAppContextResolver.java:19 dev 环境注释"Skip validation, allow all access"；L51-55 parseInternalId 非数字 appId 兜底返回 1L（可能误指向 id=1 的应用）。需确认生产环境用 StandardAppContextResolver（@ConditionalOnProperty app.resolver.type=standard）
- 修改建议：确认生产配置 app.resolver.type=standard 启用 StandardAppContextResolver；DevAppContextResolver 仅 dev 生效

## 批次结论

- 严重：3（意见 1,2,3）
- 一般：5（意见 4,5,6,7,8）

**亮点**：AppController pageSize @Max(100) ✅（之前误判无上限，实际有）；AppServiceImpl getAppList 批量查询优化(getOwnerMap/buildEamapBoundMap/buildCurrentUserRoleMap 避免 N+1)；validateVerifyType 校验完整(白名单/枚举/互斥 SOAHeader∩SOAURL/apiSecret 格式 API_SECRET_PATTERN)；StandardAppContextResolver 成员校验；DTO @NotBlank/@Size/@NotEmpty 完整；saveApp @Transactional 事务完整。

**不放行**：意见 1(密钥假加密明文存储)、2+3(凭证明文返回) 为 CRITICAL，安全阻断项。意见 4(凭证权限粒度)应同步修。
