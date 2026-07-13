# Feature Specification：嵌入能力开放面

> **文档定位**: SDDU 需求规范 — 定义嵌入能力开放面的功能需求、非功能需求和边界情况，作为 plan 阶段的输入  
> **前置依赖**: `specs-tree-ability-embedding/discovery-report.md`、`specs-tree-ability-embedding/spec.md`（父规范）、`specs-tree-embedding-platform/spec.md`（平台面输出）  
> **创建人**: SDDU Spec Agent  
> **创建时间**: 2026-07-13  
> **版本**: v1.0  
> **更新人**: SDDU Spec Agent  
> **更新时间**: 2026-07-13  
> **更新说明**: 初始创建

## 1. 元数据
> Feature 基本信息

| 字段 | 值 |
|------|-----|
| Feature ID | EMBED-OPEN-001 |
| 名称 | 嵌入能力开放面 |
| 父 Feature | EMBED-001（狭义嵌入能力） |
| 优先级 | P1 |
| 服务端 | open-server（ability 模块增强） |
| 前端 | wecodesite（Capabilities 页增强） |
| 目标版本 | v1.0 |
| 前置依赖 | 平台面 `EMBED-PLATFORM-001`（ability 目录数据由平台面产生） |

## 2. 上下文
> 回顾问题背景和目标用户

本子 Feature 聚焦**面向能力消费方的开放面**，分为两大块：

### 2.1 后端层 — open-server ability 模块增强

现有能力：
- `GET /list` — 全量能力列表 + 已订阅标记（`AbilityVO`）
- `POST /` — 订阅能力（`AddAbilityRequest.abilityType`）
- `GET /subscribed` — 已订阅能力详情（`AppAbilityDetailVO`）
- `AbilityServiceImpl.autoSubscribeAfterAbility()` — 空扩展点

存在问题：
- `addAbility` 通过 `AbilityTypeEnum.isValidCode()` 校验，**自定义类型（平台面创建）无法通过校验**
- 无取消订阅接口（订阅后无法退订）
- `autoSubscribeAfterAbility` 未实现，能力订阅后未与 API/事件权限系统打通
- 前端 `ABILITY_TYPE_MAP` / `ABILITY_SCENE_MAP` 硬编码，自定义类型不展示

### 2.2 前端层 — wecodesite Capabilities 页增强

现有能力：
- 能力卡片网格展示（已订阅/未订阅切换）
- 添加能力 → 跳转配置页（目前展示占位文本 "配置页面由能力方提供"）
- 配置页无实际功能

存在问题：
- `ABILITY_TYPE_MAP` 静态 Map，仅含 7 种预设类型
- `ABILITY_SCENE_MAP` 静态分组，自定义类型不在任何场景分组中
- 配置页无法加载嵌入能力方的实际 UI

### 2.3 目标用户

| 角色 | 说明 |
|------|------|
| **三方应用开发方** | 在 wecodesite 浏览能力目录、订阅/取消订阅能力、在配置页操作嵌入能力方提供的 UI |
| **嵌入能力方** | 提供 QianKun 子应用入口，嵌入到 wecodesite 的配置页中 |
| **平台运营方** | 验证能力目录是否正确展示（不直接操作本面） |

## 3. 目标与非目标
> 明确需求范围，防止范围蔓延

### 3.1 目标 (Goals)
> 明确本次要达成的业务目标

| # | 目标描述 | 归属层 |
|---|---------|--------|
| G-001 | 三方应用可浏览完整 ability 目录（含预设 + 自定义类型） | 后端+前端 |
| G-002 | 三方应用可订阅/取消订阅 ability | 后端 |
| G-003 | 已订阅 ability 支持跳转到嵌入能力方提供的配置页面（QianKun 子应用） | 前端 |
| G-004 | 订阅 ability 后自动桥接 API/事件权限关联（`autoSubscribeAfterAbility` 实现） | 后端 |
| G-005 | 前端能力目录动态化：不依赖硬编码常量，由后端数据驱动 | 前端 |

