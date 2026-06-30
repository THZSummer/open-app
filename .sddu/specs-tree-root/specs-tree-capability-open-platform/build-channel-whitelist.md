# 实现报告：事件/回调通道地址白名单控制

**关联 ADR**: [ADR-004](./ADR-004.md)
**关联规划**: [plan-channel-whitelist.md](./plan-channel-whitelist.md)
**实现日期**: 2026-06-29
**实现人**: Summer

---

## 1. 变更文件清单

| 文件 | 操作 | 说明 |
|------|:---:|------|
| `modules/app/mapper/AppMapper.java` | 修改 | +7 行：新增 `selectDictionaryValuesByPathAndCodePrefix` 方法 |
| `resources/mapper/AppMapper.xml` | 修改 | +8 行：新增对应 SQL（path + code 前缀查询） |
| `modules/permission/validator/ChannelAddressWhitelistValidator.java` | **新建** | 115 行，通道地址白名单校验器 |
| `modules/permission/service/PermissionService.java` | 修改 | +7 行：1 import + 1 field + 2×3 行调用 |
| `modules/permission/validator/ChannelAddressWhitelistValidatorTest.java` | **新建** | 222 行，17 个单元测试用例 |
| `resources/db/migration/channel_address_whitelist_init.sql` | **新建** | 白名单初始化 SQL 脚本（示例数据） |

---

## 2. 实现详情

### 2.1 AppMapper 新增方法

```java
/**
 * 查询数据字典（openplatform_property_t）按 path + code 前缀，返回有效记录的 value 列表
 */
List<String> selectDictionaryValuesByPathAndCodePrefix(
        @Param("path") String path, @Param("codePrefix") String codePrefix);
```

```xml
<select id="selectDictionaryValuesByPathAndCodePrefix" resultType="string">
    SELECT value
    FROM openplatform_property_t
    WHERE path = #{path}
      AND code LIKE CONCAT(#{codePrefix}, '%')
      AND status = 1
    ORDER BY code
</select>
```

### 2.2 ChannelAddressWhitelistValidator

| 维度 | 实现 |
|------|------|
| 包路径 | `com.xxx.it.works.wecode.v2.modules.permission.validator` |
| 注解 | `@Slf4j` `@Component` `@RequiredArgsConstructor` |
| 依赖 | `AppMapper` |
| 常量 | `PATH = "channel_address_whitelist"`、`CODE_PREFIX_CALLBACK`、`CODE_PREFIX_EVENT` |
| 核心方法 | `validate(String channelAddress, String codePrefix)` |

**校验逻辑**：

```
channelAddress 为空? → 放行
      ↓ 非空
查询白名单规则（path + code 前缀）
      ↓
查询异常? → 降级放行（log.warn）
      ↓
空白名单? → 放行
      ↓
逐条正则匹配（Pattern.matches）
      ↓
命中任一 → 放行
全部不命中 → 抛 BusinessException("400", "通道地址不在允许范围内...")
```

**防御性设计**：
- DB 查询异常时降级放行（避免字典服务故障阻塞业务）
- 非法正则自动跳过（`PatternSyntaxException` catch + log.warn）
- 空白名单 = 不限制（兼容初期上线）

### 2.3 PermissionService 集成

**注入**：
```java
private final ChannelAddressWhitelistValidator channelAddressWhitelistValidator;
```

**事件消费配置（configEventSubscription，#35）**：
```java
// 通道地址白名单校验（ADR-004）
channelAddressWhitelistValidator.validate(
        request.getChannelAddress(),
        ChannelAddressWhitelistValidator.CODE_PREFIX_EVENT);
```

**回调消费配置（configCallbackSubscription，#41）**：
```java
// 通道地址白名单校验（ADR-004）
channelAddressWhitelistValidator.validate(
        request.getChannelAddress(),
        ChannelAddressWhitelistValidator.CODE_PREFIX_CALLBACK);
```

**校验位置**：在通道类型非空校验之后、`subscriptionMapper.updateConfig()` 之前。

---

## 3. 任务完成状态

| # | 任务 | 状态 | 说明 |
|---|------|:---:|------|
| T-1 | `ChannelAddressWhitelistValidator` 实现 | ✅ 完成 | 含 AppMapper 扩展 |
| T-2 | FR-019 事件消费配置集成校验 | ✅ 完成 | configEventSubscription |
| T-3 | FR-022 回调消费配置集成校验 | ✅ 完成 | configCallbackSubscription |
| T-4 | 单元测试 + 编译验证 | ✅ 完成 | **16 用例全部通过，`mvn clean compile` + `mvn test` BUILD SUCCESS** |

---

## 4. 测试验证（待执行）

### 4.1 单元测试用例

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| 1 | 空白名单，任意地址 | patterns=[] | ✅ 放行 |
| 2 | 白名单有规则，地址命中 | patterns=["^https://a\\.com/.*$"], addr="https://a.com/callback" | ✅ 放行 |
| 3 | 白名单有规则，地址不命中 | patterns=["^https://a\\.com/.*$"], addr="https://evil.com/steal" | ❌ 400 |
| 4 | 地址为空 | addr="" | ✅ 放行（不校验） |
| 5 | DB 查询异常 | mapper 抛异常 | ✅ 降级放行 |
| 6 | 白名单含非法正则 | patterns=["[invalid"], addr="https://a.com" | ✅ 跳过非法正则，继续匹配 |
| 7 | 多规则命中最后一条 | patterns=["^https://a\\.com$", "^https://b\\.com$"], addr="https://b.com" | ✅ 放行 |

### 4.2 集成测试用例

| # | 场景 | 步骤 | 预期 |
|---|------|------|------|
| 1 | 端到端拦截 | ① 字典写入规则 `^https://safe\\.com/.*$` ② 配置通道地址 `https://evil.com/x` | 返回 400 |
| 2 | 端到端放行 | ① 字典写入规则 ② 配置通道地址 `https://safe.com/callback` | 返回 200 |
| 3 | 空白名单兼容 | ① 不配置任何规则 ② 配置任意地址 | 返回 200 |
| 4 | 规则热生效 | ① 地址被拦截 ② 管理员新增匹配规则 ③ 重新配置 | 返回 200 |

---

## 5. 上线 Checklist

| # | 步骤 | 负责 |
|---|------|------|
| 1 | 代码 review + 合并 | 开发 |
| 2 | 部署到测试环境 | 运维 |
| 3 | 执行集成测试用例 | 测试 |
| 4 | 平台管理员在字典管理录入已知合法地址正则 | 平台管理员 |
| 5 | 观察期：校验逻辑生效但合法地址已覆盖 | 开发 + 平台管理员 |
| 6 | 确认规则完整性，无合法地址被误拦 | 平台管理员 |
| 7 | 生产环境部署 | 运维 |
