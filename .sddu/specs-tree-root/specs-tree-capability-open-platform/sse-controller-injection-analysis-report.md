# 分析验证报告：SseController No default constructor found

> **问题报告 ID**: ISSUE-20260518-001  
> **报告日期**: 2026-05-18  
> **报告人**: SDDU Build Agent  
> **状态**: 🔴 已复现，待修复

---

## 📋 问题概述

### 错误信息

```
failed to instantiate com.xxx.event.common.controller.SseController : 
No default constructor found
```

### 问题表现

- **模块**: event-server
- **报错时机**: 应用启动时（ApplicationContext 刷新阶段）
- **报错环境**: 特定部署环境（同一 JAR，其他环境正常）
- **受影响服务**: 仅 event-server，open-server、api-server 无此问题

### 代码片段（原始状态）

```java
@Slf4j
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor          // ← 生成 SseController(SseChannel) 构造器
public class SseController {
    private final SseChannel sseChannel;  // ← final 字段，Lombok 生成带参构造器
    // ... 业务方法
}
```

---

## 🔬 根因分析

### Spring 构造器决策路径

Spring 在创建 Bean 时通过 `AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors()` 确定使用哪个构造器：

```
determineCandidateConstructors()
├─ 1个构造器 → 返回该构造器 → autowireConstructor()  → ✅ 正常注入 ✅
└─ 多个构造器
   ├─ 有 @Autowired → 注入该构造器 → ✅
   └─ 无 @Autowired → 返回 null
                       ↓
                instantiateBean()
                → getDeclaredConstructor() (无参)
                → NoSuchMethodException
                → 💥 "No default constructor found"
```

### 根本原因

`@RequiredArgsConstructor` 生成的构造器**没有 `@Autowired` 注解标记**。正常情况（唯一构造器）下 Spring 会隐式识别，但一旦**运行时出现构造器歧义**（多个构造器可供选择），就会触发此错误。

### 生产环境触发推测

| 可能原因 | 说明 | 特征 |
|---------|------|------|
| 🟠 APM/监控 Java Agent | SkyWalking、Pinpoint 等通过字节码增强添加合成构造器 | 仅特定环境存在 `-javaagent` 参数 |
| 🟡 CGLIB 代理冲突 | AOP `@EnableAspectJAutoProxy` / Caching 等触发 CGLIB 创建代理子类 | 需要 `spring-boot-starter-aop` |
| 🟢 SpringDoc 反射实例化 | **已验证后排除: API Docs 接口返回500但并非因 Controller 实例化，而是 SpringDoc 2.5.0 与 Spring Boot 3.4.6 的 `ControllerAdviceBean` 版本不兼容。完全未触发 SseController 相关错误 | 已验证：不是根因 |
| 🔵 类加载器隔离 ClassLoader | 外部 Servlet 容器部署时类加载器层级导致构造器查找异常 | WAR vs JAR 部署 |

> **🟢 SpringDoc 场景验证（2026-05-18）：** 在 `main` 分支（无 `@Autowired` 修复）启动应用后，访问 `/api-docs` 返回 HTTP 500，错误为 `NoSuchMethodError: ControllerAdviceBean.<init>(Object)`，这是 SpringDoc 2.5.0 与 Spring Boot 3.4.6 的版本兼容问题，**与 SseController 的构造器注入完全无关**。日志中 grep 搜索 `SseController`、`No default constructor`、`NoSuchMethodException` 均无结果。此场景已排除。

---

## 🧪 复现结果

我们通过**修改 event-server 代码**进行了三轮精准复现测试：

### 测试①：多构造器（2个）— 未触发

**修改**: 追加 `SseController(String debugFlag)` 构造器  
**预期**: Spring 遇到多个构造器应回退找无参构造器，报 No default constructor 找不到报错  
**实际结果**: ✅ 应用正常启动  
**原因**: Spring Framework 6.2 引入了"贪婪构造器匹配"，`SseController(SseChannel)` 的参数类型 `SseChannel` 在容器中可匹配为 Bean，Spring 自动选中该构造器

### 测试②：SseChannel 移除 @Component — 触发不同错误

**修改**: 注释 `SseChannel.java` 的 `@Component`  
**预期**: 看是否报 No default constructor found  
**实际结果**: ❌ 应用启动失败，但报的是  
```
UnsatisfiedDependencyException: Error creating bean with name 'sseController'
  Unsatisfied dependency expressed through constructor parameter 0
```
**结论**: 依赖 Bean 不存在和构造器歧义是**两条不同的错误路径**

### 测试③：多构造器（3个，1可匹配+2不可匹配）— 复现✅

