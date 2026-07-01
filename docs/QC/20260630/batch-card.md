# 批 card：卡片设置

> 10 文件全部逐行读（CardSettingService 202行在 2F 批读过，本轮补读 client6/controller1/dto2）。

## 文件覆盖表（10/10）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| client/CardServiceClient.java(48) | ✅ | 0 |
| client/CardServiceClientImpl.java(140) | ✅ | 0 |
| client/CardServiceClientStub.java(85) | ✅ | 0 |
| client/CardServiceError.java(44) | ✅ | 0 |
| client/CardServicePeriodDTO.java(48) | ✅ | 0 |
| client/CardServiceResponse.java(62) | ✅ | 0 |
| controller/CardSettingController.java(87) | ✅ | 0 |
| dto/CardSettingResponse.java(46) | ✅ | 0 |
| dto/UpdateCardPeriodRequest.java(70) | ✅ | 0 |
| service/CardSettingService.java(202) | ✅ | 1 |

## 意见 1
- 大类：业务功能 / 子类：功能需求遗漏 / 级别：一般
- 问题原因：service/CardSettingService.java:143-155 getCurrentTenantId 占位 TODO（从配置读 defaultTenantId），注释标 OQ-12 待人工二开
- 修改建议：实现真实 tenantId 工具类获取

## 结论
CardServiceClientImpl L57 UriComponentsBuilder 参数化拼 URL ✅（无注入）；L80/121 ResourceAccessException→502 ✅；L131-138 throwOnServiceError 透传卡片服务错误 ✅；CardServiceClientStub @ConditionalOnMissingBean（默认 Stub，配置后切真实）✅；CardSettingController 鉴权在 service 层 resolveAndValidate ✅；UpdateCardPeriodRequest @NotNull + 动态范围常量清晰 ✅；@Valid 校验。仅 tenantId TODO（已计入 2F 意见 4）。质量良好。
