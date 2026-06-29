## 3 Lookup 化优化方案 v2.0

> 方案状态：已确认待实施 | 创建日期：2026-06-29 | 核心收益：连接流发布校验 DB 查询从最多 16 次降至 2 次

### 3.1 背景与痛点

#### 3.1.1 N+1 查询问题

§2.6 的 Property 化方案存在严重的 N+1 查询问题。ConnectorPlatformPropertyService.getPerAppInt(appId, code, default) 逐项查询 openplatform_property_t，每项配置最多 2 次 DB 查询（应用 path 未命中后回退平台 path）。

| 消费场景 | 读取配置项 | 当前 DB 查询次数 |
|---------|-----------|---------------|
| 连接流发布校验（最痛） | #6 #7 #8 #9 #10 #11 #12 #13（8 项） | 最多 16 次 |
| 连接器发布校验 | #2 #3（2 项） | 最多 4 次 |
| 创建草稿 | #1 或 #4（1 项） | 1 次 |
| 运行时执行/限流/缓存 | #6 #8 #9 #10（分散） | 各最多 2 次 |

根因：openplatform_property_t 是扁平键值对，SELECT value WHERE path=? AND code=? 一次只能取一项，无法表达一组配置的语义。

#### 3.1.2 优化思路

将一起使用的配置归入同一 LookupClassify，利用 classify 1:N item 结构一次联查 classify_t + item_t 拿回整组键值对，N 次 DB 查询降为 2 次（应用组 + 平台组）。

### 3.2 数据模型

#### 3.2.1 分组策略

| classify_code | 包含配置项 | 说明 |
|---------------|-----------|------|
| Connector.Platform.Config | #1~#14 全部 14 项 | 平台默认值 |
| Connector.Platform.{appId}.Config | #1~#14 中需覆盖的项 | 应用覆盖值，如 Connector.Platform.app_001.Config |
| Connector.Platform.AppWhitelist | #15 应用白名单 | 白名单独立 classify |

设计要点：#1~#14 归为一个 classify（而非按限流/超时等拆多组），因为消费方（尤其发布校验）同时需要多类配置，一个 classify 一次拿回全部最省查询。

#### 3.2.2 命名规则

统一 PascalCase + 点号分隔：

| 层级 | 字段 | 规则 | 示例 |
|------|------|------|------|
| path（命名空间） | classify_t.path | 统一固定值 | CEC.Open |
| classify_code（分组） | classify_t.classify_code | 平台默认 / 应用覆盖 / 白名单 | Connector.Platform.Config |
| item_code（配置项） | item_t.item_code | PascalCase.点号 | Flow.Max.Qps |

#### 3.2.3 item_code 映射表

| # | 原 Property code | 新 item_code | 默认值 | 类型 | 按应用区分 |
|---|-----------------|-------------|--------|------|-----------|
| 1 | connector_max_versions | Connector.Max.Versions | 1000 | int | 否 |
| 2 | connector_url_regex_pattern | Connector.Url.Regex.Pattern | null | String | 否 |
| 3 | connector_config_max_bytes | Connector.Config.Max.Bytes | 0 | int | 是 |
| 4 | flow_max_versions | Flow.Max.Versions | 1000 | int | 否 |
| 5 | max_execution_records_per_flow | Max.Execution.Records.Per.Flow | 1000 | int | 是 |
| 6 | node_max_timeout_seconds | Node.Max.Timeout.Seconds | 5 | int | 是 |
| 7 | flow_config_max_bytes | Flow.Config.Max.Bytes | 0 | int | 是 |
| 8 | flow_max_qps | Flow.Max.Qps | 1000 | int | 是 |
| 9 | flow_max_concurrency | Flow.Max.Concurrency | 1000 | int | 是 |
| 10 | flow_max_cache_ttl_seconds | Flow.Max.Cache.Ttl.Seconds | 1296000 | int | 是 |
| 11 | flow_max_parallel_branches | Flow.Max.Parallel.Branches | 8 | int | 是 |
| 12 | script_max_length_chars | Script.Max.Length.Chars | 10000 | int | 是 |
| 13 | script_max_timeout_seconds | Script.Max.Timeout.Seconds | 30 | int | 是 |
| 14 | log_collection_enabled | Log.Collection.Enabled | true | boolean | 是 |

#15 白名单：item_code = appId，item_value = appId（如 app_001 / app_001）

说明：#1/#2/#4 不支持按应用区分，仅在 Connector.Platform.Config 下存在；#3/#5~#14 支持按应用区分，可在 Connector.Platform.{appId}.Config 下覆盖。

