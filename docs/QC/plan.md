# 后端代码 QC 计划（open-server + market-server）

> 本文档是后端代码质量审查的执行计划与追踪文档。基于 `/review-work` 5-agent 框架，针对大范围改动改造为分批流水线。

## 0. 元信息

| 项 | 值 |
|----|-----|
| 计划日期 | 2026-06-29 |
| 审查范围 | `open-server` + `market-server` 两个后端模块 |
| 基线 commit | `4650daed`（2026-06-10） |
| 范围终点 | HEAD（2026-06-27） |
| 涉及 commit | 421 个 |
| QC 维度 | ①代码质量+规范 ②安全漏洞 ③AI生成代码坏味道 ④架构一致性 |
| 执行策略 | open-server **全覆盖 6 批**；market-server 1 批 |
| 产出目录 | `docs/QC/`（hotspots / batch-1-market / batch-2A~2F / dualwrite-audit / ai-slops-cleanup / report） |
| 负责人 | _待填_ |

---

## 1. 改动范围统计

### 1.1 总量

| 模块 | 改动 java 文件 | 增行 | 删行 | 性质 |
|------|---------------|------|------|------|
| **open-server** | 264 | +26,017 | -4,955 | 增量改动（含大量重构） |
| **market-server** | 49 | +2,261 | -107 | 偏新增 |
| **合计** | **313** | **+28,278** | **-5,062** | — |

> ⚠️ open-server 有 **-4,955 删除行**，疑似大规模重构，是本次 QC 的重点排查对象。

### 1.2 open-server 模块改动分布（按文件数）

| 模块 | java 文件 | 删行(实测) | 备注 |
|------|-----------|-----------|------|
| common | 43 | 489 | 配置/枚举/拦截器，安全敏感 |
| app | 35 | 54 | 应用管理 |
| open-server 顶层 | 24 | **1942** | 🔴 重构最重（启动/全局配置） |
| flow | 20 | **550** | 🔴 审批流，重构重 |
| connector | 18 | **547** | 🔴 连接器，重构重 |
| ability | 15 | 0 | 能力 |
| version | 13 | 0 | 版本 |
| approval | 13 | 26 | 审批 |
| member | 12 | 0 | 成员 |
| card | 10 | 0 | 卡片 |
| connectorversion | 9 | 0 | 连接器版本 |
| flowexecrecord | 8 | 0 | 流执行记录 |
| permission | 8 | 75 | 权限，安全敏感 |
| flowversion | 7 | 0 | 流版本 |
| callback | 6 | 12 | 回调 |
| api | 5 | 10 | API |
| sync | 4 | 8 | 同步 |
| auditlog | 4 | 35 | 审计日志 |
| security | 4 | 0 | 安全 |
| debug/employee/event/lookup | 6 | 19 | 小模块 |

### 1.3 market-server 模块改动分布

| 模块 | java 文件 | 备注 |
|------|-----------|------|
| approval | 25 | 审批引擎，逻辑核心 |
| chatbotbindtab | 10 | 机器人绑定页签 |
| lookup | 8 | 字典查找 |
| dictionary | 4 | 数据字典 |
| common | 2 | 配置 |

### 1.4 跨模块重叠（架构一致性风险）

`market-server` 与 `open-server` 同时存在 **approval / dictionary / lookup** 同名模块，且本次 6.10 后两边 approval 都有改动（market 25 文件 / open 13 文件）。

> **架构发现（查 spec §1.3 确认）**：market 与 open **共享同一 MySQL `openapp` 库**，approval 的 Entity（`ApprovalRecord`/`ApprovalLog`/`ApprovalFlow`）**故意与 open-server 字段同构**——**不是复制代码，而是"共享 DB + 双写"架构**。
>
> 因此阶段 3 的 approval 不应做"代码查重"，而应聚焦：① 同库双写的**字段一致性** ② 并发写**安全**（两服务同时写同一表） ③ 操作语义对齐。dictionary / lookup 是否也同构待执行时确认。

---

## 2. 四阶段执行方案

