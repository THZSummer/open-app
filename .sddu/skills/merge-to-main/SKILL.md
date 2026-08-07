---
name: merge-to-main
description: "当用户需要将本地分支合入远端 main 分支——如"合入 main"、"把分支推到主分支"、"命令行方式合入远端 main"——加载本 Skill。执行 fetch 检查、rebase 快进合入、推送 origin/main，并删除本地源分支。"
---

# merge-to-main

## 接口

阅读本章节即可使用本 Skill，无需阅读后续执行细节。

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| `branch` | string | ❌ | 要合入的源分支名。缺省时使用**当前所在分支** |
| `delete_local` | boolean | ❌ | 合入成功后是否删除本地源分支。默认 `true` |

### 返回值

**成功**：

    { "result": "merged", "remote_main": "61c5bcaf", "local_branch_deleted": true }

**异常**：

| 情况 | 行为 |
|------|------|
| 工作区有未提交改动 | 停止，报告并让用户决定（stash / 提交 / 放弃） |
| 远端 main 有新提交且 rebase 冲突 | 停止，报告冲突文件，让用户手动解决后重试 |
| 推送被拒 | 停止，报告完整错误输出 |

### 调用示例

    # 当前在 fix/xxx 分支，直接合入
    → 检查工作区 → fetch → 快进合入 → push → 删除 fix/xxx

    { branch: "feature/auth", delete_local: false }
    → 合入 feature/auth，保留本地分支

---

## 执行流程

调用 `scripts/merge-to-main.sh`，按出参 JSON 决定后续动作。

### 步骤 1：调用脚本

```
bash .sddu/skills/merge-to-main/scripts/merge-to-main.sh [--branch <name>] [--keep-local] [--dry-run]
```

- `branch` 缺省时脚本取当前所在分支
- 若需要先预览（不修改任何状态），加 `--dry-run`

### 步骤 2：按脚本出参处理

脚本执行后输出单行 JSON 到 stdout，根据 `ok` 与 `stage` 字段决定后续：

| `stage` | `ok` | 含义 | Agent 动作 |
|---------|:---:|------|-----------|
| `precheck` | false | 前置检查失败：`dirty_worktree`（工作区脏）/ `not_a_git_repo` / `branch_not_found` / `source_is_main` 等 | 向用户报告原因。`dirty_worktree` 时询问用户 stash / 提交 / 放弃——不可擅自丢弃改动 |
| `rebase` | false | rebase 冲突 | 报告冲突，提示用户手动解决后 `git rebase --continue`（或 `git rebase --abort` 放弃），解决后重新调用脚本 |
| `merge` | false | 快进合入失败 | 报告，检查分支状态后重试 |
| `push` | false | 推送被拒（远端有新提交） | 报告，重新 fetch 检查后重试 |
| `done` | true | 合入成功 | 读取 `merged_commit` / `remote_main_after` / `local_branch_deleted` 向用户汇报 |
| `dry_run` | true | 预览成功 | 向用户展示 `plan` 字段的执行计划，确认后去掉 `--dry-run` 执行 |

### 步骤 3：完成验证

```
git status -sb        # main...origin/main（无 ahead/behind），工作区干净
git log --oneline -3  # 确认合入的提交在顶部
```

---

## 检查清单

- [ ] 合入前工作区干净（或已与用户确认处理方式）
- [ ] 已 fetch origin main，确认远端最新状态
- [ ] 远端有新提交时已 rebase（或停下询问）
- [ ] `git merge` 成功（快进无冲突）
- [ ] `git push origin main` 成功，本地与远端同步
- [ ] 本地源分支已按配置删除（保留则跳过）
- [ ] 最终 `git status -sb` 显示干净

---

## 边界

- **不创建 PR**：本 Skill 只做命令行直推 main。需要 PR 流程时使用 `gh pr create` 手动处理。
- **不删除远端分支**：仅清理本地源分支。
- **不擅自处理冲突**：工作区脏 / rebase 冲突 / merge 冲突一律停下报告，由用户决策。
- **不修改 git 全局配置**、不跳过 hooks、不 force-push。
- **合入 main 前不主动改动代码**：本 Skill 只做版本控制操作，不负责代码修改。

---

## 脚本

### scripts/merge-to-main.sh

- **用途**：执行"本地分支合入远端 main"的确定性 git 操作序列（fetch → 快进判断 → rebase → merge --ff-only → push → 清理分支），以 JSON 输出结果。Agent 不直接执行 git 命令，只调用脚本并根据出参决策。
- **入参**（命令行参数）：
  - `--branch <name>`：源分支名，缺省取当前所在分支
  - `--keep-local`：合入后保留本地源分支（缺省删除）
  - `--dry-run`：只检查状态并输出执行计划，不修改任何状态
- **出参**：
  - stdout：单行 JSON，字段见下
  - stderr：错误详情 / git 命令输出
  - exit code：`0`=成功（dry-run 为可执行）、`1`=前置检查失败、`2`=执行失败

**成功 JSON 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ok` | bool | `true` / `false` |
| `stage` | string | `precheck` / `rebase` / `merge` / `push` / `done` / `dry_run` |
| `reason` | string | 失败原因（仅失败时）：`dirty_worktree`、`not_a_git_repo`、`branch_not_found`、`source_is_main`、`rebase_conflict`、`push_rejected` 等 |
| `hint` | string | 失败时的修复建议（仅失败时） |
| `source_branch` | string | 实际合入的源分支 |
| `remote_main_before` | string | 合入前远端 main 提交短哈希 |
| `merged_commit` | string | 合入后 main 的提交短哈希（仅 `done`） |
| `remote_main_after` | string | 合入推送后远端 main 提交短哈希（仅 `done`） |
| `can_fast_forward` | string | `yes` / `no` — 是否走快进合入路径 |
| `rebase_required` | string | `yes` / `no` — 是否执行了 rebase |
| `local_branch_deleted` | string | `true` / `false` — 本地源分支是否已删除 |
| `plan` | string | dry-run 的执行计划描述（仅 `dry_run`） |

**已知行为**：
- 源分支为 `main` 时拒绝执行（`source_is_main`）——本地 main 落后时请直接 `git pull`
- 合入强制使用 `merge --ff-only`，不产生 merge commit，保证历史线性
- rebase 冲突时脚本停在 rebase 状态并退出（exit 2），Agent 报告后由用户解决（`git rebase --continue` / `--abort`）
