---
name: open-app-tools
description: "当用户想做 open-app 里某个简单的、不值得单独建技能的杂项操作时加载——如启动或重启某个工程、运行某工程的测试、检查修复代码注释空格、梳理某模块结构等。本 Skill 是 open-app 的简单操作收纳工具，提供这些常用杂项操作的执行方式。"
---

# open-app-tools

## 接口

阅读本章节即可使用本 Skill，无需阅读后续执行细节。

本技能通过 `open-app-cli` 执行固定操作（**纯 TS，独立于工程 bash 脚本**），接口即 CLI 子命令契约。

### 子命令

| 命令 | 用途 | 必填参数 | 可选参数 |
|------|------|:--:|:--:|
| `open-app-cli start --service <name>` | 启动服务 | `--service` | `--dry-run` |
| `open-app-cli stop --service <name>` | 停止服务 | `--service` | `--dry-run` |
| `open-app-cli restart --service <name>` | 重启服务 = stop + start | `--service` | `--dry-run` |
| `open-app-cli status [--service <name>]` | 检查状态（缺省全部） | — | `--service` |
| `open-app-cli dashboard [--port <port>]` | 启动 Web 看板（监控 + 操作） | — | `--port` |
| `open-app-cli dashboard stop` | 停止 dashboard 进程 | — | — |
| `open-app-cli dashboard restart` | 重启 dashboard 进程 | — | — |
| `open-app-cli completion [bash\|zsh\|powershell]` | 输出 shell 自动补全脚本 | — | — |
| `open-app-cli tui` | 终端看板（交互式 TUI） | — | — |
| `open-app-cli list` | 列出所有服务 | — | — |
| `open-app-cli help` | 显示用法 | — | — |

### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| `--service <name>` | string | 见子命令 | 服务名：`wecodesite`/`market-web`/`open-server`/`connector-api`/`api-server`/`event-server`/`market-server` |
| `--port <port>` | number | ❌ | dashboard 监听端口（默认 8899） |
| `--dry-run` | boolean | ❌ | 只打印将执行的命令，不实际执行 |

### 返回值

- **成功**：命令输出到 stdout，exit code `0`。
- **失败**：错误输出到 stderr，exit code `1`（未知服务 / 启动超时 / 探测失败）。
- **dashboard**：常驻 HTTP 服务器，提供 Web 看板：
  - `GET /` — 看板页面（每服务 访问/启动/停止/重启/日志）
  - `GET /api/status` — 服务状态（含 `urlPath` 快速访问路径）
  - `POST /api/{start|stop|restart}/:name` — 操作服务（start/restart 异步后台触发立即返回）
  - `GET /api/logs/:name?tail=` — 日志尾部（上限 2MB）
  - `GET /api/logs/:name/stream` — SSE 实时日志流
  - `GET /api/services` — 服务清单

### 调用示例

    open-app-cli restart --service open-server
    → 纯 TS 重启 open-server（spawn mvn → 等 actuator 就绪），exit 0

    open-app-cli dashboard
    → 启动看板于 http://127.0.0.1:8899，页面可 访问/启动/停止/重启/看实时日志

    open-app-cli dashboard restart
    → 重启 dashboard 进程本身（stop + detached 拉起）

    open-app-cli completion bash
    → 输出 bash 自动补全脚本，配合 source 使用

    open-app-cli tui
    → 终端看板：↑↓/j k 选择 · s启动 x停止 r重启 · l日志 · R刷新 · q退出

---

## 执行流程

1. **识别请求**：将用户请求映射到「收纳表」中的一条动作（启/停/重启、状态检查、测试、注释检查等）；找不到对应条目则停止并如实说明。
2. **确定目标**：确认目标工程名（`--service`）或模块/测试路径；缺省时从用户上下文推断。
3. **执行**：调用对应 `open-app-cli` 子命令（`restart`/`stop`/`status`/`list`），或以 README 中的标准命令执行（`mvn`、`pytest`、`check_comment_spacing.py`）。
4. **验证**：以 CLI 出参（exit code + stdout）或收纳表的「验证方式」确认结果。失败则停下报告，不假装成功。

## 收纳表

