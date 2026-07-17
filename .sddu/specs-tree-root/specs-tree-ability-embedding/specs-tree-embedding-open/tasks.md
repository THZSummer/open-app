# 任务分解：嵌入能力开放面

> **文档定位**: SDDU 任务清单 - 将技术方案分解为可并行执行的原子任务，作为 build 阶段的输入  
> **前置依赖**: plan.md（技术方案）、spec.md（需求规范）  
> **创建人**: SDDU Tasks Agent  
> **创建时间**: 2026-07-13  
> **版本**: v1.1  
> **更新人**: SDDU Plan Agent  
> **更新时间**: 2026-07-17  
> **更新说明**: 同步平台面字段变更：frontendEntryUrl → entryUrl/routePath/aliasName/requireRelease/loadType；所有接口返回字段加 loadType；TASK-005 配置页嵌入增加 loadType 分支

## 1. 依赖拓扑总览
> 任务依赖关系和执行顺序

```
Wave 1 ─── (无依赖，全部并行)
  TASK-001 [S]  VO 扩展（entryUrl/routePath/aliasName/requireRelease/loadType 字段）

Wave 2 ─── (依赖 Wave 1 + 平台面 TASK-001 DB 迁移)
  TASK-002 [M]  AbilityServiceImpl 列表/已订阅增强
  TASK-003 [M]  AbilityServiceImpl 订阅/自动桥接增强

Wave 3 ─── (依赖 Wave 2，前端）
  TASK-004 [M]  前端 Capabilities 列表渲染改造
  TASK-005 [L]  前端配置页嵌入子应用
  TASK-006 [S]  constants 场景分组扩展
```

> ⚠️ **跨 Feature 依赖**：本面后端任务（TASK-002/003）依赖平台面 `specs-tree-embedding-platform` 的 TASK-002（DB 迁移新增 `hidden`、`entry_url`、`route_path`、`alias_name`、`require_release`、`load_type` 字段）。执行前需确认平台面 TASK-002 已完成。

## 2. 任务列表
> 每个任务的详细定义

### TASK-001: VO 扩展新增 entryUrl/routePath/aliasName/requireRelease/loadType 字段
> 开放面响应结构扩展（同步平台面字段变更）

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | 无 |
| **执行波次** | 1 |
| **对应 FR** | FR-001、FR-004 |

**描述**: 在 `AbilityVO` 和 `AppAbilityDetailVO` 中新增 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType` 字段（可选，无值返回 null），移除已废弃的 `frontendEntryUrl`，保持向后兼容。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/vo/AbilityVO.java` |
| MODIFY | `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/vo/AppAbilityDetailVO.java` |

**验收标准**:
- [ ] `AbilityVO` 移除 `frontendEntryUrl`，新增 `entryUrl`(String)、`routePath`(String)、`aliasName`(String)、`requireRelease`(Integer)、`loadType`(Integer) 字段及 getter/setter
- [ ] `AppAbilityDetailVO` 移除 `frontendEntryUrl`，新增以上 5 个字段
- [ ] 所有字段为可选，序列化时无值返回 null，不破坏现有前端
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-002: AbilityServiceImpl 列表/已订阅查询增强
> 能力列表与已订阅列表逻辑改造

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-001、平台面 TASK-002 |
| **执行波次** | 2 |
| **对应 FR** | FR-001、FR-004、FR-005 |

**描述**: 修改 `AbilityServiceImpl` 的 `getAbilityList()` 和 `getSubscribedAbilities()` 方法：移除 type=6 硬编码排除逻辑，改为按 `hidden` 字段过滤（列表查询）；新增返回 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType`。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/service/impl/AbilityServiceImpl.java` |

**验收标准**:
- [ ] `getAbilityList()`：移除 `abilityType != 6` 硬编码，改为按 `hidden=0` 过滤
- [ ] `getAbilityList()`：返回结果含 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType`
- [ ] `getSubscribedAbilities()`：移除 type=6 硬编码排除
- [ ] `getSubscribedAbilities()`：返回结果含 `entryUrl`/`routePath`/`aliasName`/`requireRelease`/`loadType`
- [ ] 自定义类型（≥100）正常出现在列表中
- [ ] 已订阅标记逻辑不变
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

### TASK-003: AbilityServiceImpl 订阅校验与自动桥接增强
> 订阅逻辑改造

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-001、平台面 TASK-001 |
| **执行波次** | 2 |
| **对应 FR** | FR-002、FR-003 |

**描述**: 修改 `AbilityServiceImpl` 的 `addAbility()`（订阅）和 `autoSubscribeAfterAbility()`（自动桥接）方法：订阅校验从枚举校验改为 DB 校验；自动桥接扩展点由空实现改为打日志。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/service/impl/AbilityServiceImpl.java` |

**验收标准**:
- [ ] `addAbility()`：移除 `AbilityTypeEnum.isValidCode()` 枚举校验
- [ ] `addAbility()`：改为查询 DB 校验能力存在且 status=1
- [ ] `addAbility()`：重复订阅检查、关联插入逻辑不变
- [ ] `autoSubscribeAfterAbility()`：由空实现改为输出日志（记录 appId、abilityType）
- [ ] 自定义类型（≥100）可通过校验正常订阅
- [ ] Maven 编译通过

**验证命令**:
```bash
mvn -f open-server/pom.xml compile -q
```

