import { fileURLToPath } from 'node:url'
import { join } from 'node:path'

/**
 * open-app 服务清单（纯 TS 元数据，不引用任何工程 bash 脚本）
 * 数据来源：根目录 README.md 工程地图 + 各工程原 restart.sh 的启动/就绪逻辑（已用 TS 复刻）
 */
export interface Service {
  name: string
  desc: string
  port: number
  /** 健康检查相对路径（含 context-path），用于探活 */
  healthPath: string
  /** 用户可访问入口相对路径（如前端主页 / 后端 web 入口） */
  urlPath: string
  /** 探活类型：actuator=Spring Boot(JSON 含 status:UP)；http=HTTP 状态码 */
  probe: 'actuator' | 'http'
  /** 服务类型：backend=Spring Boot(mvn)；frontend=Vite(npm) */
  type: 'backend' | 'frontend'
  /** 启动命令（在服务目录运行）。`{profile}` 会在启动时替换为 SPRING_PROFILES_ACTIVE 或 dev */
  startCmd: { file: string; args: string[] }
}

/**
 * 仓库根目录 = 本脚本所在位置向上 4 级：
 * cli.ts → scripts/ → open-app-tools/ → skills/ → .sddu/ → 仓库根
 * 不依赖调用方 cwd，从脚本自身位置定位，更稳健。
 */
export function repoRoot(): string {
  return fileURLToPath(new URL('../../../../', import.meta.url))
}

const BACKEND = (
  name: string, desc: string, port: number,
  healthPath: string, urlPath: string, extraArgs: string[] = [],
): Service => ({
  name, desc, port, healthPath, urlPath, probe: 'actuator', type: 'backend',
  startCmd: { file: 'mvn', args: ['spring-boot:run', '-Dspring-boot.run.profiles={profile}', ...extraArgs] },
})

const FRONTEND = (name: string, desc: string, port: number, urlPath: string): Service => ({
  name, desc, port, healthPath: urlPath, urlPath, probe: 'http', type: 'frontend',
  startCmd: { file: 'npm', args: ['run', 'dev'] },
})

export const services: Service[] = [
  FRONTEND('wecodesite',    '开放平台主站前端',       5173,  '/'),
  FRONTEND('market-web',    '应用市场前端',           13000, '/market-web/'),
  BACKEND('open-server',   '开放平台主后端服务',     18080, '/open-server/actuator/health', '/open-server'),
  BACKEND('connector-api', '连接流运行时引擎',        18180, '/connector-api/actuator/health', '/connector-api', ['-Dmaven.test.skip=true']),
  BACKEND('api-server',    'API 管理服务',           18081, '/api-server/actuator/health', '/api-server'),
  BACKEND('event-server',  '事件管理服务',           18082, '/event-server/actuator/health', '/event-server'),
  BACKEND('market-server', '应用市场后端服务',        18083, '/market-server/actuator/health', '/market-server', ['-Dspring-boot.run.arguments=--server.port=18083 --server.servlet.context-path=/market-server']),
]

export function findService(name: string): Service | undefined {
  return services.find((s) => s.name === name)
}

/** 服务所在目录 = 仓库根/<工程名> */
export function serviceDir(s: Service): string {
  return join(repoRoot(), s.name)
}