```
阶段0  快扫热点（grep/ast_grep，全量313文件）   → 圈定优先级、发现低悬果实
  ↓
阶段1  market-server 快速过（1批，2.3k行）       → 轻量，approval 为核心
  ↓
阶段2  open-server 分批深审（6批，26k行）        → 本次 QC 核心
  ↓
阶段3  跨模块查重 + AI痕迹清理                   → 架构一致性
```

### 阶段 0：轻量快扫（全量，不用 review-work）

**目的**：全量扫 313 个改动文件的明显坏味道，产出热点排名，为阶段 2 的 Oracle 圈定该深读哪些文件，并发现低悬果实。

**扫描清单**：

| # | 模式 | 维度 | 工具 |
|---|------|------|------|
| 1 | 硬编码密码/密钥/token（`password=`、`secret`、`apikey`） | 安全 | grep |
| 2 | `System.out.println` / `printStackTrace()` | 质量 | grep |
| 3 | 空 catch（`catch (... e) {}`） | 质量 | ast_grep |
| 4 | SQL 字符串拼接（`"... " + var`） | 安全 | ast_grep |
| 5 | `TODO` / `FIXME` / `HACK` / `XXX` | 质量 | grep |
| 6 | 硬编码 URL/IP（`http://`、`https://` 明文） | 安全 | grep |
| 7 | `@SuppressWarnings` 滥用 | 质量 | grep |
| 8 | `new Random()`（安全场景误用） | 安全 | ast_grep |

**产出**：`docs/QC/hotspots.md`（按文件聚合命中数，降序排名）。

### 阶段 1：market-server（1 批）

49 文件、+2,261 行，**一次 review-work 跑完**（Oracle 可装下）。重点：approval 25 文件的审批引擎逻辑。

### 阶段 2：open-server（6 批，核心）

按业务域聚合 + 文件数均衡划分。每批跑一次 review-work 5-agent。

| 批次 | 范围 | 文件 | 风险标签 | 重点 |
|------|------|------|---------|------|
| **2-A** | common | 43 | 🟠 重构+安全敏感 | 配置/拦截器/枚举，删489行，权限&加解密相关 |
| **2-B** | app | 35 | 🟡 常规 | 应用管理 CRUD |
| **2-C** | open-server顶层 + flow | 44 | 🔴 重构最重 | 删 1942+550 行，挖"删了什么、为何删" |
| **2-D** | connector + ability + connectorversion | 41 | 🔴 重构重 | 删 547 行，连接器域 |
| **2-E** | version + approval + member + permission | 46 | 🟠 安全敏感 | permission 删75行，权限+审批+成员 |
| **2-F** | card + flowexecrecord + flowversion + callback + api + sync + auditlog + security + 其余 | 54 | 🟢 小模块合并 | 审计日志/回调/同步等 |

> **动态拆批规则**：若某批实测改动行数 > 8,000（Oracle context 预警线），执行时再按子包（如 common 拆 config/enums vs service/controller）拆分。

### 阶段 3：跨模块查重 + AI 痕迹清理（不用 review-work）

| 任务 | 方法 | 输出 |
|------|------|------|
| market.approval ↔ open.approval 一致性 | **同库双写审查**：Entity 字段一致性 + 并发写安全 + 操作语义对齐（spec 已确认共享 DB 同构，非代码查重） | 双写风险清单 |
| market.dictionary vs open.dictionary | 待确认是否同构；若复制则查重，若同构转一致性审查 | 重复/风险清单 |
| market.lookup vs open.lookup | 同上 | 重复/风险清单 |
| AI 生成代码坏味道清理 | `/remove-ai-slops`（先回归测试锁定行为） | 清理报告 |
| 分层架构一致性 | `lsp_find_references` + 手动 | 架构偏离清单 |

---

## 3. review-work 5-Agent 定制（针对本次事后 QC）

本次是**事后大范围 QC**，非单次实现验证，5 个 agent 需定制：