### 3.2 非目标 (Non-Goals)
> 明确本次不涉及的范围，防止需求蔓延

| # | 明确不做 | 原因 |
|---|---------|------|
| NG-001 | 不做能力使用统计与分析 | 属于进阶治理能力，后置 |
| NG-002 | 不修改能力开放平台的 API/事件/回调权限模型 | 通过 `autoSubscribeAfterAbility` 预留扩展点桥接，具体映射规则后置 |
| NG-003 | 不做 QianKun 子应用沙箱/安全隔离方案 | 前端团队单独调研设计实施 |
| NG-004 | 不做依赖 Apollo/配置中心的动态场景分组 | 场景分组当前仍使用静态常量，未来可考虑动态化 |

## 4. 用户故事
> 以用户视角描述功能需求

| # | 作为… | 我想要… | 以便… |
|---|-------|---------|-------|
| US-001 | 三方应用开发方 | 在 wecodesite 的能力页面看到完整的能力目录（包括自定义类型） | 了解有哪些能力可用 |
| US-002 | 三方应用开发方 | 为我的应用订阅一个能力 | 启用该能力 |
| US-003 | 三方应用开发方 | 为我的应用取消订阅一个已订阅的能力 | 不再使用该能力 |
| US-004 | 三方应用开发方 | 在已订阅能力的配置页看到嵌入能力方提供的实际 UI | 完成该能力的配置操作（如设置群置顶内容） |
| US-005 | 嵌入能力方 | 通过提供 QianKun 子应用入口，将配置 UI 嵌入 wecodesite | 让应用方可以配置我的能力 |

## 5. 功能需求 (FR)
> 每个需求必须有唯一标识符且可测试

### 5.1 开放面 — 后端（open-server ability 模块）

| ID | 需求描述 | 验收标准 | 优先级 |
|----|---------|---------|--------|
| FR-001 | **能力目录增强**：`GET /list` 返回所有启用的 ability 类型（含预设 7 类和平台面自定义类型），并扩展返回字段 | • 移除前端 `abilityType !== 6` 的过滤——是否展示应由后端基于配置控制（允许隐藏指定 type）<br/>• 新增字段 `frontendEntryUrl`：有则返回子应用入口 URL，无则返回 null<br/>• 返回完整能力信息（已有字段保持向后兼容）<br/>• 对于预设类型依旧通过 `ABILITY_TYPE_MAP` 常量做本地化展示；对于自定义类型直接使用后端返回的 `nameCn`/`descCn`/`iconUrl` 等 | P0 |
| FR-002 | **订阅校验增强**：`POST /` 订阅时校验 abilityType 是否存在，允许自定义类型 | • 移除 `AbilityTypeEnum.isValidCode()` 的硬编码校验<br/>• 改为查询 DB `openplatform_ability_t` 校验 `ability_type` + `status = 1`<br/>• 自定义类型和预设类型走同一逻辑 | P0 |
| FR-003 | **取消订阅**：新增 `DELETE /` 取消订阅指定 ability | • 按 `(appId, abilityType)` 软删除关联记录（status = 0）<br/>• 取消后该能力在列表中标记为未订阅<br/>• 不可取消不存在的订阅记录，返回 404 | P1 |
| FR-004 | **自动桥接扩展点实现**：实现 `autoSubscribeAfterAbility(appId, abilityType)` | • 订阅能力后触发该扩展点<br/>• 当前阶段输出日志记录 `"Ability subscribed: appId={}, abilityType={}"` 即可<br/>• 预留钩子接口，后续可扩展为自动订阅对应的 API/事件权限 | P1 |
| FR-005 | **已订阅列表增强**：`GET /subscribed` 扩展返回字段 | • 新增字段 `frontendEntryUrl`：从 `openplatform_ability_t.frontend_entry_url` 读取<br/>• 新增字段 `abilityType`：已关联，但当前 VO 缺少排序字段以外的元信息，确保名称等完整返回 | P0 |
| FR-006 | **能力隐藏控制**：支持设置某个 abilityType 在目录中隐藏（如 `GROUP_JOIN_NOTIFICATION` 类型 6 不再需要前端过滤） | • `openplatform_ability_t` 新增字段 `hidden`（tinyint，默认 0）<br/>• 隐藏的能力类型不出现在 `GET /list` 返回中<br/>• 已订阅的不受影响（`GET /subscribed` 正常返回） | P2 |

