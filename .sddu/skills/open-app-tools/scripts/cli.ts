import { start, stop, restart } from './run.js'
import { services } from './services.js'
import { statusCmd } from './status.js'
import { dashboardCmd, dashboardStop, dashboardRestart } from './dashboard.js'
import { completionCmd } from './completion.js'
import { tuiCmd } from './tui.js'

const USAGE = `open-app-cli — open-app 服务启停与状态（纯 TS，独立于工程 bash 脚本）

用法:
  open-app-cli start --service <name> [--dry-run]
  open-app-cli stop --service <name> [--dry-run]
  open-app-cli restart --service <name> [--dry-run]
  open-app-cli status [--service <name>]
  open-app-cli dashboard [--port <port>]
  open-app-cli dashboard stop
  open-app-cli dashboard restart
  open-app-cli completion [bash|zsh|powershell]
  open-app-cli tui
  open-app-cli list
  open-app-cli help

子命令:
  start       启动服务（spawn mvn/npm → 写 .pid → 等就绪）
  stop        停止服务（kill .pid + 端口兜底清理）
  restart     重启服务 = stop + start
  status      检查状态（缺省全部，可用 --service 指定单个）
  dashboard   启动 Web 看板（HTTP 服务器，监控 + 操作每个服务）
  dashboard stop      停止 dashboard 进程
  dashboard restart   重启 dashboard 进程
  completion  输出 shell 自动补全脚本（bash/zsh/powershell），配合 source 使用
  tui         终端看板（交互式 TUI：状态监控 + 启停/重启 + 实时日志）
  list        列出可用服务
  help        显示本帮助

参数:
  --service <name>   服务名（wecodesite/market-web/open-server/connector-api/api-server/event-server/market-server）
  --dry-run          只打印将执行的命令，不实际执行
  --port <port>      dashboard 监听端口（默认 8899）

示例:
  open-app-cli restart --service open-server
  open-app-cli status
  open-app-cli dashboard
  open-app-cli dashboard --port 9000
  open-app-cli start --service wecodesite
  open-app-cli stop --service market-web
`

const args = process.argv.slice(2)
const cmd = args[0]

function parseFlags(argv: string[]) {
  const flags: Record<string, string | boolean> = {}
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--service' || a === '-s') {
      flags.service = argv[++i]
    } else if (a === '--port' || a === '-p') {
      flags.port = argv[++i]
    } else if (a === '--dry-run') {
      flags.dryRun = true
    }
  }
  return flags
}

function list() {
  console.log('open-app 可用服务：')
  for (const s of services) {
    console.log(`  ${s.name.padEnd(14)} ${s.type.padEnd(8)} 端口 ${String(s.port).padEnd(5)} ${s.desc}`)
  }
}

async function main() {
  switch (cmd) {
    case 'start':
    case 'stop':
    case 'restart': {
      const flags = parseFlags(args.slice(1))
      if (!flags.service) {
        console.error(`[open-app-cli] ${cmd} 需要 --service <name>`)
        console.error(USAGE)
        process.exit(1)
      }
      const name = String(flags.service)
      const dry = Boolean(flags.dryRun)
      process.exit(await (cmd === 'start' ? start(name, dry) : cmd === 'stop' ? stop(name, dry) : restart(name, dry)))
    }
    case 'status': {
      const flags = parseFlags(args.slice(1))
      process.exit(await statusCmd(flags.service ? String(flags.service) : undefined))
    }
    case 'dashboard': {
      const tokens = args.slice(1).filter((a) => !a.startsWith('-'))
      const flags = parseFlags(args.slice(1))
      const sub = tokens[0]
      if (sub === 'stop') process.exit(await dashboardStop())
      if (sub === 'restart') process.exit(await dashboardRestart())
      // 无子参数 → 启动常驻 dashboard（不 process.exit，进程因监听而保持运行）
      const port = flags.port ? Number(flags.port) : undefined
      const code = await dashboardCmd(port)
      process.exit(code)
    }
    case 'list':
      list()
      process.exit(0)
    case 'completion': {
      const shell = (args[1] || 'bash').replace(/^--/, '')
      process.exit(completionCmd(shell))
    }
    case 'tui': {
      process.exit(await tuiCmd())
    }
    case 'help':
    case '--help':
    case '-h':
    case undefined:
      console.log(USAGE)
      process.exit(0)
    default:
      console.error(`[open-app-cli] 未知命令「${cmd}」`)
      console.error(USAGE)
      process.exit(1)
  }
}

main()
