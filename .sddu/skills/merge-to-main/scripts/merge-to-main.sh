#!/usr/bin/env bash
# ==============================================================
# merge-to-main.sh — 将本地分支合入远端 main（确定性流程）
#
# 用途：复刻"命令行方式合入远端 main"的完整 git 操作序列，
#       保证每次执行结果一致，供 merge-to-main Skill 调用。
#
# 入参：
#   --branch <name>   源分支名（默认：当前所在分支）
#   --keep-local      合入推送后保留本地源分支（默认删除）
#   --dry-run         只检查状态并输出计划，不修改任何状态
#
# 出参：
#   stdout: 单行 JSON（成功时 ok=true）
#   stderr: 错误详情 / git 命令输出
#   exit 0  成功（dry-run 时为"检查通过，可执行"）
#   exit 1  前置检查失败（非 git 仓库 / 工作区脏 / 分支不存在等）
#   exit 2  执行失败（rebase 冲突 / merge 失败 / push 被拒）
# ==============================================================
set -euo pipefail

BRANCH=""
KEEP_LOCAL=0
DRY_RUN=0

usage() {
  echo "用法: $0 [--branch <name>] [--keep-local] [--dry-run]" >&2
  echo "  --branch <name>   源分支名（默认：当前所在分支）" >&2
  echo "  --keep-local      合入推送后保留本地源分支（默认删除）" >&2
  echo "  --dry-run         只检查并输出计划，不修改任何状态" >&2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch) BRANCH="${2:-}"; shift 2 ;;
    --keep-local) KEEP_LOCAL=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) usage; exit 1 ;;
  esac
done

fail() {  # fail <stage> <reason> [detail...]
  printf '{"ok":false,"stage":"%s","reason":"%s"}\n' "$1" "$2"
  exit 1
}

# --- 前置检查 ---
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail precheck not_a_git_repo
fi

if [[ -n "$(git status --porcelain)" ]]; then
  fail precheck dirty_worktree "工作区有未提交改动，请先 stash / 提交 / 放弃"
fi

if [[ -z "$BRANCH" ]]; then
  BRANCH="$(git branch --show-current 2>/dev/null || true)"
fi
if [[ -z "$BRANCH" ]]; then
  fail precheck detached_head "HEAD 处于游离状态，请先切换到目标分支"
fi
if [[ "$BRANCH" == "main" ]]; then
  fail precheck source_is_main "源分支不能是 main（本地 main 落后时请直接 git pull）"
fi
if ! git rev-parse --verify --quiet "$BRANCH" >/dev/null; then
  fail precheck branch_not_found "分支 $BRANCH 不存在"
fi

# --- 获取远端状态 ---
if ! git fetch origin main >/dev/null 2>&1; then
  fail precheck fetch_failed "git fetch origin main 失败，请检查网络/远端配置"
fi
REMOTE_BEFORE="$(git rev-parse --short origin/main)"

# --- 快进判断：origin/main 是否为源分支祖先 ---
CAN_FF="no"
REBASE_NEEDED="yes"
if git merge-base --is-ancestor origin/main "$BRANCH" >/dev/null 2>&1; then
  CAN_FF="yes"
  REBASE_NEEDED="no"
fi

# --- dry-run：输出计划后退出 ---
if [[ "$DRY_RUN" == "1" ]]; then
  DELETION="true"
  [[ "$KEEP_LOCAL" == "1" ]] && DELETION="false"
  printf '{"ok":true,"stage":"dry_run","source_branch":"%s","remote_main_before":"%s","can_fast_forward":"%s","rebase_required":"%s","plan":"checkout main; merge --ff-only %s; push origin main; delete_local=%s"}\n' \
    "$BRANCH" "$REMOTE_BEFORE" "$CAN_FF" "$REBASE_NEEDED" "$BRANCH" "$DELETION"
  exit 0
fi

# --- 切换到源分支（若不在其上） ---
if [[ "$(git branch --show-current)" != "$BRANCH" ]]; then
  if ! git checkout "$BRANCH" >/dev/null 2>&1; then
    fail precheck checkout_failed "切换分支 $BRANCH 失败"
  fi
fi

# --- 需要 rebase 时：先变基到 origin/main ---
if [[ "$REBASE_NEEDED" == "yes" ]]; then
  if ! git rebase origin/main >/dev/null 2>&1; then
    printf '{"ok":false,"stage":"rebase","reason":"rebase_conflict","hint":"解决冲突后执行 git rebase --continue，或 git rebase --abort 放弃"}\n'
    exit 2
  fi
fi

# --- 合入 main（强制快进，绝不产生 merge commit） ---
if ! git checkout main >/dev/null 2>&1; then
  fail precheck checkout_failed "切换分支 main 失败"
fi
if ! git merge --ff-only "$BRANCH" >/dev/null 2>&1; then
  printf '{"ok":false,"stage":"merge","reason":"merge_failed","hint":"fast-forward 合入失败，请检查分支状态"}\n'
  exit 2
fi
MERGED="$(git rev-parse --short HEAD)"

# --- 推送远端 ---
if ! git push origin main >/dev/null 2>&1; then
  printf '{"ok":false,"stage":"push","reason":"push_rejected","hint":"推送被拒（远端可能新增提交），请 fetch 后检查再重试"}\n'
  exit 2
fi
REMOTE_AFTER="$(git rev-parse --short origin/main)"

# --- 清理本地源分支 ---
DELETED="false"
if [[ "$KEEP_LOCAL" != "1" ]]; then
  if git branch -d "$BRANCH" >/dev/null 2>&1; then
    DELETED="true"
  fi
fi

printf '{"ok":true,"stage":"done","source_branch":"%s","current_branch":"main","remote_main_before":"%s","merged_commit":"%s","remote_main_after":"%s","can_fast_forward":"%s","local_branch_deleted":"%s"}\n' \
  "$BRANCH" "$REMOTE_BEFORE" "$MERGED" "$REMOTE_AFTER" "$CAN_FF" "$DELETED"
