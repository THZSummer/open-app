import type { Service } from './services.js'

const TIMEOUT_MS = 2500

/** 探测单个服务是否存活（纯 TS，无外部依赖） */
export async function probe(s: Service): Promise<boolean> {
  const url = `http://127.0.0.1:${s.port}${s.healthPath}`
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  try {
    const res = await fetch(url, { signal: controller.signal })
    if (s.probe === 'actuator') {
      const body = await res.text()
      return body.includes('"status":"UP"')
    }
    return res.ok
  } catch {
    return false
  } finally {
    clearTimeout(timer)
  }
}