| Agent | 默认角色 | 本次定制 | 必要性 |
|-------|---------|---------|--------|
| **1 Goal Verify** | 验证单一实现 goal | ⚠️ 无单一 goal。**改造为**：API 契约/返回结构/异常规范一致性验证；或对照 `docs/market-server/*-spec.md` 做规范符合性验证；或直接用 `@sddu-review` 替代 | 可选 |
| **2 QA Execution** | hands-on 跑应用 | 后端服务，curl 打 API。均按 `AGENTS.md` 用 **WMI** 起服务（禁用 Start-Process）。market-server：`MarketServerApplication`，端口18080/context `/market-server`；open-server：端口18080/context `/open-server`。⚠️**两者端口冲突，审谁起谁，不可同时运行** | 主 |
| **3 Code Quality** | Oracle 看全文 | ✅ 核心。注入该批 file_contents（含 diff）。10 维度审查 | 主 |
| **4 Security** | Oracle 看全文 | ✅ 核心。注入该批 file_contents。10 项安全清单 | 副 |
| **5 Context Miner** | 挖 git/引用 | ✅ 重点挖"重构删除原因"（`git log` 追溯被删代码历史）+ 跨模块引用影响 | 主 |

**Oracle 投喂规则**：Oracle 不能读文件，必须把该批所有改动文件的**完整内容 + diff + 邻近参考文件**塞进 prompt。投喂前用 `git diff --numstat` 实测行数，超 8k 行则拆批。

---

## 4. 执行反馈机制（中粒度）

执行期间在每个有意义的时间点**主动**输出进度，绝不静默。粒度：每个 agent 完成报一行 + 阶段切换报 + 每批汇总。

### 4.1 阶段 0 快扫（同步执行，逐项报）
- 每项报命中数：`✓ 扫描 3/8 空catch：命中 X 处`
- 全部完成报 Top5 热点，并写入 `docs/QC/hotspots.md`

### 4.2 review-work 每批（5 个后台 agent，异步轮询）

| 节点 | 反馈内容 |
|------|---------|
| T0 启动 | 「开始批次 X（N 文件），5 个 agent 已派发」 |
| 执行中 | agent 阶段切换/完成时报：`活跃 5/5｜Agent3 代码质量 审查中…` |
| 静默预警 | agent 超时无信号，主动报「Agent4 仍在跑（已 90s），未卡死」 |
| 单 agent 完成 | 「Agent3 完成 → PASS，2 个 MAJOR」 |
| 批次完成 | 「批次 X 完成：3 PASS / 1 FAIL，阻塞 N 个，已写 batch-*.md」 |
| 批次间 | 「总进度 m/8 批，累计阻塞 X 个」 |

### 4.3 收尾
全部完成后写 `docs/QC/report.md` 汇总，给出阻塞问题修复优先级。

> **承诺**：后台 agent 异步运行，采用**轮询式主动汇报**，不会"派发完就没动静"。单 agent 静默超阈值主动预警；确认卡死则标 INCONCLUSIVE 并重派更小的 reviewer。

---

## 5. 工具与命令映射

| 维度 | 首选工具 | 备注 |
|------|---------|------|
| 代码质量+规范 | `/review-work` Agent3 + ast_grep | 静态模式扫描 |
| 安全漏洞 | `/review-work` Agent4（或 `/security-review` 专项深审） | 敏感模块可追加专项 |
| AI 生成坏味道 | `/remove-ai-slops` | 阶段 3 统一清理 |
| 架构一致性 | `/review-work` Agent5 + 手动查重 | 阶段 3 跨模块 |

**服务启动（QA agent 必读）**：严格遵循 `AGENTS.md` 第一条——**必须用 WMI 启动**，禁止 `Start-Process`/`cmd start`，否则 bash 工具永久卡死。

---

## 6. 风险与待确认事项