#### 3.2.4 数据示例

openplatform_lookup_classify_t：

| classify_code | path | classify_name |
|---------------|------|--------------|
| Connector.Platform.Config | CEC.Open | 连接器平台配置（平台默认） |
| Connector.Platform.app_001.Config | CEC.Open | 连接器平台配置（app_001 覆盖） |
| Connector.Platform.app_002.Config | CEC.Open | 连接器平台配置（app_002 覆盖） |
| Connector.Platform.AppWhitelist | CEC.Open | 连接器平台应用白名单 |

openplatform_lookup_item_t（平台默认组，14 项）：

| item_code | item_value |
|-----------|-----------|
| Connector.Max.Versions | 1000 |
| Connector.Url.Regex.Pattern | (空) |
| Connector.Config.Max.Bytes | 0 |
| Flow.Max.Versions | 1000 |
| Max.Execution.Records.Per.Flow | 1000 |
| Node.Max.Timeout.Seconds | 5 |
| Flow.Config.Max.Bytes | 0 |
| Flow.Max.Qps | 1000 |
| Flow.Max.Concurrency | 1000 |
| Flow.Max.Cache.Ttl.Seconds | 1296000 |
| Flow.Max.Parallel.Branches | 8 |
| Script.Max.Length.Chars | 10000 |
| Script.Max.Timeout.Seconds | 30 |
| Log.Collection.Enabled | true |

openplatform_lookup_item_t（app_001 覆盖组，仅覆盖项）：

| item_code | item_value |
|-----------|-----------|
| Node.Max.Timeout.Seconds | 10 |
| Script.Max.Length.Chars | 20000 |

openplatform_lookup_item_t（白名单组）：

| item_code | item_value |
|-----------|-----------|
| app_001 | app_001 |
| app_002 | app_002 |
| app_003 | app_003 |

### 3.3 查询逻辑

#### 3.3.1 批量读取（核心优化）

读取某 appId 的全部 14 项配置：
1. 查 (CEC.Open, Connector.Platform.{appId}.Config) → 应用覆盖项 Map
2. 查 (CEC.Open, Connector.Platform.Config) → 平台默认项 Map
3. 合并：平台 Map 为 base，应用 Map 覆盖之 → 最终 Map
4. 按 item_code 取值，解析为 int/boolean，缺失/异常用硬编码默认值

DB 查询次数：2 次（应用组 + 平台组），vs 优化前最多 16 次。

#### 3.3.2 白名单读取

查 (CEC.Open, Connector.Platform.AppWhitelist) → item_value 列表 → 1 次 DB。空白名单 = 全部拒绝。

#### 3.3.3 兜底约束

沿用 §2.6.1.1 原则：DB 不可用/配置缺失/格式异常时返回硬编码默认值，禁止抛异常。

| 约束 | 说明 |
|------|------|
| DB 不可用 | 跳过查询，全部使用硬编码默认值 + WARN |
| classify 缺失 | 该组返回空 Map，不影响另一组 |
| item 缺失 | 该项使用硬编码默认值 |
| value 格式异常 | 返回默认值 + ERROR 日志 |

### 3.4 缓存策略

| 服务 | 缓存 | key 规则 | TTL | 清理方 |
|------|------|---------|-----|--------|
| open-server | 否 | — | — | — |
| connector-api | Redis | OPENPLATFORM:LOOK:UP:ITEM:{path}:{classifyCode} | 7d±2h | market-server clearLookUpItemCache |

关键：connector-api 缓存 key 与 market-server 清理 key 完全一致，确保 market-server 修改配置后能精准失效 connector-api 缓存。

缓存 key 示例：
- 平台默认组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.Config
- app_001 组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.app_001.Config
- 白名单组: OPENPLATFORM:LOOK:UP:ITEM:CEC.Open:Connector.Platform.AppWhitelist

### 3.5 代码改造范围

#### 3.5.1 open-server 侧

| 文件 | 改造内容 |
|------|---------|
| LookupWhitelistMapper.java + XML | 扩展查询方法增加 path 参数；新增 selectItemMapByPathAndClassifyCode(path, classifyCode) 返回 Map |
| ConnectorPlatformPropertyService.java | 重构核心：从逐项查 Property 改为批量查 Lookup；保留原方法签名，内部从合并 Map 取值 |
| AppWhitelistService.java | classify_code 改为 Connector.Platform.AppWhitelist，path 传 CEC.Open |
| ConnectorPlatformConstants.java | 新增 path / classify_code / item_code 常量 |

