# 批次 1-M：market-server 审查报告

> 阶段 1 产出。审查方式：主 agent 亲自按 review-work 5 维度框架审查（环境无 oracle subagent）。

## 元信息

| 项 | 值 |
|----|-----|
| 范围 | market-server 49 文件（approval 25 / chatbotbindtab 10 / lookup 8 / dictionary 4 / common 2） |
| 审查深度 | 核心 service/engine/handler/controller/mapper.xml/entity 全读；DTO/VO/constant 扫描 |
| spec 依据 | `docs/market-server/app-version-approval-spec.md` v10.1 |
| 审查日期 | 2026-06-29 |

## 总裁定：❌ FAIL（需修复后复审）

发现 **8 个 MAJOR** 阻塞问题（正确性/并发/性能/健壮性），无 CRITICAL。代码结构清晰、SQL 规范遵守好、无注入，但存在并发安全与若干逻辑隐患。

---

## 维度 1：Goal / Spec 符合性

| # | 项 | 结论 | 证据 |
|---|---|------|------|
| 1.1 | 3 个 API 端点 | ✅ 符合 | `ApprovalController` pending/publish/approval，路径 `/service/open/v2/apps` |
| 1.2 | SQL 规范（禁 SELECT *、JOIN≤3、子查询≤3） | ✅ 符合 | 全 mapper xml 显式列字段；selectPublishedList 2 JOIN+1 子查询，在限内 |
| 1.3 | Entity 字段同构 | ⚠️ 字段集一致，**类型不一致** | 见阻塞问题 #2 |
| 1.4 | 单应用单待审 | ➖ 不在本服务 | 由 open-server 发起侧保证 |

## 维度 2：代码质量

| 严重级 | 问题 | 位置 | 说明 |
|--------|------|------|------|
| 🔴 MAJOR | **审批并发无锁** | `ApprovalEngine.java:54-114` | `process()` 先 selectById 后 update，中间无乐观锁/`SELECT FOR UPDATE`。并发审批（双击/重试）→ 两线程都过 status=PENDING 校验 → 重复审批、节点跳 2 步、log 插 2 条 |
| 🔴 MAJOR | **审批节点越界未校验** | `ApprovalEngine.java:69-70` | `nodes.get(record.getCurrentNode())` 前未校验 nodes 非空、currentNode 未越界。combinedNodes JSON 异常或 currentNode 错位 → IndexOutOfBoundsException |
| 🔴 MAJOR | **N+1 查询** | `ApprovalServiceImpl.java:59-93, 120-149` | 列表循环内逐条 selectById（version/app/ability）+selectVersionAbilityIds。pageSize=10 时约 40 次查询。spec 要求"单表查询+Service整合"，应批量预取后内存 join |
| 🔴 MAJOR | **整页 try-catch 健壮性** | `ApprovalServiceImpl.java:50-107, 111-163` | 整方法包 `catch(Exception)`，单条 record 的 businessId 坏 → 整页 500，非跳过坏数据。生产中一条脏数据致全列表不可用 |
| 🟡 MINOR | selectPublishedList 用 Map | `ApprovalServiceImpl.java:116` | `List<Map<String,Object>>` 类型不安全，手动 toLong/toString 易错。应用 VO/Entity |
| 🟡 MINOR | toDate 只认 Date | `ApprovalServiceImpl.java:202-205` | DB 返回 LocalDateTime/String 时返回 null，createTime 丢失 |
| 🟡 MINOR | action 校验顺序 | `ApprovalEngine.java:84-113` | 先 insertLog 后才判 action 合法性（L111）。虽事务回滚保底，但顺序应先校验 |
| 🟡 MINOR | dictionary/lookup 代码重复 | `DictionaryServiceImpl` / `ClassifyServiceImpl` | CRUD 模板 + convertToListVO/convertToVO 几乎一致，可抽公共基类（AI 生成痕迹） |

## 维度 3：安全