### 5.2 开放面 — 前端（wecodesite Capabilities 页）

| ID | 需求描述 | 验收标准 | 优先级 |
|----|---------|---------|--------|
| FR-101 | **动态能力目录**：能力卡片列表基于后端 API 数据渲染，不依赖 `ABILITY_TYPE_MAP` 常量 | • 预设类型（1-7）：保留现有展示逻辑（图标/名称可回退到常量）<br/>• 自定义类型（≥100）：使用后端返回的 `nameCn`/`descCn`/`iconUrl` 渲染<br/>• 卡片按钮逻辑不变（未订阅→添加，已订阅→配置） | P0 |
| FR-102 | **已订阅能力配置页 — 嵌入子应用**：点击"配置"进入的能力配置页加载嵌入能力方提供的 QianKun 子应用 | • 从 `frontendEntryUrl` 获取子应用入口<br/>• 动态创建 QianKun 子应用挂载到配置页容器<br/>• 向子应用传递 props：`appId`、`abilityType`、`nameCn`<br/>• 子应用可独立运行（拥有独立路由/状态）<br/>• 无 `frontendEntryUrl` 的能力仍展示占位文本 | P0 |
| FR-103 | **能力场景分组动态化**（可选）：支持由后端控制能力的场景分组 | • 保留现有 `ABILITY_SCENE_MAP` 静态配置作为兜底<br/>• 可扩展为后端返回分组元信息（当前阶段保持静态） | P2 |

### 5.3 接口变更清单

| 接口 | 方法 | 路径 | 变更类型 | 变更说明 |
|------|------|------|---------|---------|
| 能力列表 | GET | `/service/open/v2/ability/list` | **修改** | 无需 `ABILITY_TYPE_MAP` 兜底；新增 `frontendEntryUrl` 字段；移除前端侧边过滤 |
| 订阅能力 | POST | `/service/open/v2/ability` | **修改** | 校验逻辑改为查 DB |
| 取消订阅 | DELETE | `/service/open/v2/ability` | **新增** | 按 `appId` + `abilityType` 取消订阅 |
| 已订阅列表 | GET | `/service/open/v2/ability/subscribed` | **修改** | 新增 `frontendEntryUrl` 等字段 |

### 5.4 数据模型变更

**`openplatform_ability_t` 新增字段**（与平台面共享）：

| 字段名 | 类型 | 说明 | 归属 |
|--------|------|------|------|
| `frontend_entry_url` | VARCHAR(512) | QianKun 子应用入口 URL | 平台面写入，开放面读取 |
| `hidden` | TINYINT(1) | 是否在目录中隐藏（默认 0） | 平台面管理，开放面过滤 |

### 5.5 前端嵌入协议

**QianKun 子应用约定**（嵌入能力方需遵守）：

```
wecodesite（主应用）                         能力方子应用
─────────────────────────────────────     ──────────────────
1. 用户点击"配置"按钮                        
2. 读取 frontendEntryUrl                                    
3. 动态注册 QianKun 子应用                    
    ── mount(props) ───────────────►      接收 props:
                                             { appId, abilityType, nameCn }
4. 渲染到配置页容器                          渲染配置 UI
5. 用户操作完成                                      
    ── unmount() ◄──────────────           清理资源
```

**props 协议**：

