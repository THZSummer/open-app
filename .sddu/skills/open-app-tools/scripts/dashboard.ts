import { createServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { existsSync, openSync, readSync, statSync, closeSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { join } from 'node:path'
import { services, findService, serviceDir } from './services.js'
import { probe } from './health.js'
import { start, stop, restart, pidsOnPort } from './run.js'

const DASHBOARD_PORT = Number(process.env.OPEN_APP_DASHBOARD_PORT || 8899)
const MAX_TAIL_BYTES = 2 * 1024 * 1024 // tail 单次最多读 2MB，防 OOM
const SSE_CHUNK_BYTES = 64 * 1024       // SSE 单次增量分块 64KB，防大 Buffer

/** dashboard 进程 PID 文件（技能目录内 .dashboard.pid，相对本技能工作目录） */
const dashPidFile = () => fileURLToPath(new URL('../.dashboard.pid', import.meta.url))
/** dashboard 进程日志文件（技能目录内 .dashboard.log，供 restart 后查看） */
const dashLogFile = () => fileURLToPath(new URL('../.dashboard.log', import.meta.url))

function sendJson(res: ServerResponse, code: number, data: unknown) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(data))
}

const logsPath = (name: string) => {
  const s = findService(name)
  return s ? join(serviceDir(s), 'logs', `${name}.log`) : ''
}

async function statusPayload() {
  return Promise.all(
    services.map(async (s) => ({
      name: s.name,
      desc: s.desc,
      port: s.port,
      type: s.type,
      urlPath: s.urlPath,
      running: await probe(s),
    })),
  )
}

/** 后台异步触发，捕获未处理 rejection（服务 detached 后台运行，不占用请求/命令行） */
function trigger(p: Promise<number>) {
  void p.catch((e) => console.error('[open-app-cli] 后台操作错误:', e?.message ?? e))
}

/** 执行操作：start/restart 异步触发立即返回；stop 同步等待快速完成 */
async function runAction(action: string, name: string): Promise<{ action: string; name: string; success: boolean; status: string; code: number; message?: string }> {
  const s = findService(name)
  if (!s) return { action, name, success: false, status: 'error', code: 1, message: '未知服务' }
  if (action === 'start' || action === 'restart') {
    trigger(action === 'start' ? start(name) : restart(name))
    const verb = action === 'restart' ? '重启' : '启动'
    return { action, name, success: true, status: 'triggered', code: 0, message: `${name} ${verb}已触发，后台执行中…` }
  }
  const code = await stop(name)
  return { action, name, success: code === 0, status: code === 0 ? 'done' : 'failed', code, message: code === 0 ? `${name} 已停止` : `${name} 停止失败` }
}

/** 读取日志末尾 tailBytes 字节（上限 MAX_TAIL_BYTES，防 OOM） */
function logTailPayload(name: string, tailBytes: number) {
  const s = findService(name)
  if (!s) return { error: 'unknown', name }
  const file = logsPath(name)
  if (!file || !existsSync(file)) return { name, exists: false, content: '' }
  const size = statSync(file).size
  const safeTail = Math.max(1, Math.min(tailBytes || 6000, MAX_TAIL_BYTES))
  const readLen = Math.min(size, safeTail)
  const buf = Buffer.alloc(readLen)
  const fd = openSync(file, 'r')
  readSync(fd, buf, 0, readLen, size - readLen)
  closeSync(fd)
  return { name, exists: true, size, content: buf.toString('utf8') }
}

/** SSE 实时日志：持续读文件增量推送（单次增量分块 64KB，防大 Buffer OOM） */
function streamLogs(req: IncomingMessage, res: ServerResponse, name: string) {
  const s = findService(name)
  if (!s) {
    sendJson(res, 404, { error: 'unknown', name })
    return
  }
  const file = logsPath(name)
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
  })
  res.write('retry: 2000\n\n')
  let pos = file && existsSync(file) ? statSync(file).size : 0
  const timer = setInterval(() => {
    if (!file || !existsSync(file)) return
    const size = statSync(file).size
    if (size > pos) {
      let remaining = size - pos
      const parts: string[] = []
      const fd = openSync(file, 'r')
      while (remaining > 0) {
        const n = Math.min(remaining, SSE_CHUNK_BYTES)
        const buf = Buffer.alloc(n)
        readSync(fd, buf, 0, n, pos)
        pos += n
        remaining -= n
        parts.push(buf.toString('utf8'))
      }
      closeSync(fd)
      res.write('data: ' + JSON.stringify(parts.join('')) + '\n\n')
    }
  }, 800)
  req.on('close', () => {
    clearInterval(timer)
    try { res.end() } catch { /* ignore */ }
  })
}

