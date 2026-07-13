# 规范文档：狭义嵌入能力

**Feature ID**: EMBED-001  
**名称**: 狭义嵌入能力（Ability Embedding）  
**状态**: specifying  
**优先级**: P1  
**作者**: SDDU Spec Agent  
**创建日期**: 2026-07-13  
**依赖**: 能力开放平台 CAP-OPEN-001（独立但关联）

---

## 1. 概述

### 1.1 问题陈述

XX 通讯平台内部业务模块（IM、云盘、邮件等）拥有丰富的特有连接能力（群置顶、群通知、链接增强等），但缺乏标准化的嵌入机制，导致：

- 能力由平台预置硬编码，业务模块无法自行注册新的能力
- 前端嵌入无规范，每个能力方自建 UI，体验割裂
- 后端校验重复建设，每个能力方各自对接应用/成员系统
- 能力嵌入后无法统一治理

### 1.2 解决方案总览

构建三层架构的**狭义嵌入能力**体系：

| 层面 | 服务 | 核心职责 |
|------|------|---------|
| **① 平台面** | market-server + market-web | ability 类型 CRUD，能力目录管理后台 |
| **② 开放面** | open-server + wecodesite | ability 查询/订阅、QianKun 微前端嵌入 |
| **③ API面** | api-server | 应用认证、成员查询、权限校验接口 |

### 1.3 Goals

| # | 目标 | 归属层面 |
|---|------|---------|
| G1 | 支持平台管理员在管理后台创建/编辑/排序/删除 ability 类型 | 平台面 |
| G2 | 支持能力方注册新的 ability 类型（名称/描述/图标/前端入口URL） | 平台面 |
| G3 | 三方应用可查询能力目录并订阅 ability | 开放面 |
| G4 | 嵌入能力方通过 QianKun 微前端将 UI 嵌入 wecodesite | 开放面 |
| G5 | 嵌入能力方可调用 api-server 接口查询用户在应用中的角色 | API面 |

### 1.4 Non-Goals

| # | 非目标 | 原因 |
|---|--------|------|
| NG1 | ability 使用统计与分析 | MVP 后置，See EMBED-001 潜在问题 Q-004 |
| NG2 | ability 生命周期管理（下架通知、版本更新） | MVP 后置，See 潜在问题 Q-009 |
| NG3 | ability 与 API/Event 权限模型的自动映射 | 通过 `autoSubscribeAfterAbility` 扩展点预留，后续迭代补齐 |

---

## 2. 子Feature定义

本规范按三层架构拆分为 3 个子 Feature，各自独立进入 plan → tasks → build 阶段。

### 2.1 嵌入能力平台面（`specs-tree-embedding-platform`）

**服务**: market-server / market-web  
**核心职责**: ability 类型 CRUD 管理后台  
**关键需求**:
- ability 类型的创建/编辑/删除/排序
- 注册信息：中文名、英文名、中文描述、英文描述、图标、示意图、前端入口URL、排序
- 平台管理员管理权限，能力方只读查看
- 能力目录列表展示与检索

### 2.2 嵌入能力开放面（`specs-tree-embedding-open`）

**服务**: open-server / wecodesite  
**核心职责**: ability 查询订阅 + QianKun 微前端嵌入  
**关键需求**:
- 能力目录查询（现有的 GET /list 增强）
- 能力订阅（现有的 POST / 能力扩展）
- 已订阅能力列表（现有的 GET /subscribed 增强）
- `autoSubscribeAfterAbility` 扩展点实现
- wecodesite 提供统一的子应用注册规范，能力方按 QianKun 规范嵌入

### 2.3 嵌入能力API面（`specs-tree-embedding-api`）

**服务**: api-server  
**核心职责**: 为嵌入能力方提供标准化的服务端校验接口  
**关键需求**:
- 用户角色查询（输入应用标识（平台应用ID 或 hisAppId）+ 用户账号 → 角色列表）
- 接口安全：仅允许内部服务调用（内部凭证机制）

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 初始创建 — 拆分 3 个子 Feature | 2026-07-13 | SDDU Spec Agent |