| prop | 类型 | 说明 | 示例 |
|------|------|------|------|
| `appId` | string | 当前应用 ID | `wx_app_001` |
| `abilityType` | number | 能力类型编码 | `100` |
| `nameCn` | string | 能力中文名 | `群置顶服务` |

**子应用要求**：
- 支持 QianKun 生命周期（`bootstrap`/`mount`/`unmount`）
- 独立运行时也能正常开发调试（检测 `__POWERED_BY_QIANKUN__`）
- UI 风格尽量与 wecodesite 保持一致（参考 Ant Design）

## 6. 非功能需求 (NFR)
> 性能、安全、可用性等跨切面需求

| ID | 类别 | 需求描述 | 验收标准 |
|----|------|---------|---------|
| NFR-001 | 性能 | 能力列表查询响应时间 | P99 < 200ms（目前已有查询，新增字段不增加额外 DB 查询） |
| NFR-002 | 性能 | 能力订阅/取消订阅响应时间 | P99 < 500ms |
| NFR-003 | 可用性 | 嵌入的 QianKun 子应用加载超时处理 | 子应用加载超过 10s 展示加载失败提示，不影响主应用其他功能 |
| NFR-004 | 兼容性 | API 响应向后兼容 | 新增 `frontendEntryUrl` 字段为 optional，不破坏现有前端代码 |
| NFR-005 | 一致性 | 平台面对 ability 的启禁状态即时反映到开放面 | 禁用状态：不可新订阅；已订阅继续可用 |
| NFR-006 | 可用性 | 取消订阅前二次确认 | 弹窗确认："取消订阅后，该能力的配置数据可能会丢失，确定取消吗？" |

## 7. 边界情况 (EC)
> 异常场景和边界条件的处理方式

| ID | 场景 | 处理方式 |
|----|------|---------|
| EC-001 | 订阅的自定义能力类型被平台面禁用后，用户进入配置页 | 配置页正常展示（已订阅不受禁用影响），但展示提示："该能力已下架，新用户无法订阅" |
| EC-002 | 取消订阅后误操作再次订阅同一能力 | 走正常订阅流程，重新创建订阅记录（幂等处理） |
| EC-003 | `frontendEntryUrl` 对应的子应用不可用（服务器宕机/URL 错误） | 配置页展示加载失败提示，不阻塞 wecodesite 其他功能 |
| EC-004 | 子应用 `mount` 时 props 缺失 | 主应用确保必填 props 完整性，缺失时子应用展示"配置加载失败，请重试" |
| EC-005 | 同一个能力同时被多个应用订阅/取消订阅 | 按应用隔离，互不影响 |
| EC-006 | 用户同时打开多个能力的配置页 | 每次只有一个子应用处于活跃状态（切换时自动 unmount 上一个） |

## 8. 开放问题
> 待决策事项和需要进一步调研的内容

| # | 问题 | 影响范围 | 建议方案 | 状态 |
|---|------|---------|---------|:----:|
| 1 | QianKun 动态子应用注册如何实现？当前 `microApps.js` 是静态配置，配置页需要运行时动态加载 | 前端架构 | wecodesite 提供动态注册 API（如 `registerMicroApp(name, entry, container)`），或在配置页组件内直接使用 `loadMicroApp` API | ⏳ 待前端调研 |
| 2 | `autoSubscribeAfterAbility` 具体的权限映射规则是什么？一个 abilityType 对应哪些 scope/权限？ | 后端架构 | 当前阶段仅打日志+预留钩子，具体映射规则需在 CAP-OPEN-001 的权限模型基础上定义 | ⏳ 待确认 |
| 3 | `hidden` 字段是否真的需要？目前前端硬编码过滤 `type !== 6`，是否改为后端配置 | 数据模型 | 建议添加 `hidden` 字段，由平台面管理，默认 0，类型 6（应用入群通知）设为 1 | ⏳ 待决策 |

## 修订记录
> 记录本文档的版本变更历史

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — 嵌入能力开放面完整规范 | 2026-07-13 | SDDU Spec Agent |
