# specs-tree-app-list

## 📋 Feature 概览

| 字段 | 内容 |
|------|------|
| Feature ID | APP-MGMT-001 |
| Feature 名称 | 开放平台应用管理 |
| 状态 | planned |
| 优先级 | P1 |
| 创建日期 | 2026-06-01 |
| 最后更新 | 2026-06-03 |

## 📁 目录导航

| 文件 | 说明 | 行数 |
|------|------|------|
| [discovery.md](./discovery.md) | 需求挖掘报告（Discovery Report） | 106 |
| [spec.md](./spec.md) | 产品规范文档（Product Specification） | 259 |
| [plan.md](./plan.md) | 技术规划与详细设计（Technical Plan） | 3284 |
| [frontend-design.md](./frontend-design.md) | 前端设计（Frontend Design） | 1405 |
| [test-cases.md](./test-cases.md) | 测试用例（Test Cases） | 1049 |
| [tasks.md](./tasks.md) | 任务分解（Tasks） | 662 |
| [java-coding-standard.md](./java-coding-standard.md) | 后端 Java 开发规范 | 3993 |
| [state.json](./state.json) | 状态文件 | 26 |

## 🎯 Feature 简介

开放平台应用管理是企业内部开发者使用 WeLink 开放平台的核心入口。开发者通过此模块创建应用、配置能力、发布版本，实现与 WeLink 平台的功能集成。

**核心能力**：
- 应用的创建与配置（CRUD）
- 团队成员协作（Owner/Admin/Developer 三级权限）
- 应用能力扩展（群置顶、群通知等 7 种能力）
- 版本发布审核流程（草稿 → 审核中 → 已通过/已拒绝）

## 📊 阶段进度

```
[discovery] ✅ 完成 (2026-06-01)
    ↓
[spec]      ✅ 完成 (2026-06-02)
    ↓
[plan]      ✅ 完成 (2026-06-03) ← 当前阶段
    ↓
[tasks]     ⏳ 待开始
    ↓
[build]     ⏳ 待开始
    ↓
[review]    ⏳ 待开始
    ↓
[validate]  ⏳ 待开始
```

## 📦 交付物

### 1. 产品规范（spec.md）

- **15 个功能需求**（FR-001 ~ FR-015）
- **5 个功能模块**：
  1. 应用列表（FR-001 ~ FR-002）
  2. 凭证与基础信息（FR-003 ~ FR-005）
  3. 成员管理（FR-006 ~ FR-009）
  4. 添加应用能力（FR-010）
  5. 版本发布与审核（FR-011 ~ FR-015）

### 2. 技术规划（plan.md）

**11 个章节**：
1. 概述
2. 技术选型与架构
3. 模块划分与目录结构
4. 数据模型（接口定义）
5. 状态机（版本、成员、能力、应用）
6. 权限矩阵（Owner/Admin/Developer）
7. 路由与导航
8. 关键交互流程（13 个核心流程）
9. API 契约（30+ 端点）
10. Mock 数据策略
11. 风险与对策

**附录**：
- A: Demo 中 40 个 JavaScript 函数清单
- B: Demo 中 50 个 CSS 类对应表
- C: 参考资料

### 3. 前端设计（frontend-design.md）

**15 个章节**：
- § 1 前端目录设计
- § 2 总体结构（8 大场景速查 + 27 API 速查）
- § 3 路由结构
- § 4 全局通用组件
- § 5-§ 12 用户旅程 A-H（8 大页面：AppList/CreateAppModal/BasicInfo/Members/Capabilities/VersionRelease/VersionDetail）
- § 13 横切关注点（状态约束、权限、校验、编码规范）
- § 14 前端必读的源码位置
- § 15 27 个后端 API 速查

**配套规范**：
- [开发规范文档](../../../front/doc/开发规范文档.md)（**生成代码必读**）

## 🔗 关联文件

