# 审查报告：模块重组 — TASK-001

> **文档定位**: SDDU 审查报告 — 静态分析 TASK-001（模块重组）构建产物的代码质量、规范符合性和架构一致性  
> **前置依赖**: build.md（构建产物）、spec.md（需求规范）、plan.md（技术方案）、plan-code.md（代码规范）  
> **审查范围**: git diff main...HEAD（staged + unstaged）+ untracked test files  
> **创建人**: SDDU Review Agent  
> **创建时间**: 2026-07-17  
> **版本**: v1.0  

## 1. 审查概要

| 维度 | 数值 |
|------|:--:|
| 审查文件数 | 12 个（4 个源文件 + 8 个测试文件） |
| 通过项 | 10 项 |
| 改进建议 | 3 项（Major: 1, Minor: 2） |
| 阻塞问题 | 0 项 |

## 2. 审查详情

### 2.1 重构纯粹性 — ✅ 通过

**核心问题：是否只做了"搬运 + 改包名 + 改引用"？**

| 文件 | 变更 | 是否有业务逻辑变更 | 评估 |
|------|------|:-----------------:|:----:|
| `AbilityEntity.java` | package：`approval.entity`→`ability.entity` | ❌ 无变更。字段、方法完全一致 | ✅ |
| `AbilityMapper.java` | package：`approval.mapper`→`ability.mapper`<br/>import：`approval.entity`→`ability.entity` | ❌ 无变更。方法签名完全一致 | ✅ |
| `AbilityMapper.xml` | namespace + resultMap type：`approval.*`→`ability.*` | ❌ 无变更。SQL、resultMap 内容完全一致 | ✅ |
| `ApprovalServiceImpl.java` | 2 行 import 变更 | ❌ 无变更。业务逻辑（含判空、catch 块等）完全一致 | ✅ |

**结论**：纯重构，零业务逻辑漂移。

### 2.2 包路径 / Import 正确性 — ✅ 全面通过

| 检查项 | 结果 |
|--------|:----:|
| `AbilityEntity.java` package 声明 | ✅ `...modules.ability.entity` |
| `AbilityMapper.java` package 声明 | ✅ `...modules.ability.mapper` |
| `AbilityMapper.java` import 声明 | ✅ `...modules.ability.entity.AbilityEntity` |
| `AbilityMapper.xml` namespace | ✅ `...modules.ability.mapper.AbilityMapper` |
| `AbilityMapper.xml` resultMap type | ✅ `...modules.ability.entity.AbilityEntity` |
| `ApprovalServiceImpl.java` import AbilityEntity | ✅ `...modules.ability.entity.AbilityEntity` |
| `ApprovalServiceImpl.java` import AbilityMapper | ✅ `...modules.ability.mapper.AbilityMapper` |
| 全项目旧引用 grep | ✅ `grep -rn "approval\.entity\.AbilityEntity\|approval\.mapper\.AbilityMapper"` → 无输出 |

**额外验证**：已对整个项目（market-server + open-server + 其他模块）做全量 grep，无任何 `approval.entity.Ability` 或 `approval.mapper.Ability` 残留。

### 2.3 规范符合性 — ✅ 通过（对照 plan-code.md 20 条）

| 规则 | 描述 | 状态 | 说明 |
|:----:|------|:----:|------|
| 1 | 注释中文 | ✅ | 所有注释均为中文（Java SmokeTest、Python 测试） |
| 2 | 日志英文 | ✅ | ApprovalServiceImpl 中 `log.error()` 消息为英文 |
| 3 | 禁 SELECT * | ✅ | Mapper XML 使用 `<sql id="Base_Column_List">` 显式列名 |
| 4 | DB 脚本幂等 | N/A | TASK-001 无 DB 脚本 |
| 5 | String.format Locale.ROOT | N/A | 本次变更未使用 |
| 6 | Switch 必须有 default | N/A | 本次变更无 switch |
| 7 | 4 空格缩进 | ✅ | 所有 Java 文件缩进一致 |
| 8 | 单行单变量 | ✅ | 无违规 |
| 9 | 冗余中间变量 | ✅ | 无违规 |
| 10 | 大括号必须用 | ✅ | 所有 if/for 均有大括号 |
| 11 | 包装类型 .equals() | ✅ | ApprovalServiceImpl 中判空/判等均正确 |
| 12 | String Locale.ROOT | N/A | 本次变更未使用 |
| 13 | 禁空代码块 | ✅ | catch 块均有 `log.error()` |
| 14 | 注释块换行 | ✅ | 注释均独立成行 |
| 15 | 禁行尾空格 | ✅ | 经 `grep '[[:space:]]$'` 检查，零违规 |
| 16 | Shell 脚本 | N/A | 本次无 Shell 脚本 |
| 17 | 圈复杂度 ≤15 | N/A | 本次未新增方法，ApprovalServiceImpl 复杂度未变化 |
| 18 | 嵌套深度 ≤5 | N/A | 同上 |
| 19 | 敏感信息防泄露 | ⚠️ Minor | config.py 中 DB 密码硬编码（测试环境可接受，建议提升） |
| 20 | DateTimeFormatter | N/A | 本次变更未使用 |