| # | 动作 | 典型触发 | 处理方式 | 验证方式 |
|---|-------------|---------|---------|---------|
| 1 | 工程启/停/重启 | “重启 wecodesite” / “停掉 open-server” / “起一下 market-web” | `open-app-cli start --service <工程名>`（启动）；`open-app-cli stop --service <工程名>`（停止）；`open-app-cli restart --service <工程名>`（重启）；均可加 `--dry-run` 预览 | CLI 退出码 `0` + 输出“已启动 / 已停止 / 已重启…” |
| 2 | 服务状态监控 | “看看服务都起没起” / “open-server 状态如何” / 想看可视化看板 / 看某服务实时日志 | `open-app-cli status`（命令行）；`open-app-cli dashboard`（Web 看板，浏览器打开 http://127.0.0.1:8899） | status 输出 RUNNING/DOWN + 运行数；dashboard 页面每服务可 快速访问 / 启停重启 / 实时日志 |
| 3 | 运行测试 | “跑 open-server 的测试” / “跑前端 E2E” | 按 README 测试章节执行 `mvn -f <pom> test`、`pytest <路径>`（Java 单测 / Python 集成 / Playwright E2E） | 退出码 `0` + 测试通过数；失败则报告并定位 |
| 4 | 注释空格检查/修复 | “检查代码注释空格” | 复用根目录 `check_comment_spacing.py`（含修复逻辑），产出 `comment_spacing_fix_report.md` | 报告已生成、无遗漏 |
| 5 | 结构梳理 | “看下 api-server 用了什么” / “这模块是干嘛的” | 读取 `README.md` 工程地图 + 目录结构 + 关键配置文件，给出结构化梳理 | 从工程地图/依赖关系核对准确性 |
| 6 | 可扩充条目 | （按需新增） | 新增时补全“处理方式 + 验证方式”，并注明是否复用已有脚本/命令 | — |

---

## 检查清单

- [ ] 已识别动作并映射到「收纳表」条目
- [ ] 已确定目标工程名（`--service`）或测试路径
- [ ] 已调用对应 `open-app-cli` 子命令（或标准命令）并检查 exit code
- [ ] 执行失败已如实报告（退出码、stderr、定位方向），未假装成功
- [ ] 未越界处理不属于本技能的操作

---

## 边界

- **只收纳“简单的杂项操作”**：不做技术判断、不做架构决策、不产出 spec/plan/ADR 等正式文档。
- **不承担复杂任务**：跨模块架构变更、新 Feature 设计、需正式文档的需求——请走完整 SDDU 流程。
- **已有专门技能覆盖的高风险或专门操作不收纳**：例如“合入远端 main”已由 `merge-to-main` 技能负责，不在此重复。
- **执行深度交给 Agent**：本技能只写「遇到什么操作、怎么处理」，实际执行（读代码、跑命令、改文件、验证）由加载它的 Agent 完成。

---

## 脚本

本技能提供确定性 CLI（`scripts/`，TS 实现，经 `tsx` 运行），Agent 直接调用，避免每次手敲命令。命令入口为**纯 node 脚本**，跨平台（Windows/macOS/Linux 均可，不依赖 bash）。

### 首次安装

在本技能所在目录（即包含 `package.json` 的工作目录）执行：

```bash
cd <本技能目录>     # 进入技能自己的工作目录（不要写死绝对路径）
npm install -D tsx   # 1. 安装本地 tsx 运行时（自包含，跨平台）
npm install blessed  # 2. TUI 看板依赖（blessed，仅 tui 命令需要）
npm link             # 3. 注册全局 open-app-cli 命令
open-app-cli list    # 4. 验证
```

> 依赖说明：`tsx` 是运行时（必须）；`blessed` 是 TUI 界面库（仅 `open-app-cli tui` 需要；不用 TUI 可不装）。

- `npm link` 后即可在任何目录用全局 `open-app-cli` 命令。
- 不装依赖时，也可在本技能目录下经 `scripts/cli.ts` 用 `tsx` 运行。
- 所有引用均相对**本技能目录**（`scripts/`、`bin/`），脚本用 `import.meta.url` 定位自身，**不依赖技能被放置的绝对位置**。

### bin/open-app-cli.js — 全局命令入口（跨平台）

- **用途**：作为 npm bin 入口，`npm link` 后提供 `open-app-cli` 命令。用 tsx 注册器加载 `scripts/cli.ts`。
- **入参**（命令行）：转发 `start|stop|restart|status|dashboard|tui|completion|list|help` 子命令
- **出参**：透传 `cli.ts` 的 stdout/stderr 与 exit code；`dashboard` 为常驻进程

### scripts/cli.ts — 命令入口

- **用途**：解析子命令并分发到对应处理模块（`start`、`stop`、`restart`、`status`、`dashboard`、`tui`、`completion`、`list`、`help`）。
- **入参**（命令行）：`start|stop|restart --service <name> [--dry-run]` / `status [--service <name>]` / `dashboard [--port <port>]` | `dashboard stop` | `dashboard restart` / `tui` / `completion [bash|zsh|powershell]` / `list` / `help`
- **出参**：stdout 可读输出；exit code `0`=成功、`1`=失败（未知服务 / 启动超时 / 探测失败）

### scripts/run.ts — 启停核心（纯 TS，独立于工程脚本）

- **用途**：用 `spawn` 自主启动服务（后端 `mvn spring-boot:run`、前端 `npm run dev`），写 `.pid`、等待就绪（actuator/HTTP）；`stop` 用 `process.kill` 读 `.pid` 停止 + 跨平台端口兜底清理残留；`restart` = stop + start。**不调用任何工程 `*.sh` 脚本**。
- **入参**：`start|stop|restart(serviceName, dryRun)`
- **出参**：exit code `0`=成功、`1`=失败；`--dry-run` 只打印命令不执行

