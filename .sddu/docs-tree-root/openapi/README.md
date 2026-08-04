# open-app OpenAPI 规范索引

> **定位**: open-app 全量接口的 OpenAPI 3.0 契约文件（静态分析生成，未运行服务）
> **生成方式**: 静态分析各服务 `**/controller/*.java` 注解 + DTO 字段
> **对应全景**: [api.md](../api.md)（业务视角接口清单）、[docs-overview.md](../docs-overview.md)（项目全景入口）
> **生成时间**: 2026-08-03

## 文件清单

> 📖 每个服务提供两种视图：**md 包装视图**（GitHub 可渲染，内嵌完整 yaml 代码块 + 端点概览）+ **yaml 契约**（机器可读，供 openapi-generator/redoc/Swagger UI 使用）。

| 服务 | md 包装视图（GitHub 渲染） | yaml 契约（机器可读） | 端点数 | Schemas | 本地地址 |
|------|---------------------------|----------------------|:----:|:----:|---------|
| open-server 能力开放平台管理面 | [openapi-open-server.md](openapi-open-server.md) | [openapi-open-server.yaml](openapi-open-server.yaml) | 135 | 82 | http://localhost:18080/open-server |
| market-server 市场管理面 | [openapi-market-server.md](openapi-market-server.md) | [openapi-market-server.yaml](openapi-market-server.yaml) | 28 | 20 | http://localhost:18083/market-server |
| api-server 消费网关 | [openapi-api-server.md](openapi-api-server.md) | [openapi-api-server.yaml](openapi-api-server.yaml) | 11 | 11 | http://localhost:18081/api-server |
| event-server 事件/回调网关 | [openapi-event-server.md](openapi-event-server.md) | [openapi-event-server.yaml](openapi-event-server.yaml) | 10 | 5 | http://localhost:18082/event-server |
| connector-api 连接流执行引擎 | [openapi-connector-api.md](openapi-connector-api.md) | [openapi-connector-api.yaml](openapi-connector-api.yaml) | 2 | 2 | http://localhost:18180/connector-api |
| **合计** | | | **186** | **144** | |

## 与 api.md 的映射

> api.md 按**业务能力**组织（消费网关/平台基础能力/公共连接能力），本目录按**服务**组织。反向映射见各 yaml 头部注释"对应 api.md"。

| api.md 章节（业务视角） | 对应规范文件（服务视角） |
|------------------------|-------------------------|
| §2.1 API 消费网关 | [openapi-api-server.yaml](openapi-api-server.yaml) |
| §2.2 事件/回调网关 | [openapi-event-server.yaml](openapi-event-server.yaml) |
| §2.3 连接流执行 | [openapi-connector-api.yaml](openapi-connector-api.yaml) |
| §3.1 应用管理 | [openapi-open-server.yaml](openapi-open-server.yaml) + [openapi-market-server.yaml](openapi-market-server.yaml)（审批） |
| §3.2 资源分类 | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §3.3 嵌入能力 | [openapi-open-server.yaml](openapi-open-server.yaml) + [openapi-market-server.yaml](openapi-market-server.yaml) |
| §3.4 审批管理 | [openapi-open-server.yaml](openapi-open-server.yaml) + [openapi-market-server.yaml](openapi-market-server.yaml) |
| §3.5 基础数据 | [openapi-market-server.yaml](openapi-market-server.yaml) + [openapi-open-server.yaml](openapi-open-server.yaml) |
| §4.1 API 开放（R1） | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §4.2 事件开放（R2） | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §4.3 回调开放（R3） | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §5 连接器开放（R4 管理面） | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §6 数据开放/授权 | [openapi-api-server.yaml](openapi-api-server.yaml) |
| §7 订阅同步 | [openapi-open-server.yaml](openapi-open-server.yaml) |
| §8 健康检查 | 各服务 yaml 均含 |

## 认证方式

| 认证 | 服务 | 说明 |
|------|------|------|
| **internalAuth**（bearer） | open-server / market-server | 管理面内部凭证 + 成员权限 |
| **akskAuth**（X-AKSK-TOKEN） | api-server / event-server / connector-api | 消费面 AKSK，另支持 X-SOA-TOKEN / X-APIG-APPID |

## 使用方式

```bash
# 校验
npx @openapitools/openapi-generator-cli validate -i openapi-open-server.yaml

# 生成客户端 SDK
npx @openapitools/openapi-generator-cli generate -i openapi-open-server.yaml -g java -o ./sdk

# 生成文档
npx @openapitools/openapi-generator-cli generate -i openapi-open-server.yaml -g html2 -o ./docs
```

## 已知限制（静态分析）

1. **Schema 字段类型为 Java 类型名**：DTO 字段的复杂类型引用仅按类名 `$ref`，未展开嵌套泛型（如 `List<InnerVO>` 会生成 `array` 引用 InnerVO，InnerVO 定义在 schemas 中）
2. **未定位的 DTO**：部分响应类型（如 `ApiResponse<?>`）在代码中未找到类定义时，schema 标注"见实现"
3. **请求/响应示例**：未生成 `example` 字段（需要人工补充或运行时导出）
4. **与运行时一致性**：静态分析基于注解，若注解与实现有偏差（如 `@RequestParam` 泛型误解析）已尽力过滤，精确契约建议后续运行时导出 `/api-docs` 比对

---

## 修订记录

| 版本 | 变更说明 | 日期 | 修订人 |
|------|---------|------|--------|
| v1.0 | 静态分析生成 5 个服务 OpenAPI YAML（186 端点） | 2026-08-03 | SDDU Docs Agent |