| 严重级 | 问题 | 位置 | 说明 |
|--------|------|------|------|
| ✅ | 无 SQL 注入 | 全 mapper xml | 16 处 LIKE 全用 `#{}` 参数化，0 处 `${}` |
| ✅ | 审批人权限校验 | `ApprovalEngine.java:71` | `currentNode.getUserId().equals(UserContextHolder.getUserId())` |
| 🔴 MAJOR | **tenantId 设空** | `ChatbotBindServiceImpl.java:119` | `entity.setTenantId("")`，注释自标 `#ASSUMED`。若系统多租户 → 租户数据隔离失效 |
| 🟠 待确认 | bindAccount 的 token 来源 | `ChatbotBindServiceImpl.java:85` | token 从前端传入转发通讯录 API。需确认：是用户凭证还是平台 token？是否最小化/审计 |
| 🟡 MINOR | 错误消息含 e.getMessage() | `ApprovalServiceImpl.java:177` | `"审批操作失败："+e.getMessage()` 暴露内部异常给前端，轻微信息泄露 |
| 🟡 MINOR | 硬删除 | `ChatbotBindServiceImpl.java:147`、`ClassifyServiceImpl.java:188` | unbind 物理删除、classify 级联硬删除 item，无软删除，审计/误删风险 |

## 维度 4：架构 / 上下文

| 项 | 结论 | 说明 |
|---|------|------|
| 共享 DB 同构 | ✅ 确认 | market 与 open 共享 `openapp` 库，approval entity 同构（除 businessId 类型，见 #2） |
| handler 同库事务 | ✅ 合理 | `AppVersionPublishHandler` 更新版本状态在审批 @Transactional 内，原子性 OK |
| 双写并发 | ⚠️ 交阶段 3 | market 审批更新 approval_record_t，open 发起也写同表，两服务并发写需阶段 3 复核 |
| 版本号比较 bug | 🔴 见 #3 | selectPublishedList `MAX(version_code)` 字符串比较 |

---

## 阻塞问题汇总（按修复优先级）

| # | 优先级 | 问题 | 修复建议 |
|---|--------|------|---------|
| 1 | P0 | 审批并发无锁 | `ApprovalEngine.process` 加乐观锁：update 带 `WHERE id=? AND status=? AND current_node=?`，根据 affectedRows 判断；或 `SELECT FOR UPDATE` |
| 2 | P0 | businessId 类型不一致（market=String/open=Long） | market `ApprovalRecord.businessId` 改 Long 与 open 同构；移除 ServiceImpl/handler 里 3 处 `Long.parseLong` |
| 3 | P0 | MAX(version_code) 字符串比较 bug | selectPublishedList 改用版本自增 id 或数值列取最新，勿对字符串 version_code 用 MAX |
| 4 | P1 | N+1 查询 | 列表查询后批量 `selectByIds`（version/app/ability），内存整合 |
| 5 | P1 | 节点越界未校验 | parseNodes 后校验 `!nodes.isEmpty() && currentNode<nodes.size()`，否则抛业务异常 |
| 6 | P1 | 整页 try-catch | 循环内单条 try-catch 跳过坏数据，记 warn 日志，返回部分结果 |
| 7 | P1 | tenantId 设空 | 从应用记录继承真实 tenantId；确认系统是否多租户，若是则必须修 |
| 8 | P2 | bindAccount 并发超额 | 数量上限校验加唯一约束或行锁，防 check-then-act 竞态 |

## 通过项（亮点）

- ✅ SQL 规范执行好（显式字段、JOIN≤3、参数化绑定）
- ✅ 无 SQL 注入、无硬编码密钥/URL、无 System.out
- ✅ 审批人权限校验到位
- ✅ handler 策略模式设计合理，同库事务原子性保证
- ✅ @NotBlank/@NotNull/@Valid 入参校验完整
- ✅ Long→String VO 转换防 JS 精度丢失（chatbotbindtab）

## 结论

❌ **不通过**。8 个 MAJOR 中 #1（并发无锁）、#2（类型不同构）、#3（版本号比较 bug）为 P0，影响审批正确性与 spec 符合性，建议优先修复后复审。代码整体结构、规范、安全基线良好，问题集中在**并发与健壮性**而非规范缺失。