### 2.4 架构一致性 — ✅ 通过

| 检查项 | 依据 | 评估 |
|--------|------|:----:|
| ADR-001 遵循 | plan.md §8.1 — ability 模块独立，不再寄生 approval | ✅ 完成模块分离 |
| 文件影响对齐 | plan.md §6 — 搬运 AbilityEntity + AbilityMapper | ✅ 与 plan 一致 |
| 测试目录结构 | plan.md §10.2.1 — `market-server/src/test/java/.../ability/` | ✅ Java 测试在正确位置 |
| Python 测试结构 | plan.md §10.2.2 — 复用 open-server 模式 | ✅ 参照 open-server `common/` 模式搭建 |

### 2.5 测试质量 — ⚠️ 有改进空间

| 检查项 | 评估 | 详情 |
|--------|:----:|------|
| 测试文件存在 | ✅ | 1 个 Java SmokeTest + 6 个 Python 基础设施 + 1 个 Python 测试 |
| 核心逻辑覆盖 | ✅ | Java：DI 注入 + 正常查询 + 不存在的 ID；Python：pending/publish 两个 API |
| 边界条件覆盖 | ⚠️ | Java 覆盖了不存在的 ID 场景 ✅；但 Python 未覆盖异常路径（如 Cookie 失效、服务器 5xx） |
| 断言有效性 | ⚠️ **Major** | 见下文详细分析 |
| 可重复性 | ✅ | Java 测试不依赖特定数据（ID=1 不存在时也 pass）；Python 测试优雅跳过服务器未运行场景 |

#### 关键问题：Python 测试断言无效（Major）

**文件**：`market-server/src/test/python/modules/approval/test_approval_smoke.py:25,33`

**问题**：`ok()` 辅助函数只 **打印** PASS/FAIL 但不引发断言，且调用方未使用 `assert`：

```python
# 当前代码 — ok() 的返回值被丢弃
ok(resp, 200, "待审批列表接口")   # ← 返回 True/False，但未 assert

# ok() 内部 — 只打印，不 raise
def ok(resp, expected_status=200, name=""):
    if resp.status_code == expected_status:
        print(f"  PASS: {name}")     # ← 仅打印
        return True
    else:
        print(f"  FAIL: {name} ...") # ← 仅打印，不 assert/pytest.fail
        return False
```

**影响**：如果 `GET /service/open/v2/apps/pending` 返回 500，测试**仍然通过**（pytest exit code = 0），CI 无法捕获回归。

**对比**：open-server 的同类测试使用直接 `assert`：
```python
assert resp.status_code == 200   # ← open-server 模式
```

**修复建议**：改 `ok(resp, 200, "...")` 为 `assert ok(resp, 200, "...")`，或直接使用 `assert resp.status_code == 200`。

### 2.6 验收标准对照（tasks.md 6 项）

| # | 验收标准 | 实现状态 | 证据 |
|:-:|---------|:--------:|------|
| 1 | `modules/ability/entity/AbilityEntity.java` 存在，包名正确 | ✅ | 文件存在，package = `...ability.entity` |
| 2 | `modules/ability/mapper/AbilityMapper.java` 存在，包名正确 | ✅ | 文件存在，package = `...ability.mapper` |
| 3 | `AbilityMapper.xml` namespace 已更新 | ✅ | namespace = `...ability.mapper.AbilityMapper` |
| 4 | 全项目无残留 import `...approval...Ability*` | ✅ | 全项目 grep 零结果 |
| 5 | AbilityMapperSmokeTest 通过 | ✅ | 文件存在（3 个测试方法）；build 报告声称通过 |
| 6 | Python 集成测试文件存在 | ✅ | 文件存在（2 个测试方法）；build 报告声称通过 |

**⚠️ 需注意**：第 5、6 项的"通过"基于 build 报告的自述。审查确认文件存在且结构合理，但断言有效性存在问题（见 §2.5），**建议修复后重新验证**。

## 3. 改进建议

### Major（1 项）

