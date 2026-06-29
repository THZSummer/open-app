# 阶段 0 快扫报告 — 坏味道热点

> 阶段 0 产出。对 6.10 后改动的 java 文件做 8 类坏味道快扫，为 review-work 圈定深审重点。

## 元信息

| 项 | 值 |
|----|-----|
| 扫描日期 | 2026-06-29 |
| 基线 | `4650daed`（2026-06-10）→ HEAD |
| 扫描范围 | 6.10 后改动的 **298 个现存 java 文件**（market-server 49 + open-server 264，扣除 15 个已删除） |
| 方法 | `Select-String`（grep 正则）限定改动文件清单 |

## 结果总表

| # | 坏味道类别 | 首轮命中 | 误报剔除 | 真实命中 | 判定 |
|---|-----------|---------|---------|---------|------|
| #1 | 硬编码密钥 | 3 | 3（属性名/变量名/方法名） | **0** | ✅ 干净 |
| #2 | System.out / printStackTrace | 0 | — | **0** | ✅ 干净 |
| #3 | 空 catch（单行） | 0 | — | **0** | ✅ 干净 |
| #4 | SQL 拼接信号 | 5 | 5（全是 i18n 错误消息拼接） | **0** | ✅ 干净 |
| #5 | TODO/FIXME/HACK | 1169 | 1162（包名 `com.xxx` 误匹配 `XXX`） | **7** | 🟡 少量技术债 |
| #6 | 硬编码 http(s):// | 17 | 16（测试桩）+1（@Value 默认值） | **0 生产** | ✅ 干净 |
| #7 | @SuppressWarnings | 7 | — | 7 | 🟡 待看压制内容 |
| #8 | new Random() | 3 | — | 3（1 处有碰撞风险） | 🟠 质量隐患 |

### 误报剔除说明（避免未来重蹈）

- **#5 的 `XXX` 大小写不敏感**：匹配到包路径 `com.xxx.it.works.wecode`（每个文件的 package/import 都含 `xxx`）。**修正**：TODO 类扫描必须 `-CaseSensitive` 且用 `TODO|FIXME|HACK`（去掉 XXX 或加单词边界）。
- **#1 密钥正则过宽**：`apiSecret`/`apiKey` 等变量名、`PROP_API_SECRET="api_secret"` 属性名常量被误判。真实硬编码密钥值应为 0。
- **#4 SQL 正则误报**：错误消息字符串拼接（`"subscribed by "+count`）被误判为 SQL 拼接。实际 SQL 用 MyBatis，无字符串拼接注入。

## 真实热点详情

### 🟠 #8 — AppServiceImpl.generateAppId() 碰撞风险（最值得跟进）

```java
// open-server/.../modules/app/service/impl/AppServiceImpl.java:590
private String generateAppId() {
    String timestamp = LocalDateTime.now().format(...APP_ID_DATE_FORMAT);
    int random = new Random().nextInt(9000) + 1000; // 1000~9999  ← 仅 9000 种
    return timestamp + random;
}
```

- **问题**：appId 是应用业务唯一标识，随机空间仅 9000，高并发创建应用时 `时间戳相同 + random 撞` → appId 重复。
- **性质**：质量/健壮性（非安全，appId 非秘密）。
- **建议**：改用雪花 ID，或扩大随机空间 + 去重校验。
- **批次归属**：阶段 2 **批 2-B（app）**，review-work Agent3 重点查。

其余 2 处 Random：`FileV2Service:114`（文件 ID 后缀，碰撞影响小）、`FlowCopyService:139`（流复制，待看上下文）。

### 🟡 #5 — TODO/FIXME/HACK（7 处 / 5 文件）

| 文件 | 数 | 说明 |
|------|---|------|
| modules/card/service/CardSettingService.java | 3 | 卡片设置 |
| modules/app/service/AppCommonService.java | 1 | 应用通用 |
| **common/security/PlatformAdminPermissionAspect.java** | 1 | 🔴 安全相关，需看 TODO 内容 |
| common/user/strategy/impl/StandardUserStrategy.java | 1 | 用户策略 |
| modules/ability/service/impl/AbilityServiceImpl.java | 1 | 能力 |

> `PlatformAdminPermissionAspect` 的 TODO 在安全切面里，review-work 批 2-A（common）Agent4 安全维度要看具体内容。

### 🟡 #7 — @SuppressWarnings（7 处 / 6 文件）

| 文件 | 数 |
|------|---|
| common/snapshot/EntitySnapshotLoaderTest.java | 2（测试） |
| modules/flowversion/service/FlowVersionService.java | 1 |
| common/util/CommonUtils.java | 1 |
| modules/flowversion/controller/FlowVersionController.java | 1 |
| modules/connectorversion/controller/ConnectorVersionController.java | 1 |

> 待 review-work 看具体压制了什么警告（`unchecked`/`rawtypes` 等），判断是否合理。

## 结论与对后续 review-work 的影响

1. **代码库质量基线高**：无硬编码密钥、无 System.out、无空 catch、无 SQL 拼接注入、无生产硬编码 URL。安全维度的"低悬果实"基本没有。
2. **快扫抓到的真实问题很少**（TODO 7、Random 碰撞 1、SuppressWarnings 7），**不足以单独决定 review-work 优先级**。
3. **review-work 价值重心调整**：从"抓坏味道"转向——
   - **逻辑正确性**（审批引擎、流程编排等复杂逻辑）
   - **架构一致性**（共享 DB 双写、重构删除 -4955 行是否引入回归）
   - **规范符合性**（对照 spec、API 契约、异常规范）
4. **建议**：维持原计划，进入阶段 1（market-server）→ 阶段 2（open-server 6 批）。上述热点在对应批次里作为 Agent3/Agent4 的必查项注入。
