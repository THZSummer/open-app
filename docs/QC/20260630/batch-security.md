# 批 security：应用隔离鉴权基础设施

> 4 文件全部逐行读（AppWhitelistInterceptor 98 + AppDataIsolationAspect 107 在 flow 批读，AppWhitelistService 95 + AppContextHolder 120 本轮读）。

## 文件覆盖表（4/4）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| AppWhitelistInterceptor.java(98) | ✅ | 0 |
| AppWhitelistService.java(95) | ✅ | 0 |
| AppDataIsolationAspect.java(107) | ✅ | 1 |
| AppContextHolder.java(120) | ✅ | 0 |

## 意见 1
- 大类：安全编码 / 子类：关键资源权限分配不当 / 级别：严重
- 问题原因：AppDataIsolationAspect.java:77-80 无 X-App-Id header 时 `skip isolation check`（放行），fail-open。依赖前置 AppWhitelistInterceptor 兜底，但 /executions 等路径未覆盖 Interceptor
- 修改建议：改 fail-closed（无有效 AppContext → 抛异常拒绝）

## 结论
AppWhitelistService L55-58 白名单空/不可用时 return false（**fail-closed 安全默认** ✅）；AppWhitelistInterceptor L62-76 无 header/非白名单 → 403（**fail-closed** ✅）；AppContextHolder requireInternalAppId L69 null 抛异常 ✅；ThreadLocal clear 防泄漏 ✅。唯一问题：AppDataIsolationAspect fail-open（已在 flow 批记录，重复计入）。
