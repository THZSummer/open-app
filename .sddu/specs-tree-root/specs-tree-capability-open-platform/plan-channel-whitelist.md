# 补丁规划：事件/回调通道地址白名单控制

**关联 ADR**: [ADR-004](./ADR-004.md)（PROPOSED）
**关联 Feature**: CAP-OPEN-001（能力开放平台，validated）
**对齐设计**: 连接器 V3 FR-015 URL 正则白名单
**创建日期**: 2026-06-29
**作者**: Summer
**工作量估算**: 2 人天

---

## 1. 变更范围

### 1.1 变更矩阵

| 维度 | 变更内容 | 影响文件 |
|------|---------|---------|
| **新增组件** | `ChannelAddressWhitelistValidator` | open-server 新建 |
| **集成点 1** | FR-019 事件消费配置写入前校验 | `EventSubscriptionService` |
| **集成点 2** | FR-022 回调消费配置写入前校验 | `CallbackSubscriptionService` |
| ~~增强~~ | ~~字典写入时正则语法校验~~ | ~~market-server 已实现，无需改动~~ |
| **存储** | `openplatform_property_t` 新增数据约定 | 零 DDL，仅数据写入 |

### 1.2 不变更

| 维度 | 说明 |
|------|------|
| 运行时网关 | FR-029 / FR-030 不在本期范围 |
| 数据库表结构 | 复用现有 `openplatform_property_t`，无 DDL |
| 前端页面 | 复用数据字典管理现有页面，无新增页面 |
| 原 spec 状态 | CAP-OPEN-001 保持 validated，不修改 spec.md |

---

## 2. 数据模型

### 2.1 存储约定

复用 `openplatform_property_t`（与数据字典共用表），通过 `path` + `code` 前缀区分：

| 字段 | 事件白名单 | 回调白名单 |
|------|-----------|-----------|
| `path` | `channel_address_whitelist` | `channel_address_whitelist` |
| `code` | `event_url_regex_{seq}` | `callback_url_regex_{seq}` |
| `value` | 正则表达式 | 正则表达式 |
| `status` | 1=有效 / 0=失效 | 1=有效 / 0=失效 |

### 2.2 数据示例

```sql
-- 回调通道地址白名单
INSERT INTO openplatform_property_t (path, code, name, value, status) VALUES
('channel_address_whitelist', 'callback_url_regex_001', '回调白名单-企业域名',
 '^https://.*\\.corp\\.example\\.com/.*$', 1),
('channel_address_whitelist', 'callback_url_regex_002', '回调白名单-合作伙伴',
 '^https://webhook\\.partner\\.com/.*$', 1);

-- 事件通道地址白名单
INSERT INTO openplatform_property_t (path, code, name, value, status) VALUES
('channel_address_whitelist', 'event_url_regex_001', '事件白名单-企业域名',
 '^https://.*\\.corp\\.example\\.com/.*$', 1);
```

### 2.3 读取方式

```java
// 按 path + code 前缀查询，返回有效规则的 value 列表
List<String> patterns = propertyMapper.selectValuesByPathAndCodePrefix(
    "channel_address_whitelist", "callback_url_regex");
// → ["^https://.*\\.corp\\.example\\.com/.*$", "^https://webhook\\.partner\\.com/.*$"]
```

---

## 3. 核心组件设计

### 3.1 ChannelAddressWhitelistValidator