### scripts/status.ts — 服务状态检查

- **用途**：探测服务是否存活（复用 `health.ts` 的 `probe`）。缺省检查全部，可指定单个。
- **入参**：`serviceName?`（缺省=全部服务）
- **出参**：打印每服务 `RUNNING/DOWN` 与运行数统计；exit code `0`=成功、`1`=未知服务

### scripts/dashboard.ts — Web 看板（HTTP 服务器）

- **用途**：用 `node:http` 启动看板，监控 + 操作每个服务。提供 `/`（HTML 页面，每服务 访问/启动/停止/重启/日志）、`GET /api/status`、`POST /api/{start|stop|restart}/:name`、`GET /api/logs/:name?tail=`、`GET /api/logs/:name/stream`（SSE 实时日志）、`GET /api/services`。并负责 dashboard 进程自身的 stop/restart（`.dashboard.pid` + 端口兜底）。
- **入参**：`dashboardCmd(port?)` 启动看板；`dashboardStop()` 停止自身；`dashboardRestart()` 重启自身。缺省端口 8899（或 `OPEN_APP_DASHBOARD_PORT` 环境变量）
- **出参**：常驻 HTTP 服务器，打印访问 URL；Ctrl+C / `dashboard stop` 停止
- **OOM 防护**：日志 tail 上限 2MB；SSE 增量分块 64KB；前端日志 DOM 上限 1MB（超长只保留尾部）

### scripts/completion.ts — shell 自动补全

- **用途**：输出 bash / zsh / powershell 自动补全脚本（子命令、参数、`--service` 服务名），服务名从 `services.ts` 动态生成。
- **入参**：`completionCmd(shell)`，`shell` = `bash` | `zsh` | `powershell`
- **出参**：补全脚本写 stdout；配合 `source <(open-app-cli completion <shell>)` 使用

### scripts/tui.ts — 终端看板（TUI）

- **用途**：基于 `blessed` 的交互式终端看板：服务状态监控 + 启停/重启 + 实时日志。界面文本用 ASCII 避免终端乱码；操作通过 spawn 子进程后台执行（不阻塞/污染 TUI）；未处理异常记录到技能目录 `.tui.log` 而非崩溃退出。
- **入参**：`tuiCmd()`
- **出参**：常驻交互界面；快捷键 `j/k` 或 `↑/↓` 选择、`s` 启动、`x` 停止、`r` 重启、`l` 日志、`R`/F5 刷新、`q`/`Q`/`escape`/`C-c` 退出
- **依赖**：`blessed`（CJS，用 `createRequire` 兼容 ESM）

### scripts/health.ts — 探活

- **用途**：`probe(s)` 探测单个服务是否存活。后端（actuator）读 JSON 含 `"status":"UP"`；前端（http）看 `res.ok`。被 `status.ts` / `run.ts` 复用。
- **入参**：`Service`
- **出参**：`Promise<boolean>`

### scripts/services.ts — 服务清单

- **用途**：open-app 各服务的 `name`/`desc`/`port`/`healthPath`/`urlPath`/`probe`/`type`/`startCmd`（数据源：根目录 `README.md` + 各工程原启动逻辑，已用 TS 复刻）；提供 `repoRoot()` 定位仓库根、`serviceDir(s)` 取服务目录。
- **入参**：无
- **出参**：`Service[]`；`findService(name)`；`serviceDir(s)` 返回服务目录绝对路径

### 执行方式

```bash
# 已 npm link 后可直接用全局命令（跨平台，无需 bash）：
open-app-cli restart --service open-server
open-app-cli stop --service open-server
open-app-cli status
open-app-cli dashboard            # 启动看板 http://127.0.0.1:8899
open-app-cli dashboard stop       # 停止看板进程
open-app-cli dashboard restart    # 重启看板进程
open-app-cli tui                  # 终端看板（需真实 TTY）

# 或不经 npm link：在技能目录下直接经 tsx 运行（相对脚本）：
npx tsx scripts/cli.ts restart --service open-server
```

### 自动补全启用

```bash
# bash（Linux/macOS）—— 已写入 ~/.bashrc，新终端生效
echo 'source <(open-app-cli completion bash)' >> ~/.bashrc

# zsh（macOS 默认）
echo 'autoload -Uz compinit && compinit; source <(open-app-cli completion zsh)' >> ~/.zshrc

# PowerShell（Windows）
open-app-cli completion powershell | Out-String | Invoke-Expression   # 永久写入 $PROFILE
```

> 说明：命令本体跨平台（node）；自动补全 bash/zsh 覆盖 Linux/macOS，powershell 覆盖 Windows。CMD 无程序参数补全机制。

> **新增条目约定**：往 `scripts/` 加操作时，`cli.ts` 增子命令分支、`services.ts` 扩展服务元数据、`run.ts`/`dashboard.ts` 视需要扩展，并在「函数契约」段补用途/入参/出参。
