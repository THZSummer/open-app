# 代码审查报告：连接器平台 V3

**审查日期**: 2026-06-22（初版） → 2026-06-22（增量更新）
**审查范围**: open-server (119 源文件) + connector-api (54 源文件)
**审查基准**: spec.md §1.7 状态机 + 47 FR

## 执行摘要

| 类别 | v1 数量 | v1 已修复 | v2 增量 | v2 状态 |
|------|:---:|:---:|:---:|:---:|
| 🔴 阻塞 | 5 | ✅ 5/5 | 0 | — |
| 🟡 重要 | 6 | 1/6 → 2/6 | +1 ✅ | 余 5 |
| 🟢 建议 | 5 | 0/5 | +2 | 余 7 |
| **合计** | **16** | | | **12** |

### v2 增量审查（2026-06-22）

**审查触发**: 🟡 #7 修复 — `scriptSource` → `script` 字段名对齐

**审查结论**: ✅ **通过** — 修复正确完整，零残留漂移，无回归风险。

**修复代码**:
- `open-server/.../FlowPublishValidator.java:166`: `data.get("scriptSource")` → `data.get("script")`
- `open-server/.../FlowPublishValidatorTest.java:331,365,383,401`: JSON fixture `"scriptSource"` → `"script"`

**验证矩阵**:

| 维度 | 结果 |
|------|:---:|
| 全链路 JSON 字段名一致 (spec → plan → runtime → validator) | ✅ |
| `"scriptSource"` 全代码库残留 (排除 Java 变量名) | 0 处 |
| GraalJS Context try-with-resources 安全性 | ✅ |
| 测试用例 JSON fixture 有效性 | ✅ |
| 前次遗留 5 个 🟡 问题影响 | 无影响 |
| 新增死代码发现 (`ScriptExecutionConfig.java` 未被引用) | 🟢 建议 #17 |
| 新增命名建议 (`scriptSource` 局部变量名) | 🟢 建议 #18 |

---

## 🔴 阻塞问题（已全部修复）

1. **FlowVersion 删除范围过窄** — FR-029: 修复为允许 DRAFT/WITHDRAWN/REJECTED/INVALIDATED 四种状态删除
2. **ExecutionStatus 枚举缺失 PENDING** — 新增 `PENDING(2, "执行中")`
3. **FlowCopyService 重试循环失效** — return 移出循环体
4. **WITHDRAWN/REJECTED 版本无法编辑** — 扩展编辑条件 + 自动转 DRAFT
5. **@EnableScheduling 缺失** — ConnectorApiApplication 添加注解

## 🟡 重要问题（可延后）

6. ConnectorService 硬编码枚举数值
7. 脚本语法校验不完整（GraalJS parse 未在发布校验中执行）✅ 已修复 (v2) — FlowPublishValidator.java:166 fieldName `scriptSource`→`script`
8. Java 内存分页性能问题
9. DataProcessorExecutor 遗留代码
10. 异常处理模式不一致
11. FlowVersionService.invalidateVersion() 未使用 isValidTransition()

## 🟢 建议

12. N+1 查询优化
13. 版本号计算冗余
14. 名称唯一性校验
15. Context 清理模式统一
16. @Valid 校验注解
17. **[v2 新增]** `ScriptExecutionConfig.java` 定义但从未被引用 — 死代码，建议清理或归档
18. **[v2 新增]** `FlowPublishValidator.java:166` 局部变量名 `scriptSource` 与 JSON 字段名 `"script"` 不一致 — 建议重命名为 `scriptField` 以提高可读性

## 审查结论

✅ **通过** — 5 个阻塞问题已全部修复，编译验证通过。v2 增量审查确认 GraalJS parse 字段名对齐完整无残留。
