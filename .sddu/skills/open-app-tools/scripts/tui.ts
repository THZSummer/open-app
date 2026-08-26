import { createRequire } from 'node:module'
import { spawn } from 'node:child_process'
import { existsSync, openSync, readSync, statSync, closeSync, appendFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { services, findService, serviceDir } from './services.js'
import { probe } from './health.js'

// blessed 是 CommonJS 模块，用 createRequire 以兼容 ESM
const require = createRequire(import.meta.url)
const blessed = require('blessed') as typeof import('blessed')

const BIN = () => fileURLToPath(new URL('../bin/open-app-cli.js', import.meta.url))
const REFRESH_MS = 2000
const LOG_TAIL_BYTES = 16 * 1024

/** 读服务日志尾部（复用 CLI 逻辑，纯 TS） */
function readLogTail(name: string, tailBytes: number): string {
  const s = findService(name)
  if (!s) return '[unknown service]'
  const file = join(serviceDir(s), 'logs', `${name}.log`)
  if (!existsSync(file)) return '[no log file yet]'
  const size = statSync(file).size
  const readLen = Math.min(size, tailBytes)
  const buf = Buffer.alloc(readLen)
  const fd = openSync(file, 'r')
  readSync(fd, buf, 0, readLen, size - readLen)
  closeSync(fd)
  return buf.toString('utf8')
}

/** 触发启停/重启：spawn 子进程 detached 执行，输出丢弃，不阻塞/污染 TUI */
function triggerOp(action: string, name: string) {
  const child = spawn(process.execPath, [BIN(), action, '--service', name], {
    detached: true,
    stdio: ['ignore', 'ignore', 'ignore'],
  })
  child.unref()
}

/**
 * 终端看板（TUI）：状态监控 + 启停/重启 + 实时日志 + 快捷键。
 * 界面文本使用 ASCII，避免终端编码/字体导致乱码（????）。
 * 运行方式：open-app-cli tui
 */
export async function tuiCmd(): Promise<number> {
  // blessed 需要真实 TTY；非 TTY 环境下给出提示并退出
  if (!process.stdin.isTTY) {
    console.log('[open-app-cli] tui requires an interactive terminal (TTY).')
    return 1
  }

  const screen = blessed.screen({ smartCSR: true, title: 'open-app Service Dashboard (TUI)' })

  // 全局兜底：未处理异常/拒绝不崩 TUI，记录到日志文件（避免 unhandledRejection 导致进程退出）
  const DBG = fileURLToPath(new URL('../.tui.log', import.meta.url))
  process.on('unhandledRejection', (e) => { try { appendFileSync(DBG, `unhandledRejection: ${e}\n`) } catch { /* ignore */ } })
  process.on('uncaughtException', (e) => { try { appendFileSync(DBG, `uncaughtException: ${e?.stack ?? e}\n`) } catch { /* ignore */ } })

  // 健壮退出：q/Q/escape/C-c + SIGINT 兜底
  screen.key(['q', 'Q', 'escape', 'C-c'], () => {
    screen.destroy()
    process.exit(0)
  })
  process.on('SIGINT', () => process.exit(0))

  const title = blessed.box({
    top: 0, left: 0, width: '100%', height: 3,
    tags: true,
    content: '{bold}open-app Service Dashboard{/bold}   j/k select | s start | x stop | r restart | l log | R refresh | q quit',
    style: { fg: 'white', bg: 'blue' },
  })

  const list = blessed.list({
    top: 3, left: 0, width: '55%', height: '100%-7',
    tags: true,
    // 不用 keys/vi：避免 list 内建处理 j/k/方向键与 screen.key 重复，导致一次跳两格
    mouse: true,
    border: { type: 'line' },
    style: {
      border: { fg: 'gray' },
      selected: { bg: 'blue', fg: 'white' },
      focus: { border: { fg: 'cyan' } },
    },
  })

  const logBox = blessed.box({
    top: 3, left: '55%', width: '45%', height: '100%-7',
    tags: true,
    label: ' Live Log (press l) ',
    border: { type: 'line' },
    scrollable: true,
    alwaysScroll: true,
    mouse: true,
    style: { border: { fg: 'gray' } },
  })

  const toast = blessed.box({
    bottom: 0, left: 0, width: '100%', height: 3,
    tags: true,
    content: '',
    style: { fg: 'yellow' },
  })

  screen.append(title)
  screen.append(list)
  screen.append(logBox)
  screen.append(toast)

  let selectedName = services[0]?.name ?? ''
  let rowNames: string[] = []
  let logsVisible = false
  let lastLogLen = -1

  function showToast(msg: string) {
    toast.setContent(' {bold}' + msg + '{/bold}')
    setTimeout(() => { if (!screen.destroyed) toast.setContent('') }, 2500)
  }

  /** 刷新服务状态表 */
  async function refresh() {
    const selIdx = list.selected
    const rows = await Promise.all(
      services.map(async (s) => ({ s, running: await probe(s) })),
    )
    rowNames = rows.map(({ s }) => s.name)
    const items = rows.map(({ s, running }) =>
      `${s.name.padEnd(14)} ${s.type.padEnd(8)} port ${String(s.port).padEnd(5)} ${running ? '{green-fg}RUNNING{/green-fg}' : '{red-fg}DOWN{/red-fg}'}`,
    )
    list.setItems(items)
    list.select(Math.min(selIdx, Math.max(0, items.length - 1)))
    selectedName = rowNames[list.selected] ?? services[0]?.name ?? ''
    screen.render()
  }

  /** 更新选中服务日志（若日志视图开启） */
  function updateLog() {
    if (!logsVisible || !selectedName) return
    const content = readLogTail(selectedName, LOG_TAIL_BYTES)
    if (content.length !== lastLogLen) {
      logBox.setContent(content)
      lastLogLen = content.length
      screen.render()
    }
  }

  function toggleLog() {
    logsVisible = !logsVisible
    logBox.setLabel(logsVisible ? ' Live Log: ' + selectedName + ' ' : ' Live Log (press l) ')
    if (logsVisible) { lastLogLen = -1; updateLog() }
    else logBox.setContent('')
    screen.render()
  }

  // 操作后刷新状态（异步触发，操作由子进程后台执行）
  function doAction(action: string) {
    if (!selectedName) return
    triggerOp(action, selectedName)
    showToast(action + ' ' + selectedName + ' triggered (background)')
    setTimeout(() => { refresh().catch(() => {}) }, 1000)
  }

  screen.key(['j', 'down'], () => { list.down(); selectedName = rowNames[list.selected] ?? selectedName; if (logsVisible) { lastLogLen = -1; updateLog() } })
  screen.key(['k', 'up'], () => { list.up(); selectedName = rowNames[list.selected] ?? selectedName; if (logsVisible) { lastLogLen = -1; updateLog() } })
  screen.key(['s'], () => doAction('start'))
  screen.key(['x'], () => doAction('stop'))
  screen.key(['r'], () => doAction('restart'))
  screen.key(['l'], toggleLog)
  screen.key(['R', 'f5'], () => { showToast('refreshed'); refresh().catch(() => {}) })

  list.focus()
  await refresh().catch(() => {})
  const statusTimer = setInterval(() => { refresh().catch(() => {}) }, REFRESH_MS)
  const logTimer = setInterval(updateLog, 1000)

  // 常驻直到退出
  await new Promise<void>((resolve) => {
    screen.on('destroy', () => {
      clearInterval(statusTimer)
      clearInterval(logTimer)
      resolve()
    })
  })
  return 0
}