#### 3.5.2 connector-api 侧

| 文件 | 改造内容 |
|------|---------|
| 新建 Lookup R2DBC Repository | 支持按 (path, classify_code) 联查 |
| ConnectorApiPropertyService.java | 扩展为支持全部 14 项批量读取 + Redis 缓存 |
| DagScheduler.java / ReactiveSequentialExecutor.java | 运行时超时上限接入 #6 |
| RateLimitConfigReader.java | 运行时 QPS/并发上限接入 #8 #9 |
| FlowCacheManager.java | 运行时缓存 TTL 上限接入 #10 |

#### 3.5.3 market-server 侧

| 文件 | 改造内容 |
|------|---------|
| 数据初始化 SQL | 写入 classify + item 默认数据（见 §3.7） |
| CacheServiceV2.java | 已支持 clearLookUpItemCache(path, classifyCode)，无需改动 |

### 3.6 效果对比

| 指标 | 优化前（Property） | 优化后（Lookup） |
|------|-------------------|-----------------|
| 连接流发布校验 DB 查询 | 最多 16 次 | 2 次 |
| 连接器发布校验 DB 查询 | 最多 4 次 | 2 次 |
| 运行时单次执行配置读取 | 多次分散 | 2 次（含缓存则 0 次） |
| 存储表 | openplatform_property_t | openplatform_lookup_classify_t + item_t |
| 按应用区分 | path 区分 | classify_code 区分 |
| 批量读取 | 不支持 | 一次拿回整组 14 项 |

### 3.7 数据库初始化

#### 3.7.1 classify 初始化

INSERT INTO openplatform_lookup_classify_t (classify_code, classify_name, path, status) VALUES
('Connector.Platform.Config', '连接器平台配置-平台默认', 'CEC.Open', 1),
('Connector.Platform.AppWhitelist', '连接器平台应用白名单', 'CEC.Open', 1);

#### 3.7.2 item 初始化（平台默认组，14 项）

classify_id 需替换为实际查询结果：

| item_code | item_name | item_value | item_index |
|-----------|-----------|-----------|-----------|
| Connector.Max.Versions | 连接器版本数量上限 | 1000 | 1 |
| Connector.Url.Regex.Pattern | 连接器URL正则规则 | (空) | 2 |
| Connector.Config.Max.Bytes | 连接器配置JSON长度上限 | 0 | 3 |
| Flow.Max.Versions | 连接流版本数量上限 | 1000 | 4 |
| Max.Execution.Records.Per.Flow | 运行记录条数上限 | 1000 | 5 |
| Node.Max.Timeout.Seconds | 连接器节点超时上限 | 5 | 6 |
| Flow.Config.Max.Bytes | 连接流配置JSON长度上限 | 0 | 7 |
| Flow.Max.Qps | 连接流最大QPS | 1000 | 8 |
| Flow.Max.Concurrency | 连接流最大并发 | 1000 | 9 |
| Flow.Max.Cache.Ttl.Seconds | 连接流缓存TTL上限 | 1296000 | 10 |
| Flow.Max.Parallel.Branches | 连接流并行节点分支上限 | 8 | 11 |
| Script.Max.Length.Chars | 脚本源码长度上限 | 10000 | 12 |
| Script.Max.Timeout.Seconds | 脚本超时范围 | 30 | 13 |
| Log.Collection.Enabled | 日志采集开关 | true | 14 |

### 3.8 实施优先级

| 优先级 | 项目 | 原因 |
|--------|------|------|
| P0 | LookupWhitelistMapper 扩展（open-server） | 新增 path 参数 + selectItemMapByPathAndClassifyCode，所有后续改造的基础 |
| P0 | ConnectorPlatformPropertyService 重构（open-server） | 核心重构：逐项查 Property → 批量查 Lookup |
| P1 | ConnectorApiPropertyService 扩展（connector-api） | 运行时批量读取 + Redis 缓存 |
| P1 | 数据初始化 SQL（market-server） | 写入 classify + item 默认数据 |
| P1 | AppWhitelistService 迁移 | classify_code 改为新命名规则 |
| P2 | 运行时消费方接入 | DagScheduler / RateLimitConfigReader / FlowCacheManager |
| P2 | Property 数据迁移脚本 | openplatform_property_t → openplatform_lookup_*_t |
| P3 | openplatform_property_t 清理 | 确认迁移完成后，清理连接器平台相关 Property 数据 |
