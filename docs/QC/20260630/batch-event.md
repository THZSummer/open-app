# 批 event：事件注册

> 1 文件（EventService 515行）。grep 安全模式扫描（确认无注入/密钥/printStackTrace），未逐行读业务逻辑（context 极限，待新会话补）。

## 文件覆盖表（1/1）
| 文件 | 方式 | 问题数 |
|------|:---:|:---:|
| service/EventService.java(515) | 扫描(grep) | 1 |

## 意见 1
- 大类：安全编码 / 子类：关键资源权限分配不当 / 级别：严重
- 问题原因：EventController 用 @PlatformAdminPermission（C1 空校验，6 接口），事件注册管理对任意用户开放（已计入 C1 影响面）
- 修改建议：见 C1

## 结论
EventService 515 行未逐行读（context 极限）。grep 确认无注入/密钥/printStackTrace。Controller 鉴权问题已计入 C1。**待新会话逐行补审 EventService 业务逻辑。**
