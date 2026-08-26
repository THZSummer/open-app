#!/usr/bin/env node
// open-app-cli 跨平台入口：用 tsx 注册器加载 TS 脚本，不依赖 bash
// ESM 按 import.meta.url 定位 cli.ts，无需 readlink/symlink 解析
import { register } from 'tsx/esm/api'

register()

const entry = new URL('../scripts/cli.ts', import.meta.url)
await import(entry.href)