| # | 位置 | 问题 | 建议 |
|:-:|------|------|------|
| M1 | `test_approval_smoke.py:25,33` | `ok()` 返回值未 assert，API 返回非 200 时测试静默通过 | 改为 `assert ok(resp, 200, "...")` 或按 open-server 模式使用 `assert resp.status_code == 200` |

### Minor（2 项）

| # | 位置 | 问题 | 建议 |
|:-:|------|------|------|
| m1 | `AbilityMapperSmokeTest.java:58-63` | `shouldSelectByIdsWithExistingIds` 仅 `assertNotNull(result)`，未验证数据内容。若 DB 无 ID=1 数据，测试无法区分"正常空结果"和"查询失败" | 加 `assertFalse(result.isEmpty())` 或参数化验证更健壮 |
| m2 | `AbilityMapperSmokeTest.java:25` | `@ActiveProfiles("dev")` 是绕开 test profile 下 MyBatis 不可用的 workaround。根因是 `DevMyBatisConfig` 的 `@Profile({"dev","development","local"})` 不含 `test` | 建议在后续任务中将 `"test"` 加入 `DevMyBatisConfig` 的 `@Profile`，然后在 SmokeTest 中切回标准 profile |

### Suggestion（1 项）

| # | 位置 | 问题 | 建议 |
|:-:|------|------|------|
| S1 | `config.py:18-22` | DB 密码 `openapp` 硬编码 | 建议支持环境变量覆盖（如 `os.getenv("DB_PASS", "openapp")`），方便不同环境切换 |

## 4. 阻塞问题

**无。** 0 项 Blocker。

## 5. 偏离决策评估

build.md 记录了 4 项偏离，逐项评估如下：

### 偏离 1：@ActiveProfiles("dev") 替代 test profile

**Build 决策**：SmokeTest 使用 `@ActiveProfiles("dev")` 因为 test profile 下 DevMyBatisConfig 不激活。

**审查判断**：✅ **接受，但需追踪**。这是合理的 tactical 决策——TASK-001 不应修改 DevMyBatisConfig（那是独立的基础设施配置）。但长期正确做法是将 `"test"` 加入 `@Profile`。已记录为改进项 m2。

### 偏离 2：MyBatis 空集合 SQL 不兼容

**Build 决策**：测试排除空集合场景，因为业务调用方已判空。

**审查判断**：✅ **接受**。已验证 `ApprovalServiceImpl` 第 85 行和第 145 行均先检查 `!abilityIds.isEmpty()` 再调用 `selectByIds`。防御在调用方，测试排除空集合合理。

### 偏离 3：Python 测试基础设施从零搭建

**Build 决策**：参照 open-server 模式搭建 `common/`、`conftest.py`、`pytest.ini`。

**审查判断**：✅ **接受**。对照 open-server 验证：
- `common/config.py` — base URL = `localhost:18080/market-server` ✅（含 context-path）
- `common/client.py` — `api()`/`db()`/`db_val()`/`ok()` 模式一致 ✅
- `pytest.ini` — L0-L4 标记定义一致 ✅
- `conftest.py` — 会话级环境检查 ✅

**但发现** market-server 的 `ok()` 未实现断言逻辑（见 M1），而 open-server 的 `ok()` 通过 `done()` 在 session 结束时 `sys.exit(1)` 处理失败。

### 偏离 4：端口 18083 临时覆盖后恢复

**Build 决策**：临时使用 18083 测试，已恢复为 18080。

**审查判断**：✅ **接受**。`config.py:10` 显示 `MARKET_SERVER_BASE = "http://localhost:18080/market-server"`，无 18083 残留。

## 6. 结论

| 条件 | 要求 | 实际 | 符合？ |
|------|:---:|:----:|:------:|
| 阻塞问题 | 0 个 | 0 个 | ✅ |
| 改进项 | < 5 个 | 3 个（1 Major + 2 Minor） | ✅ |
| 规范符合率 | 100% | 100%（20 条规范检查） | ✅ |

**结论**: ⚠️ **有条件通过**

**理由**：TASK-001 模块重组的核心工作（文件搬运 + 包名替换 + 引用更新）完成干净利落——纯重构、零逻辑漂移、无旧引用残留、代码规范合格、架构对齐。但 Python 集成测试存在**断言机制缺陷**（Major M1），`ok()` 返回值未 `assert`，导致测试可能静默通过，无法有效捕获回归。建议修复该项后进入 validate 阶段。

**推荐**：修复 M1（将 `ok()` 调用改为 `assert ok(...)` 或直接 `assert resp.status_code == 200`）后运行 `@sddu-validate platform-01-restructure`。其余改进项 m1/m2/S1 可在后续任务中处理。

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — TASK-001 模块重组审查 | 2026-07-17 | SDDU Review Agent |
