import { spawn, execFileSync } from 'node:child_process'
import { existsSync, mkdirSync, openSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { findService, serviceDir, type Service } from './services.js'
import { probe } from './health.js'

/** 服务 PID 文件与日志文件，均落在服务目录内 */
const pidFile = (s: Service) => join(serviceDir(s), '.pid')
const logFile = (s: Service) => join(serviceDir(s), 'logs', `${s.name}.log`)

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

function isAlive(pid: number): boolean {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

/** 跨平台查找占用端口的进程 PID（lsof for unix；netstat -ano for windows） */
export function pidsOnPort(port: number): number[] {
  try {
    if (process.platform === 'win32') {
      const out = execFileSync('netstat', ['-ano'], { encoding: 'utf8' })
      const pids = new Set<number>()
      for (const line of out.split(/\r?\n/)) {
        if (!/LISTENING/i.test(line)) continue
        if (!line.includes(`:${port}`) && !line.includes(`.${port}`)) continue
        const pid = parseInt(line.trim().split(/\s+/).pop() ?? '', 10)
        if (Number.isFinite(pid)) pids.add(pid)
      }
      return [...pids]
    }
    const out = execFileSync('lsof', ['-ti', `:${port}`], { encoding: 'utf8' })
    return out.split(/\s+/).filter(Boolean).map(Number)
  } catch {
    return []
  }
}

async function killPids(pids: number[]) {
  for (const pid of pids) {
    try { process.kill(pid, 'SIGTERM') } catch { /* ignore */ }
  }
  await sleep(1000)
  for (const pid of pids) {
    try { process.kill(pid, 'SIGKILL') } catch { /* ignore */ }
  }
}

function resolveProfile(s: Service): string {
  return process.env.SPRING_PROFILES_ACTIVE || 'dev'
}

/** 启动服务：spawn mvn/npm（detached）→ 写 .pid → 等就绪 */
export async function start(serviceName: string, dryRun = false): Promise<number> {
  const s = findService(serviceName)
  if (!s) {
    console.error(`[open-app-cli] 未知服务「${serviceName}」`)
    return 1
  }
  const dir = serviceDir(s)
  const profile = resolveProfile(s)

  if (dryRun) {
    console.log(`[open-app-cli] $ cd ${dir} && ${s.startCmd.file} ${s.startCmd.args.map((a) => a.replaceAll('{profile}', profile)).join(' ')}`)
    console.log('[open-app-cli][dry-run] 跳过执行')
    return 0
  }

  console.log(`[open-app-cli] 启动 ${s.name}（${s.desc}, 端口 ${s.port}）`)
  console.log(`[open-app-cli] $ cd ${dir} && ${s.startCmd.file} ${s.startCmd.args.map((a) => a.replaceAll('{profile}', profile)).join(' ')}`)

  mkdirSync(join(dir, 'logs'), { recursive: true })
  const fd = openSync(logFile(s), 'a')
  const argv = s.startCmd.args.map((a) => a.replaceAll('{profile}', profile))
  const child = spawn(s.startCmd.file, argv, {
    cwd: dir,
    detached: true,
    stdio: ['ignore', fd, fd],
  })
  child.unref()
  writeFileSync(pidFile(s), String(child.pid))
  console.log(`[open-app-cli] PID: ${child.pid}，日志: ${logFile(s)}`)

  console.log('[open-app-cli] 等待就绪...')
  const ok = await waitReady(s)
  if (ok) {
    console.log(`[open-app-cli] ✅ ${s.name} 就绪 http://127.0.0.1:${s.port}${s.healthPath}`)
    return 0
  }
  console.error(`[open-app-cli] ⚠️  超时未就绪，查看日志: ${logFile(s)}`)
  return 1
}

/** 停止服务：读 .pid → kill → 端口兜底强杀（纯 TS + process.kill） */
export async function stop(serviceName: string, dryRun = false): Promise<number> {
  const s = findService(serviceName)
  if (!s) {
    console.error(`[open-app-cli] 未知服务「${serviceName}」`)
    return 1
  }
  if (dryRun) {
    console.log(`[open-app-cli] 停止 ${s.name}（端口 ${s.port}）：kill .pid + 端口兜底`)
    console.log('[open-app-cli][dry-run] 跳过执行')
    return 0
  }

  console.log(`[open-app-cli] 停止 ${s.name}（${s.desc}, 端口 ${s.port}）`)
  let stopped = false
  const pf = pidFile(s)

  if (existsSync(pf)) {
    const pid = Number(readFileSync(pf, 'utf8').trim())
    if (Number.isFinite(pid) && isAlive(pid)) {
      console.log(`[open-app-cli] 停止 PID ${pid}...`)
      await killPids([pid])
      stopped = true
    }
    rmSync(pf, { force: true })
  }

  // 端口兜底：清理任何仍占用端口的残留进程（mvn 的子 JVM 可能失联）
  const linger = pidsOnPort(s.port)
  if (linger.length > 0) {
    console.log(`[open-app-cli] 清理残留进程 ${linger.join(', ')}...`)
    await killPids(linger)
    stopped = true
  }

  console.log(stopped ? '[open-app-cli] ✅ 已停止' : '[open-app-cli] ℹ️  未在运行')
  return 0
}

/** 重启 = 停止 + 启动 */
export async function restart(serviceName: string, dryRun = false): Promise<number> {
  if (dryRun) {
    console.log(`[open-app-cli] 重启 ${serviceName}（先 stop 再 start）`)
    console.log('[open-app-cli][dry-run] 跳过执行')
    return 0
  }
  const s = findService(serviceName)
  if (!s) {
    console.error(`[open-app-cli] 未知服务「${serviceName}」`)
    return 1
  }
  console.log(`\n[open-app-cli] ==== 重启 ${s.name} ====`)
  await stop(serviceName)
  console.log('\n[open-app-cli] ---- 重新启动 ----')
  return start(serviceName)
}

async function waitReady(s: Service, timeoutMs = 60000): Promise<boolean> {
  const begin = Date.now()
  while (Date.now() - begin < timeoutMs) {
    await sleep(2000)
    if (await probe(s)) return true
  }
  return false
}