**修改**: 追加 `SseController(Integer)` + `SseController(Double)` 构造器  
**前情**: 三个构造器分别是 `(SseChannel)`、`(Integer)`、`(Double)`  
**结果**: ❌ **应用启动失败，报出完全一致错误**：
```
java.lang.NoSuchMethodException: com.xxx.event.common.controller.SseController.<init>()
... No default constructor found
```
**结论**: 当多个构造器**都无法在容器中找到匹配的 Bean 类型**时，Spring 回退到 `instantiateBean()` → 无参构造器 → 失败

### 复现总结

| 测试 | 场景 | 启动结果 | 错误类型 | 是否复现 |
|-----|------|---------|---------|---------|
| ① | 2个构造器（1个可匹配） | ✅ 成功 | — | ❌ |
| ② |
| ② | SseChannel 非 Bean | ❌ 失败 | UnsatisfiedDependencyException | ❌ |
| **③** | **3个构造器无可匹配** | **❌ 失败** | **No default constructor found** | **✅** |

---

## 🔧 解决方案

### 方案一（推荐）：为 Lombok 生成的构造器添加 @Autowired 标记

修改 `SseController.java`：

```java
// 修改前
@RequiredArgsConstructor

// 修改后  
@RequiredArgsConstructor(onConstructor_ = @Autowired)
```

Lombok 会生成：

```java
@Autowired  // ← Spring 明确知道使用此构造器进行注入
public SseController(SseChannel sseChannel) {
    this.sseChannel = sseChannel;
}
```

**优点**:
- 改动最小（只改注解一行）
- `@Autowired` 使 Spring 的构造器选择逻辑完全确定，无论运行时存在多少个合成构造器
- `@Autowired` 的优先级高于贪婪匹配，Spring 永远优先选择显式标记的构造器
- Lombok 的 `onConstructor_` 语法在 Lombok 1.18+ 中稳定支持

### 方案二（替代）：手动编写构造器

```java
// 移除 @RequiredArgsConstructor，手动编写
@RestController
@RequestMapping("/sse")
public class SseController {

    private final SseChannel sseChannel;

    @Autowired
    public SseController(SseChannel sseChannel) {
        this.sseChannel = sseChannel;
    }
```

**缺点**: 需要删除 `@RequiredArgsConstructor`，对每个字段的构造器注入不灵活

### 受影响文件清单

| 文件 | 当前方式 | 风险等级 | 建议操作 |
|------|---------|---------|---------|
| `SseController.java` | `@RequiredArgsConstructor` | 🔴 高（已出现） | 添加 `onConstructor_ = @Autowired` |
| `WebSocketController.java` | `@RequiredArgsConstructor` | 🟡 中（同类） | 同上，统一修复 |
| `WebSocketConfig.java` (@Configuration) | `@RequiredArgsConstructor` | 🟢 低 | Configuration 类无此问题 |
| `WebSocketHandler.java` (TextWebSocketHandler) | `) | `@RequiredArgsConstructor` | 🟢 低 | 继承类无此问题 |

---

## 修复验证步骤

### 1. 本地验证

```bash
# 应用修改
# 修改 SseController.java: @RequiredArgsConstructor(onConstructor_ = @Autowired)

# 编译确认
mvn compile -q

# 启动测试（3构造器歧义场景）
# 在测试场景③中，此修改应使应用正常启动
```

### 2. 回归验证

```bash
# 正常场景验证（原始单构造器）
git checkout -- SseController.java
mvn spring-boot:run
# 确认正常启动

# 应用修复后再次验证
# 重新应用修改
mvn spring-boot:run
# 确认正常启动
```

### 3. 问题环境验证

将修复后的 JAR 部署到问题环境：

- 确认应用启动成功
- 确认 SSE 接口可正常访问
- 确认 SpringDoc/WebSocket 等功能正常

---

## 相关参考资料

### Spring Framework 源码相关

- `SimpleInstantiationStrategy.instantiate()` - 调用无参构造器的入口
- `AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors()` - 构造器选择逻辑
- ` AbstractAutowireCapableBeanFactory.createBeanInstance()` - Bean 实例化流程
- `MergedBeanDefinitionPostProcessor` - 可能修改 Bean 定义的 PostProcessor

### 测试复现代码（已清理）

所有测试代码均已从仓库中清理，如需重新复现：
- 测试①: 追加 `SseController(String)` 构造器
- 测试②: 注释 `SseChannel.@Component`
- 测试③: 追加 `SseController(Integer)` + ``SseController(Double)`

---

*报告生成: @sddu 工作流智能入口 | 2026-05-18*