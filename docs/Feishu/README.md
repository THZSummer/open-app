# 飞书开放平台业务架构文档

> 项目版本：v1.0  
> 创建日期：2026-03-19  
> 最后更新：2026-03-19

## 📁 项目概述

本项目包含飞书开放平台的完整业务架构文档、UML 图集和可视化图表。

## 📂 文件结构

```
F:\Product\Feishu\
├── 📄 README.md                          # 本文件
│
├── 📊 业务架构图 (DrawIO + PNG)
│   ├── feishu-architecture.drawio        # v1 初版源文件 (4.6KB)
│   ├── feishu-architecture.png           # v1 导出图 (68KB)
│   ├── feishu-architecture-v2.drawio     # v2 版源文件 (8.5KB)
│   ├── feishu-architecture-v2.png        # v2 导出图 (121KB)
│   ├── feishu-architecture-v3.drawio     # v3 优化版源文件 (12KB) ⭐
│   ├── feishu-architecture-v3.png        # v3 导出图 (147KB) ⭐
│   ├── feishu-architecture-final.drawio  # 最终版源文件 (12KB) ⭐
│   └── feishu-architecture-final.png     # 最终版导出图 (148KB) ⭐
│
├── 📐 UML 图集
│   ├── 飞书业务架构-UML 图集.puml            # PlantUML 源文件 (17KB)
│   ├── 飞书业务架构 - 组件图.png             # 组件图渲染 (166KB)
│   ├── 飞书业务架构 - 活动图-token 流程.png   # 活动图渲染 (2KB)
│   └── 飞书业务架构 - 序列图.png             # 序列图渲染 (82KB)
│
└── 📖 文档
    └── 飞书权限架构设计.md                 # 完整架构设计文档 (28KB) ⭐
```

## 🎯 核心内容

### 1. 业务架构图 (5 层架构)

```
┌─────────────────────────────────────────────────────────────┐
│  用户层：普通用户 │ 管理员 │ 开发者                          │
├─────────────────────────────────────────────────────────────┤
│  应用层：自建应用 (Custom App) │ 商店应用 (Store App)        │
├──────────────────┬──────────────────────────────────────────┤
│  认证授权层       │           事件层                          │
│  • app_token     │   • 用户事件 (contact.*)                 │
│  • tenant_token  │   • 消息事件 (im.*)                      │
│  • user_token    │   • 应用事件 (app.*)                     │
│  (OAuth 2.0)     │                                          │
├─────────────────────────────────────────────────────────────┤
│  API 调用层：通讯录 │ 消息 │ 云文档 │ 日历                    │
├─────────────────────────────────────────────────────────────┤
│  安全机制：IP 白名单 │ 凭证加密 │ 事件验签 │ 权限审查          │
└─────────────────────────────────────────────────────────────┘
```

**推荐查看：**
- `feishu-architecture-final.png` - 最终完整版
- `feishu-architecture-v3.png` - v3 优化版

### 2. UML 图集 (10 个图)

| 图类型 | 文件 | 说明 |
|--------|------|------|
| 组件图 | `飞书业务架构 - 组件图.png` | 6 层架构组件关系 |
| 活动图 | `飞书业务架构 - 活动图-token 流程.png` | app_access_token 获取流程 |
| 活动图 | (UML 图集.puml) | OAuth2 用户授权流程 |
| 序列图 | (UML 图集.puml) | API 调用完整流程 |
| 序列图 | (UML 图集.puml) | 事件订阅推送流程 |
| 类图 | (UML 图集.puml) | Token 管理类设计 |
| 状态图 | (UML 图集.puml) | Token 生命周期 |
| 部署图 | (UML 图集.puml) | 系统架构部署 |
| 包图 | (UML 图集.puml) | 模块依赖关系 |
| 对象图 | (UML 图集.puml) | 运行时实例 |

**PlantUML 源文件：** `飞书业务架构-UML 图集.puml`

### 3. 架构设计文档

**文件：** `飞书权限架构设计.md`