| # | 事项 | 状态 | 影响 |
|---|------|------|------|
| 1 | market-server 启动方式 | ✅ 已确认 | 独立 SpringBoot 应用（`MarketServerApplication`，jar），端口 **18080**/context-path `/market-server`，共享 `openapp` 库；⚠️ **与 open-server 端口冲突**，QA 不可同时起两个服务 |
| 2 | Agent1 Goal Verify 处理方式 | ✅ 已确认 | market 批 → 对照 `app-version-approval-spec.md`/`app-single-chatbot-bindtab-spec.md` 做 **spec 符合性验证**；open 批 → 无统一 spec，**省略**或降级为 API 契约/异常规范一致性轻验证 |
| 3 | git 未跟踪文件是否纳入（`start-server.bat`/`server.log`/`uploads/`） | ❓ 待定 | 影响 QC 边界 |
| 4 | 单批行数实测，超 8k 行则动态拆批 | ⏳ 执行时验证 | Oracle context 上限 |
| 5 | open-server -4955 删除行的重构动机 | ❓ 待 Context Miner 挖掘 | 是否引入回归风险 |
| 6 | 26k 行全覆盖 = 6 批 × 5 agent，耗时长，建议分会话推进 | ⚠️ 已知 | 排期 |

---

## 7. 执行追踪表

> 每完成一项勾选；正式执行时用此表对应 `todowrite` 追踪。

### 准备阶段
- [x] 确定基线 commit（`4650daed`）
- [x] 统计改动范围与模块分布
- [x] 制定分批方案
- [x] 本计划落盘
- [x] 确认 market-server 启动方式（待确认事项1）
- [x] 确定 Agent1 处理方式（待确认事项2）

### 阶段 0：快扫 ✅
- [x] 执行 8 项坏味道扫描
- [x] 产出 `docs/QC/hotspots.md` 热点排名

### 阶段 1：market-server
- [x] 批 1-M：market-server 全量（49 文件）review-work
- [x] 产出 market-server 审查报告

### 阶段 2：open-server（6 批）
- [x] 批 2-A：common（43）
- [x] 批 2-B：app（35）
- [x] 批 2-C：open-server顶层 + flow（44）
- [x] 批 2-D：connector + ability + connectorversion（41）
- [x] 批 2-E：version + approval + member + permission（46）
- [x] 批 2-F：小模块合并（54）
- [x] 汇总 open-server 6 批审查报告

### 阶段 3：查重 + AI痕迹
- [x] approval 跨模块查重
- [x] dictionary 跨模块查重
- [x] lookup 跨模块查重
- [x] `/remove-ai-slops` 清理
- [x] 架构一致性核查

### 收尾
- [x] 汇总总报告 `docs/QC/report.md`
- [x] 阻塞问题修复跟踪

---

## 附录 A：数据获取命令

```bash
# 基线
git log --before="2026-06-10" --pretty=format:"%h" -1   # → 4650daed

# 某批改动文件清单（以批2-A common 为例）
git diff --name-only 4650daed HEAD -- "open-server/src/main/java/com/xxx/it/works/wecode/v2/common/*.java"

# 某批改动 diff（投喂 Oracle 用）
git diff 4650daed HEAD -- "open-server/src/main/java/com/xxx/it/works/wecode/v2/common/"

# 某批实测行数（拆批判断）
git diff --numstat 4650daed HEAD -- "open-server/src/main/java/com/xxx/it/works/wecode/v2/common/" | awk '{a+=$1;d+=$2} END{print "add="a" del="d}'
```

## 附录 B：批次→模块路径速查

| 批次 | git pathspec 关键词 |
|------|---------------------|
| 2-A | `v2/common/` |
| 2-B | `v2/modules/app/` |
| 2-C | `v2/OpenServerApplication*` 等 + `v2/modules/flow/` |
| 2-D | `v2/modules/connector/` + `ability/` + `connectorversion/` |
| 2-E | `v2/modules/version/` + `approval/` + `member/` + `permission/` |
| 2-F | `card/` + `flowexecrecord/` + `flowversion/` + `callback/` + `api/` + `sync/` + `auditlog/` + `security/` + `debug/` + `employee/` + `event/` + `lookup/` |
| 1-M | `market-server/src/main/java/.../v2/` 全部 |