| 文件 | 路径 | 说明 |
|------|------|------|
| Demo 源代码 | `../../../../demo-app-list.html` | 4354 行高保真原型 |
| 前端项目 | `../../../../wecodesite/` | React 18 + Ant Design 4 |
| 前端开发规范 | `../../../front/doc/开发规范文档.md` | 48K，10 大章节，生成代码必读 |
| 已有页面 | `wecodesite/src/pages/{AppList,BasicInfo,Members,Capabilities,VersionRelease}/` | 5 个独立页面 + route.js + thunk.js |
| 已有公共配置 | `wecodesite/src/configs/web.config.js` / `src/utils/constants.js` / `src/utils/common.js` | 需追加 APP_* / APP_MEMBERS_* / APP_ABILITIES_* / APP_VERSIONS_* 路径常量 |
| 已有公共组件 | `wecodesite/src/components/{CreateAppModal,BindEamapModal,DeleteConfirmModal,Layout,AppCard}/` | 复用现成组件 |

## 📋 需求清单

### Must Have（已实现于 Demo）

- [x] FR-001: 应用列表
- [x] FR-002: 创建应用
- [x] FR-003: 应用凭证（只读）
- [x] FR-004: 基本信息编辑
- [x] FR-005: 认证方式
- [x] FR-006: 成员列表
- [x] FR-007: 添加成员
- [x] FR-008: 删除成员（含 Owner 保护）
- [x] FR-009: 转移 Owner
- [x] FR-010: 能力列表
- [x] FR-011: 版本列表
- [x] FR-012: 创建版本
- [x] FR-013: 版本详情
- [x] FR-014: 发布审批
- [x] FR-015: 版本撤回

### 待补充（Demo 未覆盖）

- [ ] 删除能力（FR-010 衍生）
- [ ] 删除版本（FR-013 衍生）
- [ ] 功能实际上传（FR-004 衍生）
- [ ] 能力实际配置页（FR-010 衍生）
- [ ] 后端 API 对接（全部 FR）
- [ ] 真实身份认证（SSO）

## 🎯 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 前端框架 | React | 18.2.x |
| 路由 | React Router | 6.20.x |
| UI 库 | Ant Design | 4.24.x |
| HTTP | Axios | 1.6.x |
| 构建 | Vite | 5.0.x |
| 样式 | LESS + CSS Modules | 4.2.x |

## 👥 团队角色

| 角色 | 可见应用 | 可编辑应用 | 可添加/删除成员 | 可转移 Owner | 可添加能力 | 可发布版本 |
|------|:--------:|:----------:|:--------------:|:------------:|:----------:|:----------:|
| **Owner** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Admin** | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Developer** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

## 📈 关键指标

| 指标 | 目标 | 测量方式 |
|------|------|----------|
| 功能完成度 | 100% (15/15 FR) | 验收测试 |
| 视觉还原度 | ≥ 95% | 视觉回归测试 |
| 单元测试覆盖率 | ≥ 80% | Jest Coverage |
| 首屏加载时间 | < 2s | Lighthouse |
| 路由切换时间 | < 500ms | Performance API |
| ESLint 警告 | 0 | eslint |

## 🚀 下一步

1. **运行** `@sddu tasks app-list` 开始任务分解
2. **生成** tasks.md，包含具体的实施任务
3. **进入** build 阶段，由 sddu-build 执行实施
4. **进入** review 阶段，由 sddu-review 审查代码
5. **进入** validate 阶段，由 sddu-validate 验证功能

## 📅 时间线

| 阶段 | 计划时间 | 状态 |
|------|---------|------|
| Discovery（需求挖掘） | 1 天 | ✅ 完成 |
| Spec（规范编写） | 1 天 | ✅ 完成 |
| Plan（技术规划） | 1 天 | ✅ 完成 |
| Tasks（任务分解） | 0.5 天 | ⏳ 待开始 |
| Build（实施） | 10.5 天 | ⏳ 待开始 |
| Review（审查） | 1 天 | ⏳ 待开始 |
| Validate（验证） | 1 天 | ⏳ 待开始 |
| **总计** | **16 天** | - |

## 📞 联系方式

- **项目作者**：SDDU 智能规划
- **创建日期**：2026-06-01
- **最后更新**：2026-06-03

---

## 快速开始

如果你需要立即开始实施：

1. **阅读 spec.md** 了解产品需求
2. **阅读 plan.md** 了解技术方案
3. **阅读 frontend-design.md** 了解 UI 细节
4. **阅读开发规范文档**（`front/doc/开发规范文档.md`）作为代码编写规范
5. **查看 demo-app-list.html** 了解视觉效果
6. **阅读 tasks.md** 了解任务分解
7. **运行** `@sddu build app-list` 开始编码
