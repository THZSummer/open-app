import { findService, services, type Service } from './services.js'
import { probe } from './health.js'

/**
 * 检查服务状态。serviceName 缺省时检查全部服务。
 * @returns 退出码（0=成功；未知服务=1）
 */
export async function statusCmd(serviceName?: string): Promise<number> {
  let targets: Service[]
  if (serviceName) {
    const found = findService(serviceName)
    if (!found) {
      console.error(
        `[open-app-cli] 未知服务「${serviceName}」。可用服务：\n  ` +
          services.map((s) => `${s.name}  (端口 ${s.port}, ${s.desc})`).join('\n'),
      )
      return 1
    }
    targets = [found]
  } else {
    targets = services
  }

  // 并行探测，避免逐个等待超时
  const results = await Promise.all(
    targets.map(async (s) => ({ s, up: await probe(s) })),
  )

  console.log(`open-app 服务状态${serviceName ? `（${serviceName}）` : '（全部）'}：`)
  for (const { s, up } of results) {
    const mark = up ? 'RUNNING' : 'DOWN'
    console.log(`  ${s.name.padEnd(14)} 端口 ${String(s.port).padEnd(5)} ${mark}`)
  }

  const upCount = results.filter((r) => r.up).length
  console.log(`\n${upCount}/${results.length} 运行中`)
  return 0
}