**内容目录：**
1. 认证授权层 (Authentication Layer)
   - app_access_token
   - tenant_access_token
   - user_access_token (OAuth 2.0)
2. 应用层 (Application Layer)
   - 自建应用 vs 商店应用
   - API 权限 (Scope)
   - 应用数据权限
3. API 调用层 (API Layer)
   - 通讯录 API
   - 消息 API
   - 云文档 API
   - 日历 API
4. 事件层 (Event Subscription)
   - 用户事件
   - 消息事件
   - 应用事件
5. 安全机制
   - IP 白名单
   - 凭证加密
   - 事件验签
   - 权限审查

## 🛠️ 工具与技能

### drawio-converter 技能

本项目使用自研的 `drawio-converter` 技能进行图表转换。

**安装位置：**
```
C:\Users\Summe\.openclaw\workspace\drawio-converter.skill
```

**使用方法：**
```bash
# 单次转换
python skills\drawio-converter\scripts\drawio_convert.py input.drawio output.png

# 批量转换
python skills\drawio-converter\scripts\drawio_convert.py \
  --input-folder F:\Product\Feishu \
  --output-folder F:\Product\Feishu\exports \
  --format png \
  --scale 2 \
  --transparent
```

**支持格式：** PNG, PDF, SVG, JPG, VSDX, HTML, XML

### drawio-flowchart 技能

用于创建流程图的技能。

**安装位置：**
```
C:\Users\Summe\.openclaw\workspace\drawio-flowchart.skill
```

**脚本：**
- `create_flowchart.py` - 通用流程图
- `create_feishu_architecture_v3.py` - 飞书架构图生成

## 📊 版本历史

| 版本 | 文件 | 特点 | 日期 |
|------|------|------|------|
| v1 | `feishu-architecture.*` | 初版，基础架构 | 2026-03-19 17:17 |
| v2 | `feishu-architecture-v2.*` | 增加细节 | 2026-03-19 17:24 |
| v3 | `feishu-architecture-v3.*` | 优化层级关系 | 2026-03-19 17:33 |
| Final | `feishu-architecture-final.*` | 最终完整版 | 2026-03-19 17:40 |

## 🔗 参考资源

### 官方文档
- [飞书开放平台](https://open.feishu.cn/)
- [获取访问凭证](https://open.feishu.cn/document/ukTMukTMukTM/uMTNz4yM1MjLzUzM)
- [事件订阅指南](https://open.feishu.cn/document/server-docs/event-subscription-guide/overview)
- [API 权限配置](https://open.feishu.cn/document/ukTMukTMukTM/uQjN3QjL0YzN04CN2cDN)

### 工具
- [draw.io](https://draw.io/) - 图表绘制工具
- [PlantUML](https://plantuml.com/) - UML 图生成
- [cli-anything-drawio](https://pypi.org/project/cli-anything-drawio/) - draw.io CLI 工具

## 📝 使用建议

### 查看架构图
1. **快速预览**: 打开 PNG 文件
2. **编辑修改**: 用 draw.io 打开 .drawio 源文件
3. **查看 UML**: 访问 https://www.plantuml.com/plantuml/ 粘贴 .puml 代码

### 学习路径
1. 阅读 `飞书权限架构设计.md` 了解整体架构
2. 查看 `feishu-architecture-final.png` 建立视觉认知
3. 研究 UML 图集了解详细流程
4. 参考官方文档深入理解

### 开发参考
- **认证流程**: 参考活动图和序列图
- **API 调用**: 参考 API 调用层文档
- **事件处理**: 参考事件层和序列图

## 📦 代码仓库提交

**Commit:** `16c56b3`  
**Tag:** `v1.0.0-feishu-arch`  
**位置:** `C:\Users\Summe\.openclaw\workspace`

```bash
git show v1.0.0-feishu-arch
```

## 📞 联系与支持

如有问题或需要更新文档，请联系项目维护者。

---

**文档版本：** v1.0  
**创建日期：** 2026-03-19  
**维护者：** Summe