> ⚠️ TASK-002 与 TASK-003 均修改 `AbilityServiceImpl.java`，需串行执行避免冲突。

### TASK-004: 前端 Capabilities 列表渲染改造
> 动态能力目录

| 属性 | 值 |
|------|-----|
| **复杂度** | M |
| **前置依赖** | TASK-002 |
| **执行波次** | 3 |
| **对应 FR** | FR-101 |

**描述**: 修改 `Capabilities.jsx` 列表渲染逻辑：移除前端 type=6 硬编码过滤；自定义类型直接使用后端返回的 nameCn/descCn/iconUrl 渲染；预设类型可回退到 `ABILITY_TYPE_MAP` 常量作为兜底。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `wecodesite/src/pages/Capabilities/Capabilities.jsx` |

**验收标准**:
- [ ] 移除前端 `abilityType !== 6` 硬编码过滤
- [ ] 自定义类型（≥100）使用后端返回的 nameCn/descCn/iconUrl 渲染
- [ ] 预设类型（1-7）可回退 `ABILITY_TYPE_MAP` 常量获取中文名/图标
- [ ] 卡片按钮逻辑不变（未订阅->添加，已订阅->配置）
- [ ] 前端构建通过

**验证命令**:
```bash
cd wecodesite && npm run build
```

### TASK-005: 前端配置页嵌入子应用
> 配置页根据 loadType 分支加载

| 属性 | 值 |
|------|-----|
| **复杂度** | L |
| **前置依赖** | TASK-002 |
| **执行波次** | 3 |
| **对应 FR** | FR-102 |

**描述**: 在配置页视图根据 `loadType` 分支处理：
- loadType=1（路由加载）：根据 `routePath` 做内部路由跳转，不加载子应用
- loadType=2（微前端加载）：通过 QianKun `loadMicroApp` API 运行时动态加载子应用（入口来自 `entryUrl`），传递上下文 {appId, abilityType, nameCn}；无 entryUrl 时展示占位文本；切换/退出时卸载子应用。

同时新增 `EmbeddedSubApp.jsx` 组件封装微前端加载逻辑（loadType=2 场景）。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| NEW | `wecodesite/src/pages/Capabilities/EmbeddedSubApp.jsx` |
| MODIFY | `wecodesite/src/pages/Capabilities/Capabilities.jsx` |

**验收标准**:
- [ ] `Capabilities.jsx` 配置视图根据 `loadType` 分支：1=路由跳转，2=微前端加载
- [ ] loadType=1 时根据 `routePath` 做内部路由跳转
- [ ] loadType=2 时新增 `EmbeddedSubApp.jsx` 组件，使用 `loadMicroApp` 动态加载子应用
- [ ] 从 `entryUrl` 获取子应用入口地址（非 `frontendEntryUrl`）
- [ ] 向子应用传递上下文：appId、abilityType、nameCn
- [ ] 无 `entryUrl` 的能力展示占位文本"配置页面由能力方提供"
- [ ] 切换能力或退出配置时自动卸载子应用（unload）
- [ ] `microApps.js` 静态注册列表不变
- [ ] 前端构建通过

**验证命令**:
```bash
cd wecodesite && npm run build
```

### TASK-006: constants 场景分组扩展
> 自定义类型场景分组兜底

| 属性 | 值 |
|------|-----|
| **复杂度** | S |
| **前置依赖** | TASK-004 |
| **执行波次** | 3 |
| **对应 FR** | FR-103 |

**描述**: 在 `constants.js` 的 `ABILITY_SCENE_MAP` 中新增"其他"默认场景，自定义类型（≥100）归入该场景；保留现有预设类型场景分组作为兜底。

**涉及文件**:

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `wecodesite/src/utils/constants.js` |

**验收标准**:
- [ ] `ABILITY_SCENE_MAP` 新增"其他"场景分组
- [ ] 自定义类型（≥100）默认归入"其他"场景
- [ ] 预设类型场景分组保持不变
- [ ] 前端构建通过

**验证命令**:
```bash
cd wecodesite && npm run build
```

## 3. 任务汇总
> 任务数量、复杂度和波次的统计总览

| 统计项 | 数值 |
|--------|:--:|
| 总任务数 | 6 |
| S 级 (简单) | 2 |
| M 级 (中等) | 3 |
| L 级 (复杂) | 1 |
| 执行波次 | 3 |

## 4. 执行策略
> 各波次的执行说明

| 波次 | 任务 | 策略 |
|:--:|------|------|
| 1 | TASK-001 | 并行执行（数据结构扩展） |
| 2 | TASK-002, TASK-003 | **串行执行**（均修改 AbilityServiceImpl，避免冲突） |
| 3 | TASK-004, TASK-005, TASK-006 | TASK-004 与 TASK-005 **串行执行**（均改 Capabilities.jsx）；TASK-006 可并行 |

## 修订记录
> 记录本文档的版本变更历史

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 - 开放面任务分解（6 任务 / 3 波次） | 2026-07-13 | SDDU Tasks Agent |
| v1.1 | 同步平台面字段变更：TASK-001 重写（frontendEntryUrl → entryUrl/routePath/aliasName/requireRelease/loadType）；TASK-002 返回字段增加 loadType；TASK-005 配置页嵌入增加 loadType 分支 | 2026-07-17 | SDDU Plan Agent |
