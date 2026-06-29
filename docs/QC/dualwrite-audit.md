# 阶段 3：跨模块双写一致性 + AI 痕迹审查

> 阶段 3 产出。market-server 与 open-server 共享 MySQL `openapp` 库的多表双写风险 + AI 生成代码痕迹。

## 1. 双写架构概览

经 mapper xml 全扫描，**market-server 操作 9 张 openplatform 表**，其中与 open-server 共享的表：

| 表 | market 操作 | open 操作 | 双写性质 | 风险 |
|----|------------|-----------|---------|------|
| `openplatform_v2_approval_record_t` | 审批读写 | 发起审批写 | **双向写** | 🔴 高 |
| `openplatform_v2_approval_flow_t` | 读 | 写 | 读+写 | 🟡 中 |
| `openplatform_app_t` | 读 | 读写 | 读+写 | 🟡 中 |
| `openplatform_app_version_t` | 读 + 审批更新状态 | 读写 | **双向写** | 🔴 高 |
| `openplatform_ability_t` | 读 | 读写 | 读+写 | 🟢 低 |
| **`openplatform_property_t`** | **dictionary 写** | **app 属性写** | **双向写（不同业务）** | 🔴 高 |
| `openplatform_lookup_item_t` | 写（管理） | 读（白名单） | 写+读 | 🟡 中（缓存） |
| `openplatform_lookup_classify_t` | 写（管理） | 读 | 写+读 | 🟡 中 |

## 2. 🔴 approval 双写一致性（P0）

### 2.1 Entity 类型不同构
- market `ApprovalRecord.businessId` = **String**
- open `ApprovalRecord.businessId` = **Long**
- spec §1.3 声称"字段完全一致"，实际不符
- 后果：market 内 3 处 `Long.parseLong(record.getBusinessId())` 手动转换，若 open 写入非数字 businessId → market 解析异常

### 2.2 并发写无协调
- open 发起审批 → 写 approval_record_t（status=PENDING）
- market 审批 → 读+更新 approval_record_t（status→APPROVED/REJECTED）
- **两服务并发操作同一记录，无分布式锁/乐观锁**（market ApprovalEngine 无锁，见 batch-1 #1）
- 风险：open 撤回与 market 审批并发 → 状态不一致

### 2.3 版本状态双写
- market `AppVersionPublishHandler.onApproved` → 更新 app_version_t.status=4
- open 版本管理也操作 app_version_t.status
- 两服务更新同一版本状态，需确认状态机一致（open 的 VersionStatusEnum vs market 的 AppVersionStatusEnum 是否对齐）

**修复**：① businessId 统一为 Long；② approval_record_t 加乐观锁（version 字段）；③ 两服务版本状态枚举对齐。

## 3. 🔴 property_t 共用冲突风险（P1）

**新发现**：market 的 dictionary 与 open 的 app 属性**共用 `openplatform_property_t` 表**：
- market `DictionaryMapper.xml` 操作 `openplatform_property_t`（dictionary CRUD）
- open `AppServiceImpl` 的 `appMapper.insertProperty` 也写 `openplatform_property_t`（应用属性）

**风险**：两者若用相同的 (parent_id, property_name/path/code) 命名空间 → **数据互相覆盖或误读**。

**需确认**：
- market dictionary 的 path/code 维度与 open app 属性的 parent_id/property_name 维度是否隔离
- 是否有唯一约束防止冲突

**建议**：核查 property_t 的数据模型，确认两服务的命名空间隔离；若无隔离，加业务前缀或分表。

## 4. 🟡 lookup 读写分离的缓存同步（P2）

- market 管理 lookup（写 openplatform_lookup_item_t/classify_t）
- open 的 AppWhitelistService 读 lookup 做白名单校验
- market 修改 lookup 后，open 的白名单缓存（若有）需失效

**建议**：确认 AppWhitelistService 是否缓存白名单，若是则 market 修改 lookup 后需通知 open 失效缓存。

## 5. 🟡 AI 生成代码痕迹（P3）

| 痕迹 | 位置 | 建议 |
|------|------|------|
| CRUD 模板重复 | market dictionary/lookup/classify 三个 ServiceImpl 几乎同构 | 抽取 AbstractCrudService 基类 |
| convertVO 重复 | convertToListVO/convertToVO 在多模块重复 | 抽取通用转换工具 |
| @author SDDU Build Agent | 全项目类头注释 | 确认是否需保留生成标记 |
| @SuppressWarnings | open 7 处（util/CommonUtils、flowversion 等） | 复核压制内容是否合理 |

> `/remove-ai-slops` 命令可在修复阶段专项清理（先回归测试锁定行为）。

## 6. 结论

双写架构的**核心风险是 approval 与 property_t**：
- approval：类型不同构 + 并发无锁（P0，与 batch-1 审批并发问题叠加）
- property_t：dictionary 与 app 属性共用，命名空间隔离待确认（P1）

这两个是跨服务数据一致性的系统性风险，建议作为修复优先级最高项。lookup 缓存同步与 AI 痕迹清理为次要项。