```java
/**
 * 通道地址白名单校验器
 * 
 * 职责：在消费方配置事件/回调通道地址时，校验地址是否命中平台管理员维护的正则白名单。
 * 
 * 设计要点：
 * - 写入时校验（非运行时），频次低，不做缓存
 * - 空白名单 = 不限制（兼容初期上线）
 * - 使用 Pattern.matches() 全串匹配（非 find）
 */
@Component
public class ChannelAddressWhitelistValidator {

    @Autowired
    private OpenplatformPropertyMapper propertyMapper;

    /**
     * 校验通道地址是否在白名单内
     *
     * @param channelAddress 消费方配置的通道地址
     * @param codePrefix     规则前缀："callback_url_regex" 或 "event_url_regex"
     * @throws ChannelAddressNotAllowedException 地址不在白名单内
     */
    public void validate(String channelAddress, String codePrefix) {
        // 1. 查询白名单规则
        List<String> patterns = propertyMapper.selectValuesByPathAndCodePrefix(
            "channel_address_whitelist", codePrefix);

        // 2. 空白名单 = 不限制
        if (patterns == null || patterns.isEmpty()) {
            return;
        }

        // 3. 逐条匹配，命中任一即放行
        boolean matched = patterns.stream()
            .anyMatch(regex -> Pattern.compile(regex).matcher(channelAddress).matches());

        // 4. 全部不命中 → 拒绝
        if (!matched) {
            throw new ChannelAddressNotAllowedException(channelAddress, codePrefix);
        }
    }
}
```

### 3.2 异常定义

```java
public class ChannelAddressNotAllowedException extends BusinessException {
    public ChannelAddressNotAllowedException(String channelAddress, String codePrefix) {
        super("CHANNEL_ADDRESS_NOT_ALLOWED",
              "通道地址不在允许范围内: " + channelAddress);
    }
}
```

### 3.3 集成点

#### 3.3.1 FR-019 事件消费配置

```java
@Service
public class EventSubscriptionService {

    @Autowired
    private ChannelAddressWhitelistValidator whitelistValidator;

    public void configureChannel(EventChannelConfigRequest request) {
        // ... 原有逻辑（权限状态校验、通道类型校验等）

        // 新增：白名单校验
        if (ChannelTypeEnum.WEBHOOK.getCode().equals(request.getChannelType())) {
            whitelistValidator.validate(request.getChannelAddress(), "event_url_regex");
        }

        // ... 保存逻辑
    }
}
```

#### 3.3.2 FR-022 回调消费配置

```java
@Service
public class CallbackSubscriptionService {

    @Autowired
    private ChannelAddressWhitelistValidator whitelistValidator;

    public void configureChannel(CallbackChannelConfigRequest request) {
        // ... 原有逻辑

        // 新增：白名单校验
        if (ChannelTypeEnum.WEBHOOK.getCode().equals(request.getChannelType())) {
            whitelistValidator.validate(request.getChannelAddress(), "callback_url_regex");
        }

        // ... 保存逻辑
    }
}
```

> 💡 仅 WebHook 通道类型需要校验地址；企业内部消息平台等通道类型无需校验（地址由平台内部管控）。

### 3.4 字典写入正则校验

> market-server 数据字典管理已实现正则语法校验能力，无需额外改动。平台管理员通过字典管理维护白名单规则时，非法正则会被自动拦截。

---

## 4. 上线策略

| 阶段 | 行为 | 说明 |
|------|------|------|
| **Phase 1** | 部署校验逻辑，白名单为空 | 全部放行，不影响存量 |
| **Phase 2** | 平台管理员录入已知合法地址正则 | 观察期，校验逻辑生效但合法地址已覆盖 |
| **Phase 3** | 确认规则完整性 | 验证无合法地址被误拦 |

> 由于空白名单 = 不限制，上线过程对业务零影响。平台管理员按节奏录入规则即可。

---

## 5. 任务分解

| # | 任务 | 模块 | 估时 | 前置 |
|---|------|------|:---:|:---:|
| T-1 | `ChannelAddressWhitelistValidator` 实现 + 单元测试 | open-server | 0.5d | — |
| T-2 | FR-019 事件消费配置集成白名单校验 + 单元测试 | open-server | 0.5d | T-1 |
| T-3 | FR-022 回调消费配置集成白名单校验 + 单元测试 | open-server | 0.5d | T-1 |
| T-4 | 集成测试（端到端：配置白名单 → 配置通道地址 → 校验拦截/放行） | — | 0.5d | T-2, T-3 |
| | **合计** | | **2d** | |

### 5.1 任务依赖图

```
T-1 (校验器) ──┬──▶ T-2 (事件集成)──┐
               │                     ├──▶ T-4 (集成测试)
               └──▶ T-3 (回调集成)──┘
```

### 5.2 验收标准