const PAGE = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>open-app 服务看板</title>
<style>
  * { box-sizing: border-box }
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif; margin: 0; background: #f5f6fa; color: #2f3542; }
  header { background: #2f3542; color: #fff; padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; }
  header h1 { font-size: 18px; margin: 0 }
  header .meta { font-size: 12px; opacity: .8 }
  main { max-width: 1000px; margin: 24px auto; padding: 0 16px }
  .toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
  .toolbar button { padding: 8px 16px; border: none; border-radius: 6px; cursor: pointer; background: #57606f; color: #fff; font-size: 13px; }
  .toolbar button:hover { background: #454c59 }
  .toolbar .autoref { font-size: 12px; color: #57606f; display: flex; align-items: center; gap: 6px; }
  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
  th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
  th { background: #fafbfc; color: #57606f; font-weight: 600; }
  td.dot .pill { padding: 4px 10px; border-radius: 12px; font-weight: 600; font-size: 12px; }
  .pill.up { background: #d4f5dc; color: #1e7a34 }
  .pill.down { background: #ffe0e0; color: #b3271e }
  .ops button { margin-right: 6px; padding: 6px 10px; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
  .ops button:disabled { opacity: .4; cursor: not-allowed }
  .ops .start { background: #2ed573; color: #fff }
  .ops .stop { background: #ff6b6b; color: #fff }
  .ops .restart { background: #57606f; color: #fff }
  .ops .log { background: #3b82f6; color: #fff }
  .ops a.open { display: inline-block; margin-right: 6px; padding: 6px 10px; border-radius: 6px; font-size: 12px; text-decoration: none; background: #9b59b6; color: #fff; }
  .ops a.open:hover { background: #7d3c98 }
  .ops a.open[disabled] { opacity: .4; pointer-events: none }
  footer { text-align: center; color: #99a1aa; font-size: 11px; padding: 20px; }
  .toast { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: #2f3542; color: #fff; padding: 10px 18px; border-radius: 8px; font-size: 13px; opacity: 0; transition: opacity .3s; }
  .toast.show { opacity: .95 }
  .logpanel { position: fixed; right: 16px; bottom: 16px; width: 600px; max-height: 70vh; background: #1e1e1e; color: #d6d6d6; border-radius: 10px; box-shadow: 0 8px 30px rgba(0,0,0,.4); display: flex; flex-direction: column; z-index: 100; font-family: Menlo, Consolas, monospace; }
  .loghead { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: #2a2a2a; border-radius: 10px 10px 0 0; }
  .loghead span { font-size: 13px; color: #eee }
  .loghead button { background: #57606f; color: #fff; border: none; border-radius: 6px; padding: 4px 10px; cursor: pointer; font-size: 12px }
  .logbody { padding: 10px 14px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; font-size: 12px; line-height: 1.5; }
</style>
</head>
<body>
<header>
  <h1>open-app 服务看板</h1>
  <span class="meta" id="meta">加载中…</span>
</header>
<main>
  <div class="toolbar">
    <button onclick="refresh()">刷新</button>
    <label class="autoref"><input type="checkbox" id="auto" onchange="toggleAuto()"> 自动刷新</label>
    <span class="meta" id="count"></span>
  </div>
  <table>
    <thead>
      <tr><th>服务</th><th>类型</th><th>端口</th><th>状态</th><th>操作</th></tr>
    </thead>
    <tbody id="rows"></tbody>
  </table>
</main>
<footer>open-app-cli dashboard · 纯 TS · 不依赖工程脚本</footer>
<div class="toast" id="toast"></div>
<div class="logpanel" id="logpanel" hidden>
  <div class="loghead">
    <span id="logtitle">日志</span>
    <button id="logclose">关闭</button>
  </div>
  <div class="logbody" id="logbody"></div>
</div>

<script>
const $ = (id) => document.getElementById(id)
let timer = null
let es = null

function toast(msg) {
  const t = $('toast')
  t.textContent = msg
  t.classList.add('show')
  setTimeout(() => t.classList.remove('show'), 2500)
}

function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

async function load() {
  const res = await fetch('/api/status')
  const list = await res.json()
  const rows = list.map((s) => {
    const pill = s.running ? '<span class="pill up">RUNNING</span>' : '<span class="pill down">DOWN</span>'
    return '<tr>' +
      '<td><strong>' + esc(s.name) + '</strong><br><small>' + esc(s.desc) + '</small></td>' +
      '<td>' + esc(s.type) + '</td>' +
      '<td>' + s.port + '</td>' +
      '<td class="dot">' + pill + '</td>' +
      '<td class="ops">' +
        '<a class="open" target="_blank" href="http://' + location.hostname + ':' + s.port + s.urlPath + '" title="' + esc(s.urlPath) + '">访问</a>' +
        '<button class="start" data-action="start" data-name="' + esc(s.name) + '">启动</button>' +
        '<button class="stop" data-action="stop" data-name="' + esc(s.name) + '">停止</button>' +
        '<button class="restart" data-action="restart" data-name="' + esc(s.name) + '">重启</button>' +
        '<button class="log" data-log="' + esc(s.name) + '">日志</button>' +
      '</td></tr>'
  }).join('')
  $('rows').innerHTML = rows
  const up = list.filter((s) => s.running).length
  $('count').textContent = up + ' / ' + list.length + ' 运行中'
  $('meta').textContent = new Date().toLocaleTimeString()
  bind()
}

function bind() {
  document.querySelectorAll('.ops button[data-action]').forEach(function (b) {
    b.addEventListener('click', function () { operate(b.dataset.action, b.dataset.name) })
  })
  document.querySelectorAll('.ops button[data-log]').forEach(function (b) {
    b.addEventListener('click', function () { openLog(b.dataset.log) })
  })
}

async function operate(action, name) {
  const btns = document.querySelectorAll('button')
  btns.forEach((b) => (b.disabled = true))
  try {
    const res = await fetch('/api/' + action + '/' + name, { method: 'POST' })
    const d = await res.json()
    if (d.status === 'triggered') {
      toast('⏳ ' + name + ' ' + action + ' 已触发，后台执行中…')
    } else {
      toast((d.code === 0 ? '✅ ' : '❌ ') + action + ' ' + name)
    }
  } catch (e) {
    toast('❌ ' + action + ' ' + name + ' 请求失败')
  }
  btns.forEach((b) => (b.disabled = false))
  await load()
}

function openLog(name) {
  const panel = $('logpanel')
  panel.hidden = false
  $('logtitle').textContent = name + ' 日志'
  if (es) { es.close(); es = null }
  $('logbody').textContent = '加载中…'
  fetch('/api/logs/' + name + '?tail=6000')
    .then(function (r) { return r.json() })
    .then(function (d) {
      $('logbody').textContent = d.exists ? d.content : '【暂无日志文件】'
      trimBody()
      $('logbody').scrollTop = $('logbody').scrollHeight
    })
  es = new EventSource('/api/logs/' + name + '/stream')
  es.onmessage = function (e) {
    try {
      $('logbody').textContent += JSON.parse(e.data)
      trimBody()
      $('logbody').scrollTop = $('logbody').scrollHeight
    } catch (err) { /* ignore */ }
  }
}

// 日志面板 DOM 文本上限 1MB，超出只保留尾部，防长时间运行内存/页面 OOM
function trimBody() {
  const b = $('logbody')
  const MAX = 1048576
  if (b.textContent.length > MAX) {
    b.textContent = b.textContent.slice(-MAX)
  }
}

function closeLog() {
  if (es) { es.close(); es = null }
  $('logpanel').hidden = true
}

function refresh() { load() }

function toggleAuto() {
  if ($('auto').checked) {
    if (timer) clearInterval(timer)
    timer = setInterval(load, 5000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

$('logclose').addEventListener('click', closeLog)
load()
</script>
</body>
</html>`

export function dashboardCmd(port = DASHBOARD_PORT): Promise<number> {
  const server = createServer((req: IncomingMessage, res: ServerResponse) => {
    void handleRequest(req, res)
  })
  server.listen(port, () => {
    // 写自身 PID，供 dashboard stop/restart 使用
    try { writeFileSync(dashPidFile(), String(process.pid)) } catch { /* ignore */ }
    console.log(`[open-app-cli] dashboard 已启动: http://127.0.0.1:${port}`)
    console.log(`[open-app-cli] 按 Ctrl+C 停止`)
  })
  return new Promise((resolve) => {
    server.on('error', (e) => {
      console.error(`[open-app-cli] dashboard 服务器错误: ${e.message}`)
      resolve(1)
    })
  })
}

/** 停止 dashboard 进程：读 .dashboard.pid + 端口兜底 */
export async function dashboardStop(): Promise<number> {
  const pf = dashPidFile()
  const pids = new Set<number>()

  if (existsSync(pf)) {
    const pid = Number(readFileSync(pf, 'utf8').trim())
    if (Number.isFinite(pid) && pid > 0) pids.add(pid)
    rmSync(pf, { force: true })
  }
  // 端口兜底：PID 文件缺失/失效时，清理占用监听端口的进程
  for (const pid of pidsOnPort(DASHBOARD_PORT)) pids.add(pid)

  if (pids.size === 0) {
    console.log('[open-app-cli] dashboard 未在运行')
    return 0
  }
  for (const pid of pids) {
    try {
      process.kill(pid, 'SIGTERM')
      console.log(`[open-app-cli] 已停止 dashboard (PID ${pid})`)
    } catch {
      console.log(`[open-app-cli] dashboard (PID ${pid}) 已不在运行`)
    }
  }
  await new Promise((r) => setTimeout(r, 400))
  // 强杀兜底
  for (const pid of pids) {
    try { process.kill(pid, 0); process.kill(pid, 'SIGKILL') } catch { /* ignore */ }
  }
  return 0
}

/** 重启 dashboard：停止旧进程，detached 重新拉起，日志重定向到 .dashboard.log 便于查看 */
export async function dashboardRestart(): Promise<number> {
  await dashboardStop()
  const bin = fileURLToPath(new URL('../bin/open-app-cli.js', import.meta.url))
  const log = dashLogFile()
  const fd = openSync(log, 'a')
  const child = spawn(process.execPath, [bin, 'dashboard'], { detached: true, stdio: ['ignore', fd, fd] })
  child.unref()
  console.log(`[open-app-cli] dashboard 已重新启动 (PID ${child.pid})`)
  console.log(`[open-app-cli] 日志文件: ${log}`)

  // 等待端口监听确认启动成功
  await new Promise((r) => setTimeout(r, 1500))
  if (pidsOnPort(DASHBOARD_PORT).length > 0) {
    console.log(`[open-app-cli] ✅ dashboard 已就绪: http://127.0.0.1:${DASHBOARD_PORT}`)
    return 0
  }
  console.error(`[open-app-cli] ⚠️ 未能确认就绪，请查看日志: ${log}`)
  console.error('[open-app-cli] 可执行: tail -f ' + log)
  return 1
}

async function handleRequest(req: IncomingMessage, res: ServerResponse) {
  try {
    const url = new URL(req.url ?? '/', `http://${req.headers.host ?? '127.0.0.1'}`)
    const p = url.pathname

    if (req.method === 'GET' && (p === '/' || p === '/index.html')) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
      res.end(PAGE)
      return
    }

    if (req.method === 'GET' && p === '/api/status') {
      sendJson(res, 200, await statusPayload())
      return
    }

    const opMatch = p.match(/^\/api\/(start|stop|restart)\/([^/]+)$/)
    if (req.method === 'POST' && opMatch) {
      const [, action, name] = opMatch
      sendJson(res, 200, await runAction(action, name))
      return
    }

    const streamMatch = p.match(/^\/api\/logs\/([^/]+)\/stream$/)
    if (req.method === 'GET' && streamMatch) {
      streamLogs(req, res, streamMatch[1])
      return
    }

    const logMatch = p.match(/^\/api\/logs\/([^/]+)$/)
    if (req.method === 'GET' && logMatch) {
      const tail = Number(url.searchParams.get('tail') || 6000)
      const payload = logTailPayload(logMatch[1], tail)
      sendJson(res, (payload as any).error ? 404 : 200, payload)
      return
    }

    if (req.method === 'GET' && p === '/api/services') {
      sendJson(res, 200, services.map((s) => ({ name: s.name, desc: s.desc, port: s.port, type: s.type, urlPath: s.urlPath })))
      return
    }

    sendJson(res, 404, { error: 'not_found', path: p })
  } catch (e: any) {
    sendJson(res, 500, { error: 'internal', message: e?.message ?? String(e) })
  }
}
