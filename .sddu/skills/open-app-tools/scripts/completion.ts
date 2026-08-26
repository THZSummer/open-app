import { services } from './services.js'

const CMDS = 'start stop restart status dashboard tui completion list help'
const SERVICE_NAMES = services.map((s) => s.name).join(' ')

function bashScript(): string {
  return [
    '# bash completion for open-app-cli',
    `_open_app_cli() {`,
    `  local cur prev`,
    `  cur="\${COMP_WORDS[COMP_CWORD]}"`,
    `  prev="\${COMP_WORDS[COMP_CWORD-1]}"`,
    `  local commands="${CMDS}"`,
    `  local services="${SERVICE_NAMES}"`,
    ``,
    `  # 第一层：子命令`,
    `  if [[ \${COMP_CWORD} -eq 1 ]]; then`,
    `    COMPREPLY=( \$(compgen -W "\${commands}" -- "\${cur}") )`,
    `    return 0`,
    `  fi`,
    ``,
    `  # --service 参数：补全服务名`,
    `  case "\${prev}" in`,
    `    --service|-s) COMPREPLY=( \$(compgen -W "\${services}" -- "\${cur}") ); return 0 ;;`,
    `  esac`,
    ``,
    `  # 第二层：按子命令补全参数`,
    `  case "\${COMP_WORDS[1]}" in`,
    `    dashboard) COMPREPLY=( \$(compgen -W "stop restart --port -p" -- "\${cur}") ); return 0 ;;`,
    `    start|stop|restart) COMPREPLY=( \$(compgen -W "--service -s --dry-run" -- "\${cur}") ); return 0 ;;`,
    `    status) COMPREPLY=( \$(compgen -W "--service -s" -- "\${cur}") ); return 0 ;;`,
    `  esac`,
    ``,
    `  COMPREPLY=( \$(compgen -f -- "\${cur}") )`,
    `}`,
    `complete -F _open_app_cli open-app-cli`,
  ].join('\n')
}

function zshScript(): string {
  return [
    '#compdef open-app-cli',
    `_open_app_cli() {`,
    `  local -a commands services`,
    `  commands=(${CMDS})`,
    `  services=(${SERVICE_NAMES})`,
    ``,
    `  if (( CURRENT == 2 )); then`,
    `    _describe 'command' commands`,
    `    return`,
    `  fi`,
    ``,
    `  case "\${words[CURRENT-1]}" in`,
    `    --service|-s) _describe 'service' services; return ;;`,
    `  esac`,
    ``,
    `  case "\${words[2]}" in`,
    `    dashboard) _values 'action' stop restart '--port' '-p' ;;`,
    `    start|stop|restart) _values 'flag' '--service' '-s' '--dry-run' ;;`,
    `    status) _values 'flag' '--service' '-s' ;;`,
    `  esac`,
    `}`,
    `compdef _open_app_cli open-app-cli`,
  ].join('\n')
}

function powershellScript(): string {
  const svcs = SERVICE_NAMES.split(' ')
  const list = svcs.map((s) => `'${s}'`).join(',')
  return [
    '# PowerShell completion for open-app-cli',
    `Register-ArgumentCompleter -Native -CommandName 'open-app-cli' -ScriptBlock {`,
    `    param(\$wordToComplete, \$commandAst, \$cursorPosition)`,
    `    \$Commands = @('start','stop','restart','status','dashboard','tui','completion','list','help')`,
    `    \$Services = @(${list})`,
    `    \$tokens = @(\$commandAst.CommandElements | ForEach-Object { \$_.ToString() })`,
    `    \$prev = \$tokens[-2]`,
    `    \$candidates = @()`,
    ``,
    `    if (\$tokens.Count -le 2) {`,
    `        \$candidates += \$Commands | Where-Object { \$_ -like "\$wordToComplete*" }`,
    `    } elseif (\$prev -eq '--service' -or \$prev -eq '-s') {`,
    `        \$candidates += \$Services | Where-Object { \$_ -like "\$wordToComplete*" }`,
    `    } elseif (\$tokens[1] -eq 'dashboard') {`,
    `        \$candidates += 'stop','restart','--port','-p' | Where-Object { \$_ -like "\$wordToComplete*" }`,
    `    } elseif (\$tokens[1] -in @('start','stop','restart')) {`,
    `        \$candidates += '--service','-s','--dry-run' | Where-Object { \$_ -like "\$wordToComplete*" }`,
    `    } elseif (\$tokens[1] -eq 'status') {`,
    `        \$candidates += '--service','-s' | Where-Object { \$_ -like "\$wordToComplete*" }`,
    `    }`,
    ``,
    `    \$candidates | ForEach-Object {`,
    `        [System.Management.Automation.CompletionResult]::new(\$_, \$_, 'ParameterValue', \$_)`,
    `    }`,
    `}`,
  ].join('\n')
}

/**
 * 输出 shell 自动补全脚本到 stdout。
 * @param shell bash | zsh | powershell（缺省 bash）
 */
export function completionCmd(shell: string): number {
  const s = shell.toLowerCase()
  if (s === 'zsh') process.stdout.write(zshScript())
  else if (s === 'powershell' || s === 'pwsh') process.stdout.write(powershellScript())
  else process.stdout.write(bashScript())
  process.stdout.write('\n')
  return 0
}