| # | 场景 | 预期 |
|---|------|------|
| AC-1 | 白名单为空，配置任意通道地址 | ✅ 放行 |
| AC-2 | 白名单有规则，地址命中其中一条 | ✅ 放行 |
| AC-3 | 白名单有规则，地址不命中任何规则 | ❌ 拒绝，返回「通道地址不在允许范围内」 |
| AC-4 | 白名单有规则，部分规则 status=0（失效） | 仅校验 status=1 的规则 |
| ~~AC-5~~ | ~~字典写入非法正则~~ | ~~market-server 已实现，无需验证~~ |
| ~~AC-6~~ | ~~字典写入合法正则~~ | ~~market-server 已实现，无需验证~~ |
| AC-7 | 通道类型为非 WebHook（如企业内部消息平台） | 不做白名单校验，直接放行 |
| AC-8 | 正则全串匹配：`^https://a\.com/callback$`，地址 `https://a.com/callback/extra` | ❌ 拒绝（不匹配） |
| AC-9 | 正则全串匹配：`^https://a\.com/.*$`，地址 `https://a.com/callback` | ✅ 放行（匹配） |

---

## 6. 测试计划

### 6.1 单元测试

| 测试类 | 覆盖范围 |
|--------|---------|
| `ChannelAddressWhitelistValidatorTest` | 空白名单放行、命中放行、不命中拒绝、失效规则过滤、正则编译异常 |
| ~~`DictionaryServiceTest`（扩展）~~ | ~~market-server 已实现，无需新增测试~~ |
| `EventSubscriptionServiceTest`（扩展） | 事件配置 WebHook 时触发校验、非 WebHook 不触发 |
| `CallbackSubscriptionServiceTest`（扩展） | 回调配置 WebHook 时触发校验、非 WebHook 不触发 |

### 6.2 集成测试

| 场景 | 步骤 | 预期 |
|------|------|------|
| 端到端拦截 | ① 字典写入白名单规则（market-server） ② 消费方配置不匹配的通道地址 | 返回 400 |
| 端到端放行 | ① 字典写入白名单规则（market-server） ② 消费方配置匹配的通道地址 | 返回 200 |
| 空白名单兼容 | ① 不配置任何白名单规则 ② 消费方配置任意通道地址 | 返回 200 |
| 规则热生效 | ① 消费方配置地址被拦截 ② 管理员新增匹配规则（market-server） ③ 消费方重新配置 | 返回 200（无缓存，实时生效） |

---

## 7. 后续增强（本期不做）

| 增强项 | 说明 | 触发条件 |
|--------|------|---------|
| 运行时网关校验 | FR-029/FR-030 分发/路由前二次校验 | 安全审计要求提升时 |
| 存量地址巡检 | 扫描已有 channelAddress，标记不在白名单内的记录 | 白名单规则稳定后 |
| 空白名单语义收紧 | 空 = 全部禁止（当前空 = 不限制） | 所有消费方均已配置白名单地址 |
| 平台级兜底规则 | 对齐 connector V3 plan-config #2 `url_regex_pattern` | 需要全局统一兜底时 |
| Caffeine 缓存 | 如写入频次升高，加缓存减少 DB 查询 | 性能瓶颈出现时 |

---

## 8. 文件清单

| 文件 | 操作 | 说明 |
|------|:---:|------|
| `ADR-004.md` | 新建 | ✅ 已完成 |
| `plan-channel-whitelist.md` | 新建 | ✅ 本文档 |
| `ChannelAddressWhitelistValidator.java` | 新建 | 校验器 |
| `ChannelAddressNotAllowedException.java` | 新建 | 异常 |
| `EventSubscriptionService.java` | 修改 | 集成校验 |
| `CallbackSubscriptionService.java` | 修改 | 集成校验 |
| `OpenplatformPropertyMapper.java/xml` | 修改 | 新增 `selectValuesByPathAndCodePrefix` 方法 |
| `ChannelAddressWhitelistValidatorTest.java` | 新建 | 单元测试 |
| 相关 Service 测试类 | 修改 | 扩展测试用例 |
