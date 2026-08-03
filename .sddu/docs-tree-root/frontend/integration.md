# 前端微前端集成 — 第三方集成

> **文档定位**: sddu-docs-integration — 第三方集成文档 — 外部服务、回调、认证方式  
> **输出文件名**: frontend-integration.md  
> **数据来源**: 代码扫描生成 — qiankunProject + wecodesite 依赖 + market-server 配置  
> **创建人**: SDDU Docs Agent  
> **创建时间**: 2026-08-03  
> **版本**: v1.0 (CODE-SCAN)  

## 1. 微前端架构（qiankun 2.10.16）

### 1.1 主应用（qiankunProject/main-app）

- `registerMicroApps(microApps)` + `start()` 在 src/index.jsx
- HashRouter，activeRule 为函数形式（hashRule），从 location.hash 匹配
- 子应用渲染到 `<div id="container">`

### 1.2 子应用注册表

| name | entry | activeRule | 端口 |
|------|-------|-----------|:----:|
| sub-app-b | //localhost.uat.com:5174 | /qiankun/sub-b | 5174 |
| sub-app-c | //localhost.uat.com:8082 | /qiankun/sub-c | 8082 |
| sub-app-d | //localhost.uat.com:8083 | /qiankun/sub-d | 8083 |
| sub-app-e | //localhost.uat.com:5175 | /qiankun/sub-e | 5175 |

### 1.3 接入方式

- **加载类型**（V5 迁移脚本）：能力表 `load_type` 1=路由加载、2=微前端加载
- **子应用标识**：`alias_name`（子应用唯一标识）
- **激活路由**：`route_path`（子应用激活路由）
- **入口地址**：`entry_url`（微前端子应用入口）
- **展示控制**：`hidden` 字段控制开放面是否展示

## 2. 外部服务集成

| 外部服务 | 集成方 | 说明 |
|---------|--------|------|
| 通讯录 API（wecontact） | market-server | 成员选择器/用户搜索，x-welink-tenantid 头 |
| 审批平台 | open-server / market-server | approval-url-prefix 跳转 |
| 内部网关 | api-server (prod) | INTERNAL_GATEWAY_URL |
| 三方能力 API | api-server /gateway/api/** | 能力消费代理 |

## 3. 前端工具集成

| 工具 | 用途 |
|------|------|
| Monaco Editor | 脚本节点 / JSON Schema 编辑（wecodesite） |
| @xyflow/react | 连接流编排画布（React Flow） |
| xlsx | Excel 导入导出（LookUp/字典批量导入） |
| puppeteer | 测试/截图（market-web） |

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 代码扫描全量生成 | 2026-08-03 | SDDU Docs Agent |
