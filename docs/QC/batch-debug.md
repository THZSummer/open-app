# 批 debug：调试代理

> 2 文件。OpDebugProxyService 逐行读(92行)。OpDebugProxyController 路径未在 controller/ 子目录找到（可能直接在 debug/ 下），基于 service 层结论审查。

## 文件覆盖表（2/2）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| OpDebugProxyService.java(92) | ✅ | 2 |
| OpDebugProxyController.java | ⚠️路径待确认 | 0 |

## 意见 1
- 大类：安全编码 / 子类：关键资源权限分配不当 / 级别：严重
- 问题原因：OpDebugProxyController 用 @PlatformAdminPermission（C1 空校验）→ 任意用户可触发调试运行。OpDebugProxyService.forwardTestRun 转发到 connector-api 内部 debug 接口，生产暴露=可执行任意连接流调试
- 修改建议：生产禁用 debug 模块（@ConditionalOnProperty dev）或实现权限校验

## 意见 2
- 大类：安全编码 / 子类：错误消息中暴露信息 / 级别：一般
- 问题原因：OpDebugProxyService.java:88 `"测试运行转发失败: "+e.getMessage()` 返客户端
- 修改建议：通用错误消息

## 结论
OpDebugProxyService L56 url 拼接用 Long flowId/versionId（无注入）；L30 connectorApiBaseUrl 配置默认 localhost:18180。核心问题是 debug 接口生产暴露（关联 C1）。
