# Java 开发规范

> **版本**: v1.0  
> **适用项目**: open-app-v5（应用管理模块）  
> **参考规范**: 阿里巴巴 Java 开发手册（嵩山版）+ 项目特定规范  
> **最后更新**: 2026-06-05

---

## 目录

1. [编程规约](#一编程规约)
   - [命名风格](#一命名风格)
   - [常量定义](#二常量定义)
   - [代码格式](#三代码格式)
   - [OOP 规约](#四oop-规约)
   - [注释规约](#五注释规约)
   - [控制语句](#六控制语句)
2. [异常日志](#二异常日志)
   - [异常处理](#一异常处理)
   - [日志规约](#二日志规约)
3. [MySQL 数据库](#三mysql-数据库)
   - [建表规约](#一建表规约)
   - [SQL 语句](#二sql-语句)
   - [ORM 映射](#三orm-映射)
4. [项目特定规范](#四项目特定规范)

---

## 一、编程规约

### （一）命名风格

#### 1. 【强制】代码中的命名均不能以下划线或美元符号开始，也不能以下划线或美元符号结束

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `name` / `userName` / `appId` | `_name` / `__name` / `$name` / `name_` / `name$` |

```java
// ✅ 正确示例
public class AppController {
    private String appId;
    private String userName;
}

// ❌ 错误示例
public class AppController {
    private String _appId;      // 禁止下划线开头
    private String userName$;   // 禁止美元符号结尾
}
```

**原因**：下划线和美元符号在命名中无实际意义，容易引起混淆。

---

#### 2. 【强制】代码中的命名严禁使用拼音与英文混合的方式，更不允许直接使用中文的方式

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `ali` / `alibaba` / `taobao` | `DaZhePromotion` / `getPingfenByName()` / `String fw` |

```java
// ✅ 正确示例
public class AppService {
    public AppDetail getAppDetail(String appId) { ... }
}

// ❌ 错误示例
public class AppService {
    public AppDetail getAppXiangQing(String appId) { ... }  // 拼音混合
    public AppDetail getApp详情(String appId) { ... }       // 中文命名
}
```

**说明**：正确的英文拼写和语法可以让阅读者易于理解，避免歧义。注意，纯拼音命名方式也要避免采用。

---

#### 3. 【强制】类名使用 UpperCamelCase 风格，但以下情形例外：DO / BO / DTO / VO / AO / PO / UID 等

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `ForceCode` / `UserDO` / `HtmlDTO` / `XmlService` | `forcecode` / `UserDo` / `HTMLDto` / `XMLService` |

```java
// ✅ 正确示例
public class AppController { }
public class CreateAppRequestDTO { }
public class AppDetailVO { }
public class AppMemberDO { }

// ❌ 错误示例
public class appController { }           // 首字母小写
public class CreateAppRequestDto { }    // DTO 应该大写
public class APPDetailVO { }             // 全大写缩写不应连用
```

**分层领域模型规约**：
- **DO（Data Object）**：与数据库表结构一一对应，通过 DAO 层向上传输数据源对象。
- **DTO（Data Transfer Object）**：数据传输对象，Service 或 Manager 向外传输的对象。
- **BO（Business Object）**：业务对象，由 Service 层输出的封装业务逻辑的对象。
- **VO（View Object）**：显示层对象，通常是 Web 向模板渲染引擎层传输的对象。
- **Query**：数据查询对象，各层接收上层的查询请求。注意超过 2 个参数的查询封装，禁止使用 Map 类来传输。

---

#### 4. 【强制】方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase 风格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `localValue` / `getHttpMessage()` / `inputUserId` | `LocalValue` / `getHTTPMessage()` / `input_user_id` |

```java
// ✅ 正确示例
public class AppService {
    private String appId;                    // 成员变量：lowerCamelCase
    
    public AppDetail getAppDetail(String appId) {  // 方法名 + 参数：lowerCamelCase
        String localValue = "test";          // 局部变量：lowerCamelCase
        return appMapper.selectById(appId);
    }
}

// ❌ 错误示例
public class AppService {
    private String AppId;                    // 成员变量首字母大写
    
    public AppDetail GetAppDetail(String app_id) {  // 方法名大写 + 参数下划线
        String LocalValue = "test";          // 局部变量首字母大写
        return appMapper.selectById(app_id);
    }
}
```

---

#### 5. 【强制】常量命名全部大写，单词间用下划线隔开，力求语义表达完整清楚，不要嫌名字长

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `MAX_STOCK_COUNT` / `CACHE_EXPIRED_TIME` | `MAX_COUNT` / `EXPIRED_TIME` |

```java
// ✅ 正确示例
public class AppConstants {
    public static final int MAX_APP_NAME_LENGTH = 255;
    public static final String DEFAULT_APP_ICON = "default_icon.png";
    public static final int CACHE_EXPIRED_TIME_SECONDS = 3600;
}

// ❌ 错误示例
public class AppConstants {
    public static final int MAX_LENGTH = 255;              // 语义不清
    public static final String defaultIcon = "default.png"; // 未大写
    public static final int cacheExpired = 3600;            // 未大写
}
```

---

#### 6. 【强制】抽象类命名使用 Abstract 或 Base 开头；异常类命名使用 Exception 结尾；测试类命名以它要测试的类的名称开始，以 Test 结尾

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `AbstractAppService` / `BaseController` | `AppServiceAbstract` |
| `AppNotFoundException` / `PermissionDeniedException` | `AppNotFound` / `PermissionError` |
| `AppServiceTest` / `MemberControllerTest` | `TestAppService` / `MemberTest` |

```java
// ✅ 正确示例
public abstract class AbstractAppService { }
public class BaseAppController { }
public class AppNotFoundException extends RuntimeException { }
public class AppServiceTest { }

// ❌ 错误示例
public abstract class AppServiceAbstract { }    // Abstract 应在开头
public class AppNotFound extends RuntimeException { }  // Exception 应结尾
public class TestAppService { }                // Test 应结尾
```

---

#### 7. 【强制】类型与中括号紧挨相连来表示数组

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `int[] arrayDemo` | `int arrayDemo[]` |

```java
// ✅ 正确示例
public class AppService {
    private String[] appIds;
    private int[] statusList;
    
    public void process(String[] ids) { }
}

// ❌ 错误示例
public class AppService {
    private String appIds[];        // 中括号应在类型后
    private int statusList[];
    
    public void process(String ids[]) { }
}
```

---

#### 8. 【强制】POJO 类中的布尔类型的变量，都不要加 is 前缀，否则部分框架解析会引起序列化错误

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `Boolean deleted` / `Boolean bound` | `Boolean isDeleted` / `Boolean isBound` |

```java
// ✅ 正确示例
public class AppDO {
    private Boolean deleted;      // 数据库字段：is_deleted
    private Boolean bound;        // 数据库字段：is_bound
    
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}

// ❌ 错误示例
public class AppDO {
    private Boolean isDeleted;    // ❌ 前缀 is 会导致序列化问题
    
    public Boolean getDeleted() { return isDeleted; }  // 方法名自动去掉 is，导致属性名混淆
}
```

**说明**：在本文 MySQL 规约中的建表约定第一条，表达是与否的变量采用 `is_xxx` 的命名方式，所以需要在 `<resultMap>` 设置从 `is_xxx` 到 `xxx` 的映射关系。

---

#### 9. 【强制】包名统一使用小写，点分隔符之间有且仅有一个自然语义的英语单词。包名统一使用单数形式，但是类名如果有复数含义，类名可以使用复数形式

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `com.xxx.works.app` / `com.xxx.works.service` | `com.xxx.works.App` / `com.xxx.works.appService` |

```java
// ✅ 正确示例
package com.xxx.works.app.controller;
package com.xxx.works.app.service;
package com.xxx.works.app.dto;

// ❌ 错误示例
package com.xxx.works.App;           // 包名大写
package com.xxx.works.appService;    // 驼峰命名
package com.xxx.works.app.dto.vo;    // 多个单词连在一起
```

---

#### 10. 【强制】杜绝完全不规范的缩写，避免望文不知义

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `AbstractClass` / `condition` / `Function` | `AbsClass` / `condi` / `Fu` |

```java
// ✅ 正确示例
public class AppService {
    public void createApplication(AppRequest request) { }
    public void updateConfiguration(ConfigRequest request) { }
}

// ❌ 错误示例
public class AppService {
    public void createApp(AppRequest request) { }       // App 可接受（通用缩写）
    public void updateCfg(CfgRequest request) { }       // ❌ 不规范缩写
    public void updConf(ConfigRequest request) { }      // ❌ upd 不规范
}
```

---

#### 11. 【推荐】为了达到代码自解释的目标，任何定义编程元素在命名时，使用尽量完整单词组合来表达其意

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `PullCodeFromRemoteRepository` | `int a` |

```java
// ✅ 正确示例
public class AppService {
    public void updateAppBasicInfo(String appId, AppBasicInfo info) { }
    public List<AppMember> getAppMemberList(String appId) { }
}

// ❌ 错误示例
public class AppService {
    public void update(String id, AppBasicInfo i) { }  // 参数名语义不清
    public List<AppMember> getList(String id) { }      // 方法名语义不清
}
```

---

#### 12. 【推荐】如果模块、接口、类、方法使用了设计模式，在命名时体现出具体模式，将设计模式体现在名字中，有利于阅读者快速理解架构设计理念

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `OrderFactory` / `LoginProxy` / `ResourceObserver` | - |

```java
// ✅ 正确示例
public class AppFactory {                    // 工厂模式
    public static App createApp(String type) { }
}

public class AppServiceProxy {               // 代理模式
    private AppService target;
}

public class PermissionObserver {            // 观察者模式
    public void onPermissionChanged(PermissionEvent event) { }
}
```

---

#### 13. 【推荐】接口类中的方法和属性不要加任何修饰符号（public 也不要加），保持代码的简洁性，并加上有效的 Javadoc 注释

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `void f();` / `String COMPANY = "alibaba";` | `public abstract void f();` |

```java
// ✅ 正确示例
public interface AppService {
    /**
     * 创建应用
     */
    AppDetail createApp(CreateAppRequest request);
    
    /**
     * 应用名称最大长度
     */
    int MAX_NAME_LENGTH = 255;
}

// ❌ 错误示例
public interface AppService {
    public abstract AppDetail createApp(CreateAppRequest request);  // 多余修饰符
    public static final int MAX_NAME_LENGTH = 255;                  // 多余修饰符
}
```

---

#### 14. 【强制】接口和实现类的命名有两套规则

**规则 1**：对于 Service 和 DAO 类，基于 SOA 的理念，暴露出来的服务一定是接口，内部的实现类用 Impl 的后缀与接口区别

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `CacheServiceImpl` 实现 `CacheService` 接口 | `CacheService` 实现 `CacheInterface` |

```java
// ✅ 正确示例
public interface AppService {
    AppDetail getApp(String appId);
}

public class AppServiceImpl implements AppService {
    @Override
    public AppDetail getApp(String appId) { ... }
}

// ❌ 错误示例
public interface AppServiceInterface {    // ❌ 接口名不应有 Interface 后缀
    AppDetail getApp(String appId);
}

public class AppService implements AppServiceInterface { ... }
```

**规则 2**：如果是形容能力的接口名称，取对应的形容词为接口名（通常是 –able 的形式）

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `AbstractTranslator` 实现 `Translatable` | - |

```java
// ✅ 正确示例
public interface Translatable {
    String translate(String text);
}

public abstract class AbstractTranslator implements Translatable { }
```

---

#### 15. 【参考】枚举类名建议带上 Enum 后缀，枚举成员名称需要全大写，单词间用下划线隔开

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `ProcessStatusEnum.SUCCESS` / `ProcessStatusEnum.UNKNOWN_REASON` | `ProcessStatus.Success` |

```java
// ✅ 正确示例
public enum AppStatusEnum {
    ACTIVE(1, "有效"),
    INACTIVE(0, "无效"),
    DELETED(-1, "已删除");
    
    private final int code;
    private final String desc;
    
    AppStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

// ❌ 错误示例
public enum AppStatus {
    Active(1, "有效"),          // ❌ 未全大写
    Inactive(0, "无效"),        // ❌ 未全大写
    Deleted(-1, "已删除");      // ❌ 未全大写
}
```

---

#### 16. 【参考】各层命名规约

**Service/DAO 层方法命名规约**：
1. 获取单个对象的方法用 `get` 做前缀。
2. 获取多个对象的方法用 `list` 做前缀，复数结尾，如：`listObjects`。
3. 获取统计值的方法用 `count` 做前缀。
4. 插入的方法用 `save/insert` 做前缀。
5. 删除的方法用 `remove/delete` 做前缀。
6. 修改的方法用 `update` 做前缀。

```java
// ✅ 正确示例
public interface AppService {
    AppDetail getAppById(String appId);                 // 获取单个对象
    List<AppDetail> listAppsByOwner(String userId);     // 获取多个对象
    int countAppsByStatus(Integer status);              // 获取统计值
    void saveApp(CreateAppRequest request);             // 插入
    void removeApp(String appId);                       // 删除
    void updateAppBasicInfo(UpdateAppRequest request);  // 更新
}
```

---

### （二）常量定义

#### 1. 【强制】不允许任何魔法值（即未经预先定义的常量）直接出现在代码中

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `String key = "Id#" + tradeId;` | `String key = "Id#taobao_" + tradeId;` |

```java
// ✅ 正确示例
public class AppConstants {
    public static final String CACHE_KEY_PREFIX = "app:";
    public static final String ID_SEPARATOR = "#";
}

public class AppService {
    public String buildCacheKey(String appId) {
        return AppConstants.CACHE_KEY_PREFIX + appId;
    }
}

// ❌ 错误示例
public class AppService {
    public String buildCacheKey(String appId) {
        return "app:" + appId;          // ❌ 魔法值 "app:"
    }
    
    public void process(String key) {
        if (key.startsWith("Id#")) {    // ❌ 魔法值 "Id#"
            // ...
        }
    }
}
```

---

#### 2. 【强制】long 或者 Long 初始赋值时，使用大写的 L，不能是小写的 l，小写容易跟数字 1 混淆，造成误解

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `Long a = 2L;` | `Long a = 2l;` |

```java
// ✅ 正确示例
public class AppConstants {
    public static final Long MAX_FILE_SIZE = 10485760L;    // 10MB
    public static final Long CACHE_EXPIRED_TIME = 3600L;   // 1 hour
}

// ❌ 错误示例
public class AppConstants {
    public static final Long MAX_FILE_SIZE = 10485760l;    // ❌ 小写 l 容易混淆为数字 1
    public static final Long CACHE_EXPIRED_TIME = 3600l;
}
```

---

#### 3. 【推荐】不要使用一个常量类维护所有常量，按常量功能进行归类，分开维护

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `CacheConsts` / `ConfigConsts` | `Constants` |

```java
// ✅ 正确示例 - 按功能分类
public class CacheConsts {
    public static final String APP_CACHE_PREFIX = "app:";
    public static final int CACHE_EXPIRED_SECONDS = 3600;
}

public class ConfigConsts {
    public static final String DEFAULT_ICON = "default.png";
    public static final int MAX_NAME_LENGTH = 255;
}

// ❌ 错误示例 - 大而全的常量类
public class Constants {
    public static final String APP_CACHE_PREFIX = "app:";
    public static final int CACHE_EXPIRED_SECONDS = 3600;
    public static final String DEFAULT_ICON = "default.png";
    public static final int MAX_NAME_LENGTH = 255;
    // ... 所有常量混在一起
}
```

---

### （三）代码格式

#### 1. 【强制】大括号的使用约定。如果是大括号内为空，则简洁地写成 {} 即可，不需要换行；如果是非空代码块则：

1. 左大括号前不换行。
2. 左大括号后换行。
3. 右大括号前换行。
4. 右大括号后还有 else 等代码则不换行；表示终止的右大括号后必须换行。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 大括号规范使用 | 左大括号前换行 |

```java
// ✅ 正确示例
public class AppService {
    public void process(AppRequest request) {
        if (request == null) {
            return;
        }
        
        if (request.isValid()) {
            doSomething();
        } else {
            handleError();
        }
    }
    
    public void emptyMethod() {}
}

// ❌ 错误示例
public class AppService 
{                                    // ❌ 左大括号前换行
    public void process(AppRequest request) 
    {
        if (request == null) 
        {
            return;
        }
    }
}
```

---

#### 2. 【强制】左小括号和字符之间不出现空格；同样，右小括号和字符之间也不出现空格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `if (status == 1)` | `if ( status == 1 )` |

```java
// ✅ 正确示例
public class AppService {
    public void process(int status) {
        if (status == 1) {
            doSomething();
        }
        
        for (int i = 0; i < 10; i++) {
            processItem(i);
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void process(int status) {
        if ( status == 1 ) {            // ❌ 括号内多余空格
            doSomething();
        }
        
        for (int i = 0; i < 10; i ++ ) { // ❌ 括号内多余空格
            processItem(i);
        }
    }
}
```

---

#### 3. 【强制】if/for/while/switch/do 等保留字与括号之间都必须加空格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `if (status == 1)` | `if(status == 1)` |

```java
// ✅ 正确示例
public class AppService {
    public void process(int status) {
        if (status == 1) {              // if 和括号之间有空格
            doSomething();
        }
        
        for (int i = 0; i < 10; i++) {  // for 和括号之间有空格
            processItem(i);
        }
        
        while (hasNext()) {             // while 和括号之间有空格
            fetchNext();
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void process(int status) {
        if(status == 1) {               // ❌ 缺少空格
            doSomething();
        }
        
        for(int i = 0; i < 10; i++) {   // ❌ 缺少空格
            processItem(i);
        }
    }
}
```

---

#### 4. 【强制】任何二目、三目运算符的左右两边都需要加一个空格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `int a = 3;` / `if (a > 0)` | `int a=3;` / `if (a>0)` |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        int a = 3;                      // 赋值运算符两边有空格
        long b = 4L;
        
        if (a > 0 && b > 0) {           // 逻辑运算符两边有空格
            int result = a + b;         // 算术运算符两边有空格
        }
        
        String status = a > 0 ? "positive" : "negative";  // 三目运算符两边有空格
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        int a=3;                        // ❌ 赋值运算符缺少空格
        long b=4L;
        
        if (a>0 && b>0) {               // ❌ 比较运算符缺少空格
            int result=a+b;             // ❌ 算术运算符缺少空格
        }
    }
}
```

---

#### 5. 【强制】采用 4 个空格缩进，禁止使用 Tab 字符

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 4 个空格缩进 | Tab 缩进或 2 个空格 |

```java
// ✅ 正确示例 - 4 个空格
public class AppService {
    public void process(AppRequest request) {
        if (request == null) {
            return;
        }
        
        // 设置默认值
        if (request.getStatus() == null) {
            request.setStatus(0);
        }
        
        saveRequest(request);
    }
}

// ❌ 错误示例 - 2 个空格或 Tab
public class AppService {
  public void process(AppRequest request) {    // ❌ 2 个空格
    if (request == null) {
      return;
    }
  }
}
```

**IDE 配置**：
- IntelliJ IDEA: Settings → Editor → Code Style → Java → Tab size: 4
- Eclipse: Preferences → Java → Code Style → Formatter → Indentation
- VS Code: Settings → Editor: Tab Size: 4

---

#### 6. 【强制】注释的双斜线与内容之间有且仅一个空格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `// 这是示例注释` | `//这是示例注释` |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        // 这是示例注释，注意在双斜线之后有一个空格
        String name = "test";
        
        // 处理业务逻辑
        doSomething();
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        //这是示例注释，双斜线后没有空格
        String name = "test";
        
        //处理业务逻辑
        doSomething();
    }
}
```

---

#### 7. 【强制】单行字符数限制不超过 120 个，超出需要换行时遵循如下原则：

1. 第二行相对第一行缩进 4 个空格，从第三行开始不再继续缩进。
2. 运算符与下文一起换行。
3. 方法调用的点符号与下文一起换行。
4. 方法调用中的多个参数需要换行时，在逗号后进行。
5. 在括号前不要换行。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 合理换行 | 在括号前换行 |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        StringBuffer sb = new StringBuffer();
        // 超过 120 个字符的情况下，换行缩进 4 个空格，点号和方法名称一起换行
        sb.append("zi").append("xin")
          .append("huang")
          .append("huang")
          .append("huang");
        
        // 方法调用参数换行，在逗号后进行
        method("a", "b", "c",
               "d", "e", "f");
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        StringBuffer sb = new StringBuffer();
        // ❌ 不要在括号前换行
        sb.append("zi").append("xin").append
            ("huang");
        
        // ❌ 不要在逗号前换行
        method("a", "b", "c"
               , "d", "e", "f");
    }
}
```

---

#### 8. 【强制】方法参数在定义和传入时，多个参数逗号后边必须加空格

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `method("a", "b", "c")` | `method("a","b","c")` |

```java
// ✅ 正确示例
public class AppService {
    public void process(String appId, String userId, Integer status) {
        // 参数定义：逗号后有空格
    }
    
    public void call() {
        // 方法调用：逗号后有空格
        process("app001", "user001", 1);
    }
}

// ❌ 错误示例
public class AppService {
    public void process(String appId,String userId,Integer status) {
        // ❌ 逗号后缺少空格
    }
    
    public void call() {
        // ❌ 逗号后缺少空格
        process("app001","user001",1);
    }
}
```

---

#### 9. 【强制】IDE 的 text file encoding 设置为 UTF-8；IDE 中文件的换行符使用 Unix 格式，不要使用 Windows 格式

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| UTF-8 编码 + Unix 换行符（LF） | GBK 编码 + Windows 换行符（CRLF） |

**IDE 配置**：
- IntelliJ IDEA: Settings → Editor → File Encodings → Global Encoding: UTF-8
- IntelliJ IDEA: Settings → Editor → Code Style → Line separator: Unix and macOS (\n)

---

#### 10. 【推荐】没有必要增加若干空格来使某一行的字符与上一行对应位置的字符对齐

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 自然对齐 | 强制对齐 |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        int a = 3;
        long b = 4L;
        float c = 5F;
        StringBuffer sb = new StringBuffer();
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        int         a =  3;
        long        b =  4L;
        float       c =  5F;
        StringBuffer sb = new StringBuffer();
    }
}
```

**说明**：增加 sb 这个变量，如果需要对齐，则给 a、b、c 都要增加几个空格，在变量比较多的情况下，是非常累赘的事情。

---

#### 11. 【推荐】不同逻辑、不同语义、不同业务的代码之间插入一个空行分隔开来以提升可读性

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 逻辑块之间有空行 | 所有代码挤在一起 |

```java
// ✅ 正确示例
public class AppService {
    public void createApp(CreateAppRequest request) {
        // 参数校验
        validateRequest(request);
        
        // 检查应用名称唯一性
        checkAppNameUniqueness(request.getName());
        
        // 创建应用
        App app = buildApp(request);
        appMapper.insert(app);
        
        // 初始化成员
        createOwnerMember(app.getId(), request.getCreatorId());
        
        // 发送事件通知
        notifyAppCreated(app.getId());
    }
}

// ❌ 错误示例
public class AppService {
    public void createApp(CreateAppRequest request) {
        validateRequest(request);
        checkAppNameUniqueness(request.getName());
        App app = buildApp(request);
        appMapper.insert(app);
        createOwnerMember(app.getId(), request.getCreatorId());
        notifyAppCreated(app.getId());
    }
}
```

---

### （四）OOP 规约

#### 1. 【强制】避免通过一个类的对象引用访问此类的静态变量或静态方法，无谓增加编译器解析成本，直接用类名来访问即可

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `ClassName.staticMethod()` | `object.staticMethod()` |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        String value = AppConstants.DEFAULT_ICON;    // 通过类名访问静态变量
        int max = AppConstants.MAX_NAME_LENGTH;
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        AppConstants constants = new AppConstants();
        String value = constants.DEFAULT_ICON;       // ❌ 通过对象访问静态变量
        int max = constants.MAX_NAME_LENGTH;
    }
}
```

---

#### 2. 【强制】所有的覆写方法，必须加 @Override 注解

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 加上 `@Override` 注解 | 没有 `@Override` 注解 |

```java
// ✅ 正确示例
public class AppServiceImpl implements AppService {
    @Override
    public AppDetail getApp(String appId) {
        return appMapper.selectById(appId);
    }
    
    @Override
    public String toString() {
        return "AppServiceImpl{}";
    }
}

// ❌ 错误示例
public class AppServiceImpl implements AppService {
    public AppDetail getApp(String appId) {    // ❌ 缺少 @Override 注解
        return appMapper.selectById(appId);
    }
    
    public String toString() {                  // ❌ 缺少 @Override 注解
        return "AppServiceImpl{}";
    }
}
```

**原因**：`getObject()` 与 `get0bject()` 的问题。一个是字母的 O，一个是数字的 0，加 `@Override` 可以准确判断是否覆盖成功。

---

#### 3. 【强制】相同类型的包装类对象之间值的比较，全部使用 equals 方法比较

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `a.equals(b)` | `a == b` |

```java
// ✅ 正确示例
public class AppService {
    public void compare(Integer a, Integer b) {
        if (a.equals(b)) {
            // 正确比较
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void compare(Integer a, Integer b) {
        if (a == b) {           // ❌ 对象比较使用 == 可能错误
            // 错误比较
        }
    }
}
```

**说明**：对于 `Integer var = ?` 在 -128 至 127 范围内的赋值，Integer 对象是在 `IntegerCache.cache` 产生，会复用已有对象，这个区间内的 Integer 值可以直接使用 `==` 进行判断，但是这个区间之外的所有数据，都会在堆上产生，并不会复用已有对象，这是一个大坑，推荐使用 equals 方法进行判断。

---

#### 4. 【强制】所有的 POJO 类属性必须使用包装数据类型

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `private Integer status;` | `private int status;` |

```java
// ✅ 正确示例
public class AppDO {
    private Long id;
    private String appId;
    private Integer status;         // 使用包装类
    private Boolean deleted;        // 使用包装类
}

// ❌ 错误示例
public class AppDO {
    private long id;
    private String appId;
    private int status;             // ❌ 使用基本类型
    private boolean deleted;        // ❌ 使用基本类型
}
```

**说明**：POJO 类属性没有初值是提醒使用者在需要使用时，必须自己显式地进行赋值，任何 NPE 问题，或者入库检查，都由使用者来保证。数据库的查询结果可能是 null，因为自动拆箱，用基本数据类型接收有 NPE 风险。

---

#### 5. 【强制】RPC 方法的返回值和参数必须使用包装数据类型

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `public Integer getStatus()` | `public int getStatus()` |

```java
// ✅ 正确示例
public interface AppService {
    Integer getAppStatus(String appId);     // 返回包装类
    void updateStatus(String appId, Integer status);  // 参数使用包装类
}

// ❌ 错误示例
public interface AppService {
    int getAppStatus(String appId);         // ❌ 返回基本类型
    void updateStatus(String appId, int status);  // ❌ 参数使用基本类型
}
```

---

#### 6. 【推荐】所有的局部变量使用基本数据类型

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `int count = 0;` | `Integer count = 0;` |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        int count = 0;              // 局部变量使用基本类型
        long total = 0L;
        boolean flag = false;
        
        for (int i = 0; i < count; i++) {
            // ...
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        Integer count = 0;          // ❌ 局部变量使用包装类
        Long total = 0L;
        Boolean flag = false;
    }
}
```

---

#### 7. 【强制】POJO 类必须写 toString 方法

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 重写 `toString()` 方法 | 未重写 `toString()` 方法 |

```java
// ✅ 正确示例
public class AppDO {
    private Long id;
    private String appId;
    private String name;
    
    @Override
    public String toString() {
        return "AppDO{" +
                "id=" + id +
                ", appId='" + appId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

// ❌ 错误示例
public class AppDO {
    private Long id;
    private String appId;
    private String name;
    
    // ❌ 未重写 toString() 方法
}
```

**说明**：在方法执行抛出异常时，可以直接调用 POJO 的 `toString()` 方法打印其属性值，便于排查问题。使用 IDE 中的工具：source > generate toString 时，如果继承了另一个 POJO 类，注意在前面加一下 `super.toString()`。

---

#### 8. 【推荐】类内方法定义的顺序依次是：公有方法或保护方法 > 私有方法 > getter/setter 方法

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 方法顺序：公有 > 私有 > getter/setter | getter/setter 夹杂在业务方法中间 |

```java
// ✅ 正确示例
public class AppService {
    // 公有方法
    public AppDetail getApp(String appId) { }
    public void createApp(CreateAppRequest request) { }
    
    // 保护方法
    protected void validateRequest(CreateAppRequest request) { }
    
    // 私有方法
    private void checkAppNameUniqueness(String name) { }
    private App buildApp(CreateAppRequest request) { }
    
    // getter/setter 方法
    public String getAppName() { }
    public void setAppName(String appName) { }
}

// ❌ 错误示例
public class AppService {
    public AppDetail getApp(String appId) { }
    public String getAppName() { }              // ❌ getter 方法穿插在业务方法中
    public void createApp(CreateAppRequest request) { }
    public void setAppName(String appName) { }  // ❌ setter 方法穿插在业务方法中
    private void checkAppNameUniqueness(String name) { }
}
```

---

#### 9. 【推荐】循环体内，字符串的连接方式，使用 StringBuilder 的 append 方法进行扩展

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `StringBuilder.append()` | `str = str + "hello"` |

```java
// ✅ 正确示例
public class AppService {
    public String buildAppNames(List<App> apps) {
        StringBuilder sb = new StringBuilder();
        for (App app : apps) {
            sb.append(app.getName()).append(",");
        }
        return sb.toString();
    }
}

// ❌ 错误示例
public class AppService {
    public String buildAppNames(List<App> apps) {
        String result = "";
        for (App app : apps) {
            result = result + app.getName() + ",";  // ❌ 每次循环创建新的 String 对象
        }
        return result;
    }
}
```

**说明**：反编译出的字节码文件显示每次循环都会 new 出一个 StringBuilder 对象，然后进行 append 操作，最后通过 toString 方法返回 String 对象，造成内存资源浪费。

---

#### 10. 【强制】禁止使用构造器注入（private final），必须使用 @Autowired 注解注入

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 `@Autowired` 注解注入 | 使用 `private final` 构造器注入 |

```java
// ✅ 正确示例 - 使用 @Autowired 注解注入
@Service
public class AppService {
    
    @Autowired
    private AppMapper appMapper;
    
    @Autowired
    private MemberService memberService;
    
    @Autowired
    private PermissionService permissionService;
    
    public AppDetail getApp(String appId) {
        return appMapper.selectById(appId);
    }
}

// ❌ 错误示例 - 使用 private final 构造器注入
@Service
public class AppService {
    
    private final AppMapper appMapper;              // ❌ 禁止使用 private final
    
    private final MemberService memberService;      // ❌ 禁止使用 private final
    
    private final PermissionService permissionService;  // ❌ 禁止使用 private final
    
    public AppService(AppMapper appMapper, 
                      MemberService memberService,
                      PermissionService permissionService) {
        this.appMapper = appMapper;
        this.memberService = memberService;
        this.permissionService = permissionService;
    }
    
    public AppDetail getApp(String appId) {
        return appMapper.selectById(appId);
    }
}
```

**原因**：
1. **统一风格**：团队代码风格统一，避免多种注入方式混用
2. **简洁明了**：`@Autowired` 注解方式更简洁，减少样板代码
3. **易于维护**：新增依赖时只需添加一行注解，无需修改构造函数
4. **可读性强**：依赖关系一目了然，便于代码审查
5. **符合历史习惯**：项目历史代码均采用 `@Autowired` 注解方式

**注意事项**：
- `@Autowired` 注解可以放在字段上，也可以放在 setter 方法上
- 推荐直接放在字段上，更加简洁
- 如果存在循环依赖，可以考虑使用 `@Lazy` 注解配合 `@Autowired` 使用

---

#### 11. 【强制】所有 Controller 接口返回必须使用 `ApiResponse<T>` 统一格式，必须使用 `ResponseCodeEnum` 枚举

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 `ApiResponse<T>` 统一格式 | 直接返回业务对象或其他格式 |

```java
// ✅ 正确示例 - 使用 ApiResponse<T> 统一格式
@RestController
@RequestMapping("/api/v1/app")
public class AppController {
    
    @Autowired
    private AppService appService;
    
    /**
     * 根据 ID 获取应用详情
     */
    @GetMapping("/{appId}")
    public ApiResponse<AppDetailVO> getApp(@PathVariable String appId) {
        AppDetailVO appDetail = appService.getAppById(appId);
        return ApiResponse.success(appDetail);
    }
    
    /**
     * 创建应用
     */
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody @Valid CreateAppRequest request) {
        String appId = appService.createApp(request);
        return ApiResponse.success(appId);
    }
    
    /**
     * 更新应用基本信息
     */
    @PutMapping("/{appId}/basic-info")
    public ApiResponse<Void> updateBasicInfo(
            @PathVariable String appId,
            @RequestBody @Valid UpdateAppRequest request) {
        appService.updateBasicInfo(appId, request);
        return ApiResponse.success();
    }
}

// ❌ 错误示例 - 直接返回业务对象
@RestController
@RequestMapping("/api/v1/app")
public class AppController {
    
    @GetMapping("/{appId}")
    public AppDetailVO getApp(@PathVariable String appId) {  // ❌ 直接返回业务对象
        return appService.getAppById(appId);
    }
    
    @PostMapping
    public String createApp(@RequestBody CreateAppRequest request) {  // ❌ 直接返回 String
        return appService.createApp(request);
    }
    
    @PutMapping("/{appId}/basic-info")
    public void updateBasicInfo(@PathVariable String appId,  // ❌ 返回 void
                                 @RequestBody UpdateAppRequest request) {
        appService.updateBasicInfo(appId, request);
    }
}
```

**`ApiResponse<T>` 结构定义**：

```java
/**
 * 统一 API 响应格式
 * 
 * @param <T> 数据类型
 */
public class ApiResponse<T> {
    
    /**
     * 响应码（来自 ResponseCodeEnum）
     */
    private String code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(ResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(ResponseCodeEnum.SUCCESS.getMessage());
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 成功响应（无数据）
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    // 失败响应
    public static <T> ApiResponse<T> error(ResponseCodeEnum codeEnum) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(codeEnum.getCode());
        response.setMessage(codeEnum.getMessage());
        response.setData(null);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 失败响应（自定义消息）
    public static <T> ApiResponse<T> error(ResponseCodeEnum codeEnum, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(codeEnum.getCode());
        response.setMessage(message);
        response.setData(null);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // getter/setter 省略
}
```

**`ResponseCodeEnum` 枚举定义**：

```java
/**
 * 响应码枚举
 */
public enum ResponseCodeEnum {
    
    // 成功
    SUCCESS("000000", "操作成功"),
    
    // 客户端错误 4xx
    BAD_REQUEST("400000", "请求参数错误"),
    UNAUTHORIZED("401000", "未授权访问"),
    FORBIDDEN("403000", "无权限访问"),
    NOT_FOUND("404000", "资源不存在"),
    METHOD_NOT_ALLOWED("405000", "请求方法不允许"),
    CONFLICT("409000", "资源冲突"),
    VALIDATION_ERROR("422000", "参数校验失败"),
    
    // 服务端错误 5xx
    INTERNAL_ERROR("500000", "服务器内部错误"),
    SERVICE_UNAVAILABLE("503000", "服务暂不可用"),
    
    // 业务错误 1xxx
    APP_NOT_FOUND("100100", "应用不存在"),
    APP_NAME_DUPLICATED("100101", "应用名称已存在"),
    APP_ACCESS_DENIED("100102", "无权访问该应用"),
    MEMBER_NOT_FOUND("100200", "成员不存在"),
    PERMISSION_DENIED("100300", "权限不足"),
    
    // 第三方服务错误 2xxx
    EXTERNAL_SERVICE_ERROR("200000", "外部服务调用失败"),
    DATABASE_ERROR("200100", "数据库操作失败");
    
    private final String code;
    private final String message;
    
    ResponseCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
```

**异常处理示例**：

```java
// ✅ 正确示例 - 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        logger.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(ResponseCodeEnum.valueOf(e.getCode()), e.getMessage());
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", message);
        return ApiResponse.error(ResponseCodeEnum.VALIDATION_ERROR, message);
    }
    
    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        logger.error("Unexpected exception", e);
        return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
    }
}

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMessage());
        this.code = codeEnum.getCode();
    }
    
    public BusinessException(ResponseCodeEnum codeEnum, String message) {
        super(message);
        this.code = codeEnum.getCode();
    }
    
    public String getCode() {
        return code;
    }
}

// ✅ 正确示例 - Service 层抛出业务异常
@Service
public class AppService {
    
    public AppDetailVO getAppById(String appId) {
        AppDO app = appMapper.selectById(appId);
        if (app == null) {
            throw new BusinessException(ResponseCodeEnum.APP_NOT_FOUND);
        }
        return convertToVO(app);
    }
    
    public String createApp(CreateAppRequest request) {
        // 检查应用名称唯一性
        if (appMapper.existsByName(request.getName())) {
            throw new BusinessException(ResponseCodeEnum.APP_NAME_DUPLICATED);
        }
        
        // 创建应用
        AppDO app = buildApp(request);
        appMapper.insert(app);
        
        return app.getAppId();
    }
}
```

**前端接收格式示例**：

```json
// ✅ 成功响应
{
    "code": "000000",
    "message": "操作成功",
    "data": {
        "appId": "app_123456",
        "name": "测试应用",
        "status": 1
    },
    "timestamp": 1717574400000
}

// ✅ 失败响应
{
    "code": "100100",
    "message": "应用不存在",
    "data": null,
    "timestamp": 1717574400000
}

// ✅ 参数校验失败响应
{
    "code": "422000",
    "message": "应用名称不能为空, 应用类型不能为空",
    "data": null,
    "timestamp": 1717574400000
}
```

**原因**：
1. **统一格式**：前后端交互格式统一，便于前端统一处理
2. **类型安全**：泛型保证类型安全，编译期即可发现类型错误
3. **错误处理**：异常情况也返回统一格式，前端无需特殊处理
4. **可读性强**：响应码和消息清晰，便于前端理解和调试
5. **易于扩展**：新增响应码只需在枚举中添加即可
6. **便于监控**：统一的响应码便于日志分析和监控告警

**注意事项**：
- 所有 Controller 方法必须返回 `ApiResponse<T>`，不允许直接返回业务对象
- 所有异常必须通过全局异常处理器捕获并转换为 `ApiResponse<T>`
- 响应码必须使用 `ResponseCodeEnum`，不允许硬编码字符串
- 新增业务错误码必须在 `ResponseCodeEnum` 中定义，遵循编码规范
- `timestamp` 字段使用 `System.currentTimeMillis()` 获取当前时间戳

---

### （五）注释规约

#### 1. 【强制】类、类属性、类方法的注释必须使用 Javadoc 规范，使用 `/** 内容 */` 格式，不得使用 `// xxx` 方式

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 Javadoc 格式 | 使用单行注释格式 |

```java
// ✅ 正确示例
/**
 * 应用服务类
 * 
 * @author Summer
 * @date 2026-06-05
 */
public class AppService {
    /**
     * 应用 ID
     */
    private String appId;
    
    /**
     * 根据 ID 获取应用详情
     * 
     * @param appId 应用 ID
     * @return 应用详情
     */
    public AppDetail getApp(String appId) {
        // ...
    }
}

// ❌ 错误示例
// 应用服务类
public class AppService {
    // 应用 ID
    private String appId;
    
    // 根据 ID 获取应用详情
    public AppDetail getApp(String appId) {
        // ...
    }
}
```

---

#### 2. 【强制】所有的类都必须添加创建者和创建日期

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 `@author` 和 `@date` | 缺少创建者和创建日期 |

```java
// ✅ 正确示例
/**
 * 应用服务类
 * 
 * @author Summer
 * @date 2026-06-05
 */
public class AppService {
    // ...
}

// ❌ 错误示例
/**
 * 应用服务类
 */
public class AppService {
    // ...
}
```

---

#### 3. 【强制】方法内部单行注释，在被注释语句上方另起一行，使用 `//` 注释

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 注释在被注释语句上方 | 注释在被注释语句后方 |

```java
// ✅ 正确示例
public class AppService {
    public void createApp(CreateAppRequest request) {
        // 参数校验
        validateRequest(request);
        
        // 检查应用名称唯一性
        checkAppNameUniqueness(request.getName());
        
        // 创建应用
        App app = buildApp(request);
        appMapper.insert(app);
    }
}

// ❌ 错误示例
public class AppService {
    public void createApp(CreateAppRequest request) {
        validateRequest(request);        // 参数校验
        checkAppNameUniqueness(request.getName());  // 检查应用名称唯一性
        App app = buildApp(request);     // 创建应用
        appMapper.insert(app);
    }
}
```

---

#### 4. 【强制】所有的枚举类型字段必须要有注释，说明每个数据项的用途

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 每个枚举值都有注释 | 枚举值缺少注释 |

```java
// ✅ 正确示例
public enum AppStatusEnum {
    /**
     * 有效
     */
    ACTIVE(1),
    
    /**
     * 无效
     */
    INACTIVE(0),
    
    /**
     * 已删除
     */
    DELETED(-1);
    
    private final int code;
    
    AppStatusEnum(int code) {
        this.code = code;
    }
}

// ❌ 错误示例
public enum AppStatusEnum {
    ACTIVE(1),      // ❌ 缺少注释
    INACTIVE(0),    // ❌ 缺少注释
    DELETED(-1);    // ❌ 缺少注释
    
    private final int code;
}
```

---

#### 5. 【推荐】与其"半吊子"英文来注释，不如用中文注释把问题说清楚。专有名词与关键字保持英文原文即可

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 中文注释清晰表达 | 英文注释表达不清 |

```java
// ✅ 正确示例
public class AppService {
    /**
     * 创建应用
     * 
     * @param request 创建应用请求
     * @return 应用详情
     */
    public AppDetail createApp(CreateAppRequest request) {
        // 校验应用名称唯一性
        checkAppNameUniqueness(request.getName());
        
        // 创建应用记录
        App app = buildApp(request);
        appMapper.insert(app);
        
        return app;
    }
}

// ❌ 错误示例
public class AppService {
    /**
     * Create application
     */
    public AppDetail createApp(CreateAppRequest request) {
        // Check app name unique        // ❌ 英文注释表达不清
        checkAppNameUniqueness(request.getName());
        
        // Create app record
        App app = buildApp(request);
        appMapper.insert(app);
        
        return app;
    }
}
```

---

#### 6. 【推荐】代码修改的同时，注释也要进行相应的修改，尤其是参数、返回值、异常、核心逻辑等的修改

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 注释与代码同步更新 | 代码修改但注释未更新 |

```java
// ✅ 正确示例
/**
 * 更新应用基本信息
 * 
 * @param appId 应用 ID
 * @param request 更新请求（包含基本信息和描述）
 * @return 更新后的应用详情
 * @throws AppNotFoundException 应用不存在
 */
public AppDetail updateAppBasicInfo(String appId, UpdateAppRequest request) {
    // ...
}

// ❌ 错误示例
/**
 * 更新应用名称
 * 
 * @param appId 应用 ID
 * @param name 应用名称
 * @return 应用详情
 */
public AppDetail updateAppBasicInfo(String appId, UpdateAppRequest request) {
    // ❌ 注释未更新，仍然说的是更新名称，但实际参数已变为 UpdateAppRequest
    // ...
}
```

---

#### 7. 【参考】谨慎注释掉代码。在上方详细说明，而不是简单地注释掉。如果无用，则删除

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 说明注释原因并标注时间 | 直接注释掉代码 |

```java
// ✅ 正确示例
public class AppService {
    public void process() {
        // 正常逻辑
        doSomething();
        
        /// 2026-06-05 暂时禁用旧逻辑，等待新版本上线后删除
        /// oldProcess();
    }
}

// ❌ 错误示例
public class AppService {
    public void process() {
        doSomething();
        
        // oldProcess();    // ❌ 直接注释掉，未说明原因
        // validateRequest();
        // checkPermission();
    }
}
```

**说明**：代码被注释掉有两种可能性：
1. 后续会恢复此段代码逻辑。
2. 永久不用。

前者如果没有备注信息，难以知晓注释动机。后者建议直接删掉（代码仓库保存了历史代码）。

---

#### 8. 【强制】所有业务逻辑必须有注释说明，复杂的业务规则必须说明【业务背景】、【计算逻辑】、【异常处理】

> **目的**：业务代码是团队协作的产物，注释能帮助其他开发者快速理解业务意图，避免误改。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 业务逻辑有详细注释说明意图 | 只有代码没有注释，其他人看不懂 |

**必须写业务注释的场景**：

| 场景 | 注释内容 |
|------|----------|
| **业务规则判断** | 为什么这样判断、依据是什么规则/法 |
| **状态流转** | 当前状态是什么、下一步是什么、触发条件 |
| **计算公式** | 公式含义、字段来源、特殊场景处理 |
| **权限校验** | 哪个角色有什么权限、为什么需要校验 |
| **第三方对接** | 对接的哪个系统、协议是什么、字段含义 |
| **异步/定时任务** | 什么时候执行、执行什么、失败怎么处理 |
| **魔法数字** | 数字代表什么业务含义（应同时抽取为常量） |
| **try-catch 异常处理** | 为什么要捕获、捕获后怎么处理、为什么吞掉异常 |

**代码示例**：

```java
// ✅ 正确示例 - 业务规则有详细注释
/**
 * 校验应用转移 Owner 的合法性
 * 
 * 业务规则：
 * 1. 只有当前 Owner 才能发起转移（权限校验）
 * 2. 目标用户必须是当前应用的成员（不能转让给非成员）
 * 3. 目标用户不能是当前 Owner 自身（不能转给自己）
 * 4. 转移后原 Owner 自动降级为管理员（保持应用有管理员）
 *
 * @param appId    应用 ID
 * @param fromUid  原 Owner ID
 * @param toUid    目标用户 ID
 * @throws BusinessException 业务校验失败时抛出
 */
private void validateTransferOwner(String appId, String fromUid, String toUid) {
    // 校验当前用户是否为 Owner（权限校验，避免越权操作）
    AppMember currentOwner = getCurrentOwner(appId);
    BusinessException.assertEquals(currentOwner.getUserId(), fromUid,
        "PERMISSION_DENIED", "只有当前 Owner 才能发起转移");

    // 校验目标用户必须是当前应用的成员（业务规则：不能转让给外部用户）
    AppMember targetMember = appMemberMapper.selectByAppAndUser(appId, toUid);
    BusinessException.assertNotNull(targetMember,
        "TARGET_NOT_MEMBER", "目标用户不是应用成员");

    // 校验不能转给自己（业务规则：避免无意义操作）
    BusinessException.assertNotEquals(toUid, fromUid,
        "SAME_USER", "Owner 不能转给自己");

    // 校验完成后执行转移：原 Owner 降级为管理员，目标用户升级为 Owner
}
```

```java
// ✅ 正确示例 - 状态流转有注释
/**
 * 提交版本发布申请
 * 
 * 状态流转：DRAFT -> PENDING_REVIEW
 * 触发条件：草稿状态 + 所有必填字段已填写
 * 后续流程：进入审批队列，审批人审批后变为 APPROVED 或 REJECTED
 */
public void submitForReview(String versionId) {
    Version version = versionMapper.selectById(versionId);
    
    // 只有 DRAFT 状态可以提交（业务规则：已提交的版本不能重复提交）
    BusinessException.assertEquals(version.getStatus(), VersionStatus.DRAFT,
        "INVALID_STATUS", "只有草稿状态可以提交");
    
    // 业务规则：必填字段必须全部填写（版本号、发布说明、变更日志）
    validateRequiredFields(version);
    
    version.setStatus(VersionStatus.PENDING_REVIEW);
    version.setSubmitTime(new Date());
    versionMapper.update(version);
}
```

```java
// ✅ 正确示例 - 计算公式有注释
/**
 * 计算应用评分
 * 
 * 评分公式：总分 = 文档完整度(30%) + 代码质量(40%) + 用户反馈(30%)
 *  - 文档完整度 = 已填写字段数 / 总必填字段数 * 100
 *  - 代码质量 = (单元测试覆盖率 * 50% + 代码审查通过率 * 50%) * 100
 *  - 用户反馈 = (好评数 / 总评价数) * 100
 * 
 * @param appId 应用 ID
 * @return 评分（0-100）
 */
private double calculateAppScore(String appId) {
    // 文档完整度（权重 30%）
    double docScore = calculateDocCompleteness(appId);
    
    // 代码质量（权重 40%）
    double codeScore = calculateCodeQuality(appId);
    
    // 用户反馈（权重 30%）
    double feedbackScore = calculateUserFeedback(appId);
    
    // 加权平均计算
    return docScore * 0.3 + codeScore * 0.4 + feedbackScore * 0.3;
}
```

```java
// ✅ 正确示例 - 异常处理有注释
try {
    eamapService.unbind(eamapAppCode);
} catch (EamapServiceException e) {
    // EAMAP 解绑失败不回滚（业务规则：解绑失败不影响主流程，记录日志后继续）
    log.warn("[UNBIND_EAMAP] 解绑EAMAP失败但不影响主流程, appId={}, eamapAppCode={}, error={}",
        appId, eamapAppCode, e.getMessage());
}

// ❌ 错误示例 - 业务逻辑没有注释
private void validateTransferOwner(String appId, String fromUid, String toUid) {
    AppMember currentOwner = getCurrentOwner(appId);
    BusinessException.assertEquals(currentOwner.getUserId(), fromUid,
        "PERMISSION_DENIED", "只有当前 Owner 才能发起转移");
    
    AppMember targetMember = appMemberMapper.selectByAppAndUser(appId, toUid);
    BusinessException.assertNotNull(targetMember,
        "TARGET_NOT_MEMBER", "目标用户不是应用成员");
    
    BusinessException.assertNotEquals(toUid, fromUid,
        "SAME_USER", "Owner 不能转给自己");
}
```

**业务注释模板**（推荐格式）：

```java
/**
 * [方法名]
 *
 * 业务背景：[为什么需要这个方法，解决什么业务问题]
 * 处理逻辑：[主要步骤说明]
 * 业务规则：[关键业务规则、限制条件]
 * 数据来源：[数据从哪里来，比如数据库表/第三方接口]
 * 异常处理：[异常情况如何处理]
 *
 * @param xxx 参数说明
 * @return 返回值说明
 */
```

**原因**：
1. **团队协作**：其他开发者能快速理解业务意图，减少沟通成本
2. **避免误改**：有注释的代码不容易被误改或误删
3. **新成员上手**：新人通过注释就能理解业务，不需要反复问人
4. **需求变更**：业务规则变更时，注释能帮助快速定位需要修改的地方
5. **审计追溯**：业务规则的注释是审计追溯的重要依据

**注意事项**：
- 业务注释**不是简单复述代码**，要说明【为什么】而不是【做什么】
- 注释要**随着代码同步更新**，代码改了注释也要改
- 业务规则的来源（如法务要求、合规要求）也要写清楚
- 不要在注释里写"以后优化"、"临时方案"这类不负责任的话
- 注释的格式要统一，推荐使用"业务背景/处理逻辑/业务规则/异常处理"模板

---

#### 9. 【强制】所有实体类（DO/BO/DTO/VO/AO/PO）必须有完整的类注释和字段注释

> **目的**：实体类是数据的载体，字段含义、数据来源、业务约束必须清晰，否则后人对字段的理解会有歧义。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 每个字段都有注释说明含义和约束 | 实体类没有注释，字段含义靠猜 |

**必须写注释的字段**：

| 字段类型 | 注释内容 |
|----------|----------|
| **所有业务字段** | 字段含义、取值范围、默认值 |
| **枚举/状态字段** | 每个取值代表什么状态、流转规则 |
| **外键字段** | 关联哪个表/哪个实体 |
| **金额/数量字段** | 单位、精度、是否可为负 |
| **时间字段** | 时区、精度（秒/毫秒）、是否可为空 |
| **加密字段** | 加密方式、是否可解密 |
| **业务唯一字段** | 是否唯一、生成规则 |

**代码示例**：

```java
// ✅ 正确示例 - 完整注释的 DO 实体类
/**
 * 应用 DO（数据库实体）
 *
 * 对应数据库表：app
 *
 * @author Summer
 * @date 2026-06-05
 */
@Data
@TableName("app")
public class AppDO {

    /** 应用 ID（业务唯一，系统自动生成，前缀 app_） */
    @TableId(type = IdType.ASSIGN_ID)
    private String appId;

    /** 中文名称（必填，≤255 字符，全局唯一） */
    private String nameCn;

    /** 英文名称（必填，≤255 字符，全局唯一，格式：数字+字母+下划线） */
    private String nameEn;

    /** 中文描述（可选，≤2000 字符） */
    private String descCn;

    /** 英文描述（可选，≤2000 字符） */
    private String descEn;

    /**
     * 应用图标 URL
     * - 必填
     * - 格式：HTTP URL
     * - 尺寸：128×128px
     * - 大小：≤100KB
     */
    private String icon;

    /**
     * 功能示意图 URL 列表
     * - 可选
     * - 格式：JSON 数组，每个元素是 HTTP URL
     * - 尺寸：360×200px
     * - 大小：每张 ≤500KB
     */
    private String diagramUrls;

    /**
     * 应用类型
     * @see AppType
     * 0-个人应用 1-业务应用
     */
    private Integer appType;

    /**
     * 应用子类型
     * @see AppSubType
     * 0-存量个人应用（可升级为业务应用）
     * 4-业务应用-标准
     */
    private Integer appSubType;

    /**
     * 应用状态
     * @see AppStatus
     * 0-失效 1-有效
     * 失效的应用不在应用列表展示
     */
    private Integer status;

    /** EAMAP 编码（可选，未绑定 EAMAP 时为空） */
    private String eamapAppCode;

    /** 是否已绑定 EAMAP（true-已绑定 false-未绑定，与 eamapAppCode 联动） */
    private Boolean eamapBound;

    /** 创建人用户 ID（应用的所有者，自动成为 Owner） */
    private String creatorId;

    /** 创建时间（精确到秒，UTC+8） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 更新时间（精确到秒，UTC+8，每次更新自动刷新） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
```

```java
// ✅ 正确示例 - DTO（请求对象）
/**
 * 创建应用请求 DTO
 *
 * @author Summer
 * @date 2026-06-05
 */
@Data
public class CreateAppRequest {

    /** 中文名称（必填，2-50 字符，全局唯一） */
    @NotBlank(message = "中文名称不能为空")
    @Size(min = 2, max = 50, message = "中文名称长度必须在 2-50 之间")
    private String nameCn;

    /** 英文名称（必填，2-50 字符，数字+字母+下划线，全局唯一） */
    @NotBlank(message = "英文名称不能为空")
    @Size(min = 2, max = 50, message = "英文名称长度必须在 2-50 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "英文名称只能包含数字、字母、下划线")
    private String nameEn;

    /** 中文描述（可选，0-500 字符） */
    @Size(max = 500, message = "中文描述不能超过 500 字符")
    private String descCn;

    /** 英文描述（可选，0-500 字符） */
    @Size(max = 500, message = "英文描述不能超过 500 字符")
    private String descEn;

    /** 应用图标 URL（必填，HTTP URL，128×128px，≤100KB） */
    @NotBlank(message = "应用图标不能为空")
    @Pattern(regexp = "^https?://.*", message = "应用图标必须是 HTTP URL")
    private String icon;

    /** EAMAP 编码（必填，应用创建后自动绑定） */
    @NotBlank(message = "EAMAP 编码不能为空")
    private String eamapAppCode;
}
```

```java
// ✅ 正确示例 - VO（返回值对象）
/**
 * 应用详情 VO
 *
 * @author Summer
 * @date 2026-06-05
 */
@Data
public class AppDetailVO {

    /** 应用 ID */
    private String appId;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 应用类型名称（已转中文，PA-个人应用 / BA-业务应用） */
    private String appTypeText;

    /** 当前用户角色（OWNER / ADMIN / DEVELOPER） */
    private String currentUserRole;

    /** EAMAP 绑定状态（true-已绑定 false-未绑定） */
    private Boolean eamapBound;

    /** EAMAP 名称（已绑定时返回，未绑定时返回 null） */
    private String eamapAppName;

    /** 最后更新时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String lastUpdateTime;
}
```

```java
// ❌ 错误示例 - 实体类没有字段注释
@Data
@TableName("app")
public class AppDO {

    private String appId;
    private String nameCn;
    private String nameEn;
    private String descCn;
    private String descEn;
    private String icon;
    private Integer appType;
    private Integer appSubType;
    private Integer status;
    private String eamapAppCode;
    private Date createTime;
    private Date updateTime;
}
```

**枚举类的注释**：

```java
// ✅ 正确示例
/**
 * 应用类型枚举
 *
 * @author Summer
 * @date 2026-06-05
 */
public enum AppType {

    /** 个人应用（0） */
    PERSONAL(0, "个人应用"),

    /** 业务应用（1） */
    BUSINESS(1, "业务应用");

    @EnumValue
    private final Integer code;
    private final String desc;

    AppType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

**原因**：
1. **可读性**：实体类是数据的"契约"，字段含义必须清晰
2. **避免歧义**：同名字段在不同业务上下文含义可能不同（如 `status` 在订单和在用户中含义不同）
3. **约束可见**：字段的长度、必填、格式等约束写在注释里，新人不用看代码也能知道
4. **数据迁移**：字段含义清楚，数据迁移和对接第三方时不容易出错
5. **API 文档**：实体类的注释就是最好的 API 文档

**注意事项**：
- 实体类的字段注释**必须包含业务含义**，不要只写"ID"、"名称"这种废话
- 字段约束（必填、长度、格式）**写在注释里**，同时也要在 DTO 上用 `@NotBlank`、`@Size` 等注解标注
- 枚举值必须有注释说明每个值代表什么
- 关联字段要说明关联的实体或表
- 时间字段要说明时区和精度
- 加密字段要说明加密方式

---

### （六）控制语句

#### 1. 【强制】在一个 switch 块内，每个 case 要么通过 break/return 等来终止，要么注释说明程序将继续执行到哪一个 case 为止；在一个 switch 块内，都必须包含一个 default 语句并且放在最后，即使空代码

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 default 分支 | 缺少 default 分支 |

```java
// ✅ 正确示例
public String getStatusText(Integer status) {
    switch (status) {
        case 0:
            return "待审批";
        case 1:
            return "已通过";
        case 2:
            return "已驳回";
        case 3:
            return "已撤销";
        default:
            log.warn("Unknown status: {}", status);
            return "未知状态";
    }
}

// ❌ 错误示例
public String getStatusText(Integer status) {
    switch (status) {
        case 0:
            return "待审批";
        case 1:
            return "已通过";
        case 2:
            return "已驳回";
        case 3:
            return "已撤销";
        // ❌ 缺少 default 分支
    }
    return "未知状态";  // 应该在 default 中处理
}
```

---

#### 2. 【强制】在 if/else/for/while/do 语句中必须使用大括号

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用大括号包裹代码块 | 省略大括号（即使只有一行） |

```java
// ✅ 正确示例
public class AppService {
    public void process(Integer status) {
        if (status == null) {
            return;
        }
        
        if (status > 0) {
            log.info("Status is positive: {}", status);
        }
        
        for (String item : items) {
            processItem(item);
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void process(Integer status) {
        if (status == null) return;             // ❌ 省略大括号
        
        if (status > 0) log.info("Status: {}", status);  // ❌ 省略大括号
        
        for (String item : items) processItem(item);     // ❌ 省略大括号
    }
}
```

**原因**：避免"苹果公司 SSL/TLS 重大漏洞"类问题（因缺少括号导致的逻辑错误）。

---

#### 3. 【推荐】表达异常的分支时，少用 if-else 方式，这种方式可以改写成

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 早返回减少嵌套 | 多层 if-else 嵌套 |

```java
// ✅ 正确示例
public void process(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("订单不能为空");
    }
    
    if (!order.isValid()) {
        log.warn("订单无效，orderId={}", order.getId());
        return;
    }
    
    // 主逻辑
    processOrder(order);
}

// ❌ 错误示例
public void process(Order order) {
    if (order != null) {
        if (order.isValid()) {
            processOrder(order);
        } else {
            log.warn("订单无效");
        }
    } else {
        throw new IllegalArgumentException("订单不能为空");
    }
}
```

**说明**：如果非得使用 `if()...else if()...else...` 方式表达逻辑，【强制】避免后续代码维护困难，请勿超过 3 层。

---

#### 4. 【推荐】除常用方法（如 getXxx/isXxx）等外，不要在条件判断中执行其它复杂的语句，将复杂逻辑判断的结果赋值给一个有意义的布尔变量名，以提高可读性

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 将复杂判断赋值给变量 | 条件判断中执行复杂逻辑 |

```java
// ✅ 正确示例
public void process(File file) {
    boolean fileExists = (file != null) && file.exists() && file.canRead();
    
    if (fileExists) {
        processFile(file);
    }
}

// ❌ 错误示例
public void process(File file) {
    if ((file != null) && file.exists() && file.canRead()) {  // ❌ 条件判断过于复杂
        processFile(file);
    }
}
```

---

#### 5. 【推荐】循环体中的语句要考量性能，以下操作尽量移至循环体外处理

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 对象定义移至循环体外 | 循环内重复创建对象 |

```java
// ✅ 正确示例
public void process(List<String> items) {
    StringBuilder sb = new StringBuilder();  // 移至循环体外
    Connection conn = getConnection();       // 移至循环体外
    
    for (String item : items) {
        sb.append(item);
    }
    
    closeConnection(conn);
}

// ❌ 错误示例
public void process(List<String> items) {
    for (String item : items) {
        StringBuilder sb = new StringBuilder();  // ❌ 循环内创建对象
        Connection conn = getConnection();       // ❌ 循环内获取连接
        sb.append(item);
        closeConnection(conn);
    }
}
```

---

## 二、异常日志

### （一）异常处理

#### 1. 【强制】Java 类库中定义的可以通过预检查方式规避的 RuntimeException 异常不应该通过 catch 的方式来处理

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用预检查 | 使用 catch 捕获 |

```java
// ✅ 正确示例
public void process(App app) {
    if (app != null) {
        app.doSomething();
    }
}

// ❌ 错误示例
public void process(App app) {
    try {
        app.doSomething();
    } catch (NullPointerException e) {
        // ❌ 不应通过 catch 处理可预检查的异常
    }
}
```

**说明**：无法通过预检查的异常除外，比如，在解析字符串形式的数字时，不得不通过 catch NumberFormatException 来实现。

---

#### 2. 【强制】异常不要用来做流程控制，条件控制

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用条件判断 | 使用异常控制流程 |

```java
// ✅ 正确示例
public App getApp(String appId) {
    App app = appMapper.selectById(appId);
    if (app == null) {
        throw new AppNotFoundException("应用不存在");
    }
    return app;
}

// ❌ 错误示例
public App getApp(String appId) {
    try {
        return appMapper.selectById(appId);
    } catch (NullPointerException e) {
        // ❌ 使用异常做流程控制
        return null;
    }
}
```

**说明**：异常设计的初衷是解决程序运行中的各种意外情况，且异常的处理效率比条件判断方式要低很多。

---

#### 3. 【强制】catch 时请分清稳定代码和非稳定代码，稳定代码指的是无论如何不会出错的代码。对于非稳定代码的 catch 尽可能进行区分异常类型，再做对应的异常处理

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 区分异常类型 | 笼统捕获 Exception |

```java
// ✅ 正确示例
public void process(String filePath) {
    try {
        File file = new File(filePath);
        readFile(file);
    } catch (FileNotFoundException e) {
        log.error("文件不存在: {}", filePath, e);
    } catch (IOException e) {
        log.error("读取文件失败: {}", filePath, e);
    }
}

// ❌ 错误示例
public void process(String filePath) {
    try {
        File file = new File(filePath);
        readFile(file);
    } catch (Exception e) {         // ❌ 笼统捕获所有异常
        log.error("处理失败", e);
    }
}
```

---

#### 4. 【强制】捕获异常是为了处理它，不要捕获了却什么都不处理而抛弃之，如果不想处理它，请将该异常抛给它的调用者。最外层的业务使用者，必须处理异常，将其转化为用户可以理解的内容

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 处理异常或向上抛出 | 空的 catch 块 |

```java
// ✅ 正确示例
public void process(String appId) {
    try {
        doSomething(appId);
    } catch (Exception e) {
        log.error("处理失败，appId={}", appId, e);
        throw new BusinessException("操作失败，请稍后重试", e);
    }
}

// ❌ 错误示例
public void process(String appId) {
    try {
        doSomething(appId);
    } catch (Exception e) {
        // ❌ 空 catch 块，吞掉异常
    }
}
```

---

#### 5. 【强制】有 try 块放到了事务代码中，catch 异常后，如果需要回滚事务，一定要注意手动回滚事务

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 手动回滚事务 | 忘记回滚事务 |

```java
// ✅ 正确示例
@Transactional
public void createApp(CreateAppRequest request) {
    try {
        doSomething();
    } catch (Exception e) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();  // 手动回滚
        throw e;
    }
}

// ❌ 错误示例
@Transactional
public void createApp(CreateAppRequest request) {
    try {
        doSomething();
    } catch (Exception e) {
        // ❌ 忘记回滚事务
        log.error("创建失败", e);
    }
}
```

---

#### 6. 【强制】捕获异常与抛异常，必须是完全匹配，或者捕获异常是抛异常的父类

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 异常类型匹配或为父类 | 异常类型不匹配 |

```java
// ✅ 正确示例
public void process() throws IOException {
    try {
        readFile();
    } catch (IOException e) {        // 完全匹配
        throw e;
    }
}

public void process2() throws Exception {
    try {
        readFile();
    } catch (IOException e) {        // IOException 是 Exception 的子类
        throw new Exception("处理失败", e);
    }
}

// ❌ 错误示例
public void process() throws IOException {
    try {
        readFile();
    } catch (SQLException e) {       // ❌ IOException 和 SQLException 不匹配
        throw e;                     // 编译错误
    }
}
```

---

#### 7. 【推荐】方法的返回值可以为 null，不强制返回空集合，或者空对象等，必须添加注释充分说明什么情况下会返回 null 值

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 注释说明返回 null 的情况 | 返回 null 但未说明 |

```java
// ✅ 正确示例
/**
 * 根据 ID 获取应用详情
 * 
 * @param appId 应用 ID
 * @return 应用详情，如果应用不存在则返回 null
 */
public App getApp(String appId) {
    return appMapper.selectById(appId);
}

// ❌ 错误示例
/**
 * 根据 ID 获取应用详情
 */
public App getApp(String appId) {
    return appMapper.selectById(appId);  // ❌ 未说明可能返回 null
}
```

---

#### 8. 【推荐】防止 NPE，是程序员的基本修养，注意 NPE 产生的场景

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 进行空指针检查 | 未进行空指针检查 |

```java
// ✅ 正确示例
public void process(String appId) {
    App app = appMapper.selectById(appId);
    if (app != null) {
        String name = app.getName();
        if (name != null) {
            log.info("App name: {}", name);
        }
    }
}

// ❌ 错误示例
public void process(String appId) {
    App app = appMapper.selectById(appId);
    String name = app.getName();          // ❌ app 可能为 null
    log.info("App name length: {}", name.length());  // ❌ name 可能为 null
}
```

**NPE 常见场景**：
1. 返回类型为基本数据类型，return 包装数据类型的对象时，自动拆箱有可能产生 NPE。
2. 数据库的查询结果可能为 null。
3. 集合里的元素即使 isNotEmpty，取出的数据元素也可能为 null。
4. 远程调用返回对象时，一律要求进行空指针判断，防止 NPE。
5. 对于 Session 中获取的数据，建议 NPE 检查，避免空指针。
6. 级联调用 `obj.getA().getB().getC()`；一连串调用，易产生 NPE。

---

#### 9. 【推荐】定义时区分 unchecked / checked 异常，避免直接抛出 new RuntimeException()，更不允许抛出 Exception 或者 Throwable，应使用有业务含义的自定义异常

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用自定义异常 | 直接抛出 RuntimeException |

```java
// ✅ 正确示例
public class AppNotFoundException extends RuntimeException {
    public AppNotFoundException(String message) {
        super(message);
    }
}

public class AppService {
    public App getApp(String appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new AppNotFoundException("应用不存在: " + appId);
        }
        return app;
    }
}

// ❌ 错误示例
public class AppService {
    public App getApp(String appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new RuntimeException("应用不存在");  // ❌ 直接抛出 RuntimeException
        }
        return app;
    }
}
```

---

#### 10. 【强制】所有异常必须通过全局异常处理器捕获并转换为 `ApiResponse<T>` 统一格式返回给前端

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 异常转换为统一格式 | 直接抛出异常或返回错误页面 |

```java
// ✅ 正确示例 - 全局异常处理器返回统一格式
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        logger.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(ResponseCodeEnum.valueOf(e.getCode()), e.getMessage());
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", message);
        return ApiResponse.error(ResponseCodeEnum.VALIDATION_ERROR, message);
    }
    
    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Illegal argument: {}", e.getMessage());
        return ApiResponse.error(ResponseCodeEnum.BAD_REQUEST, e.getMessage());
    }
    
    /**
     * 处理 NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResponse<Void> handleNullPointerException(NullPointerException e) {
        logger.error("Null pointer exception", e);
        return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR, "系统内部错误，请联系管理员");
    }
    
    /**
     * 处理数据库异常
     */
    @ExceptionHandler(DataAccessException.class)
    public ApiResponse<Void> handleDataAccessException(DataAccessException e) {
        logger.error("Database error", e);
        return ApiResponse.error(ResponseCodeEnum.DATABASE_ERROR);
    }
    
    /**
     * 处理未知异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        logger.error("Unexpected exception", e);
        return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR);
    }
}

// ❌ 错误示例 - Controller 直接抛出异常
@RestController
public class AppController {
    
    @GetMapping("/{appId}")
    public AppDetail getApp(@PathVariable String appId) {
        // ❌ 异常未被捕获，前端收到默认错误页面或 500 错误
        return appService.getAppById(appId);
    }
}

// ❌ 错误示例 - Controller 返回不同格式
@RestController
public class AppController {
    
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e) {  // ❌ 返回 String
        return "Error: " + e.getMessage();
    }
    
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {  // ❌ 返回 Map
        Map<String, Object> result = new HashMap<>();
        result.put("error", e.getMessage());
        return result;
    }
}

// ❌ 错误示例 - 缺少全局异常处理器
// 没有全局异常处理器，所有异常都返回默认的 500 错误页面
```

**异常处理优先级**：

全局异常处理器按照以下优先级处理异常：

1. **业务异常** (`BusinessException`)：记录 WARN 日志，返回业务错误码和消息
2. **参数校验异常** (`MethodArgumentNotValidException`)：提取字段错误信息，返回校验错误
3. **非法参数异常** (`IllegalArgumentException`)：记录 WARN 日志，返回参数错误
4. **空指针异常** (`NullPointerException`)：记录 ERROR 日志，返回内部错误
5. **数据库异常** (`DataAccessException`)：记录 ERROR 日志，返回数据库错误
6. **其他未知异常** (`Exception`)：记录 ERROR 日志，返回内部错误

**异常响应示例**：

```json
// ✅ 业务异常响应
{
    "code": "100100",
    "message": "应用不存在",
    "data": null,
    "timestamp": 1717574400000
}

// ✅ 参数校验异常响应
{
    "code": "422000",
    "message": "应用名称不能为空, 应用类型不能为空",
    "data": null,
    "timestamp": 1717574400000
}

// ✅ 系统异常响应
{
    "code": "500000",
    "message": "服务器内部错误",
    "data": null,
    "timestamp": 1717574400000
}

// ❌ 错误示例 - 未捕获的异常返回默认错误页面
{
    "timestamp": "2024-06-05T10:00:00.000+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/v1/app/app_123"
}
```

**原因**：
1. **统一格式**：所有异常都返回统一格式，前端无需特殊处理
2. **友好提示**：将技术异常转换为用户友好的错误消息
3. **便于调试**：日志记录完整异常堆栈，便于排查问题
4. **安全防护**：隐藏系统内部错误细节，防止敏感信息泄露
5. **监控告警**：统一的错误码便于日志分析和监控告警
6. **用户体验**：前端可以统一展示错误消息，提升用户体验

**注意事项**：
- 必须配置全局异常处理器，捕获所有异常
- 业务异常记录 WARN 日志，系统异常记录 ERROR 日志
- 不要向用户暴露系统内部错误细节（如 SQL 错误、堆栈信息）
- 异常消息要对用户友好，便于理解
- 新增异常类型时，需要在全局异常处理器中添加对应的处理方法
- 异常处理方法必须返回 `ApiResponse<T>` 统一格式

---

### （二）日志规约

#### 1. 【强制】应用中不可直接使用日志系统（Log4j、Logback）中的 API，而应依赖使用日志框架 SLF4J 中的 API，使用门面模式的日志框架，有利于维护和各个类的日志处理方式统一

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 SLF4J | 使用 Log4j/Logback 直接调用 |

```java
// ✅ 正确示例
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppService {
    private static final Logger logger = LoggerFactory.getLogger(AppService.class);
    
    public void process() {
        logger.info("Processing app");
    }
}

// ❌ 错误示例
import org.apache.log4j.Logger;

public class AppService {
    private Logger logger = Logger.getLogger(AppService.class);  // ❌ 使用 Log4j
    
    public void process() {
        logger.info("Processing app");
    }
}
```

---

#### 2. 【强制】日志文件推荐至少保存 15 天，因为有些异常具备以"周"为频次发生的特点

**配置示例**（Logback）：

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>15</maxHistory>  <!-- 保留 15 天 -->
    </rollingPolicy>
</appender>
```

---

#### 3. 【强制】对 trace/debug/info 级别的日志输出，必须使用条件输出形式或者使用占位符的方式

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用条件判断或占位符 | 直接字符串拼接 |

```java
// ✅ 正确示例 - 条件判断
if (logger.isDebugEnabled()) {
    logger.debug("Processing trade with id: " + id + " and symbol: " + symbol);
}

// ✅ 正确示例 - 占位符
logger.debug("Processing trade with id: {} and symbol: {}", id, symbol);

// ❌ 错误示例 - 直接字符串拼接
logger.debug("Processing trade with id: " + id + " and symbol: " + symbol);
```

**说明**：如果日志级别是 warn，上述日志不会打印，但是会执行字符串拼接操作，如果 symbol 是对象，会执行 toString() 方法，浪费了系统资源，执行了上述操作，最终日志却没有打印。

---

#### 4. 【强制】避免重复打印日志，浪费磁盘空间，务必在 log4j.xml 中设置 additivity=false

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 设置 additivity=false | 未设置 additivity |

```xml
<!-- ✅ 正确示例 -->
<logger name="com.xxx.works.app" additivity="false">
    <appender-ref ref="FILE"/>
</logger>

<!-- ❌ 错误示例 -->
<logger name="com.xxx.works.app">
    <appender-ref ref="FILE"/>
</logger>
```

---

#### 5. 【强制】异常信息应该包括两类信息：案发现场信息和异常堆栈信息。如果不处理，那么通过关键字 throws 往上抛出

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含现场信息和堆栈 | 仅打印异常消息 |

```java
// ✅ 正确示例
public void process(String appId) {
    try {
        doSomething(appId);
    } catch (Exception e) {
        log.error("Process failed, appId={}, request={}", appId, request, e);
        throw e;
    }
}

// ❌ 错误示例
public void process(String appId) {
    try {
        doSomething(appId);
    } catch (Exception e) {
        log.error("Process failed: " + e.getMessage());  // ❌ 缺少堆栈信息
    }
}
```

---

#### 6. 【推荐】谨慎地记录日志。生产环境禁止输出 debug 日志；有选择地输出 info 日志；如果使用 warn 来记录刚上线时的业务行为信息，一定要注意日志输出量的问题，避免把服务器磁盘撑爆，并记得及时删除这些观察日志

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 合理控制日志级别 | 生产环境输出 debug 日志 |

```xml
<!-- ✅ 正确示例 - 生产环境配置 -->
<root level="INFO">
    <appender-ref ref="FILE"/>
</root>

<!-- ❌ 错误示例 - 生产环境配置 -->
<root level="DEBUG">
    <appender-ref ref="FILE"/>
</root>
```

---

#### 7. 【推荐】可以使用 warn 日志级别来记录用户输入参数错误的情况，避免投诉时无所适从。如非必要，请不在此场景打出 error 级别，避免频繁报警

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 warn 记录参数错误 | 使用 error 记录参数错误 |

```java
// ✅ 正确示例
public void process(CreateAppRequest request) {
    if (request.getName() == null || request.getName().isEmpty()) {
        logger.warn("Invalid request, name is empty, userId={}", getCurrentUserId());
        throw new IllegalArgumentException("应用名称不能为空");
    }
}

// ❌ 错误示例
public void process(CreateAppRequest request) {
    if (request.getName() == null || request.getName().isEmpty()) {
        logger.error("Invalid request, name is empty");  // ❌ 用户输入错误不应使用 error
        throw new IllegalArgumentException("应用名称不能为空");
    }
}
```

**说明**：注意日志输出的级别，error 级别只记录系统逻辑出错、异常或者重要的错误信息。

---

#### 8. 【项目特定】日志信息统一使用英文

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `log.info("Permission created, id={}", id)` | `log.info("权限创建成功, id={}", id)` |

```java
// ✅ 正确示例
@Slf4j
@Service
public class PermissionService {
    public void createPermission(Permission permission) {
        log.info("Creating permission, scope={}", permission.getScope());
        permissionRepository.save(permission);
        log.info("Permission created successfully, id={}", permission.getId());
    }
}

// ❌ 错误示例
@Slf4j
@Service
public class PermissionService {
    public void createPermission(Permission permission) {
        log.info("创建权限, scope={}", permission.getScope());
        permissionRepository.save(permission);
        log.info("权限创建成功, id={}", permission.getId());
    }
}
```

---

#### 9. 【项目特定】日志中禁止打印敏感信息，如直接打印请求头、Token、密码等

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 过滤敏感信息后打印日志 | 直接打印包含敏感信息的对象 |

```java
// ✅ 正确示例
@Slf4j
@RestController
public class ApiController {
    
    public void handleRequest(HttpServletRequest request) {
        // 只记录必要的非敏感信息
        log.info("Request received, uri={}, method={}", 
            request.getRequestURI(), request.getMethod());
        
        String token = request.getHeader("Authorization");
        if (token != null) {
            log.info("Authorization header present, length={}", token.length());
        }
    }
    
    public void login(LoginRequest request) {
        // 脱敏处理后记录
        log.info("User login attempt, username={}", request.getUsername());
        // 不记录密码
    }
}

// ❌ 错误示例
@Slf4j
@RestController
public class ApiController {
    
    public void handleRequest(HttpServletRequest request) {
        // ❌ 直接打印请求头，可能包含敏感信息
        log.info("Request headers: {}", request.getHeaderNames());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            // ❌ 打印所有请求头，包括 Authorization
            log.info("Header: {}={}", name, request.getHeader(name));
        }
    }
    
    public void login(LoginRequest request) {
        // ❌ 直接打印包含密码的对象
        log.info("Login request: {}", request);
    }
}
```

**敏感信息类型**：
- 认证信息：Token、Session ID、Cookie
- 个人信息：密码、身份证号、手机号、邮箱
- 支付信息：银行卡号、CVV、支付密码
- 业务敏感信息：API Key、私钥、加密密钥

---

#### 10. 【强制】所有请求入口和出口必须添加日志，记录请求参数、响应结果和执行时间

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 入口出口都有日志，记录完整信息 | 缺少入口或出口日志 |

```java
// ✅ 正确示例 - 方式一：手动添加日志（推荐用于关键业务接口）
@Slf4j
@RestController
@RequestMapping("/api/v1/app")
public class AppController {
    
    @Autowired
    private AppService appService;
    
    /**
     * 创建应用
     */
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody @Valid CreateAppRequest request) {
        long startTime = System.currentTimeMillis();
        
        // 入口日志：记录请求参数
        log.info("[CREATE_APP] Request received, request={}", request);
        
        try {
            // 执行业务逻辑
            String appId = appService.createApp(request);
            
            // 出口日志：记录响应结果和执行时间
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[CREATE_APP] Request completed, appId={}, elapsed={}ms", appId, elapsed);
            
            return ApiResponse.success(appId);
        } catch (Exception e) {
            // 异常出口日志：记录异常和执行时间
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[CREATE_APP] Request failed, elapsed={}ms, error={}", elapsed, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 根据 ID 获取应用详情
     */
    @GetMapping("/{appId}")
    public ApiResponse<AppDetailVO> getApp(@PathVariable String appId) {
        long startTime = System.currentTimeMillis();
        
        // 入口日志
        log.info("[GET_APP] Request received, appId={}", appId);
        
        try {
            AppDetailVO appDetail = appService.getAppById(appId);
            
            // 出口日志
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[GET_APP] Request completed, appId={}, elapsed={}ms", appId, elapsed);
            
            return ApiResponse.success(appDetail);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[GET_APP] Request failed, appId={}, elapsed={}ms", appId, elapsed, e);
            throw e;
        }
    }
    
    /**
     * 更新应用基本信息
     */
    @PutMapping("/{appId}/basic-info")
    public ApiResponse<Void> updateBasicInfo(
            @PathVariable String appId,
            @RequestBody @Valid UpdateAppRequest request) {
        long startTime = System.currentTimeMillis();
        
        // 入口日志
        log.info("[UPDATE_BASIC_INFO] Request received, appId={}, request={}", appId, request);
        
        try {
            appService.updateBasicInfo(appId, request);
            
            // 出口日志
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[UPDATE_BASIC_INFO] Request completed, appId={}, elapsed={}ms", appId, elapsed);
            
            return ApiResponse.success();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[UPDATE_BASIC_INFO] Request failed, appId={}, elapsed={}ms", appId, elapsed, e);
            throw e;
        }
    }
}

// ❌ 错误示例 - 缺少入口日志
@Slf4j
@RestController
public class AppController {
    
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
        String appId = appService.createApp(request);
        
        log.info("App created, appId={}", appId);  // ❌ 只有出口日志，缺少入口日志
        
        return ApiResponse.success(appId);
    }
}

// ❌ 错误示例 - 缺少出口日志
@Slf4j
@RestController
public class AppController {
    
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
        log.info("Creating app, request={}", request);  // ❌ 只有入口日志，缺少出口日志
        
        String appId = appService.createApp(request);
        return ApiResponse.success(appId);
    }
}

// ❌ 错误示例 - 缺少执行时间
@Slf4j
@RestController
public class AppController {
    
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
        log.info("Creating app, request={}", request);  // ❌ 缺少开始时间记录
        
        String appId = appService.createApp(request);
        
        log.info("App created, appId={}", appId);  // ❌ 没有记录执行时间
        
        return ApiResponse.success(appId);
    }
}

// ❌ 错误示例 - 异常情况没有日志
@Slf4j
@RestController
public class AppController {
    
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
        log.info("Creating app, request={}", request);
        
        String appId = appService.createApp(request);  // ❌ 异常时没有日志记录
        
        log.info("App created, appId={}", appId);
        return ApiResponse.success(appId);
    }
}
```

**日志格式规范**：

| 字段 | 必填 | 说明 | 示例 |
|------|:----:|------|------|
| `[METHOD_NAME]` | ✅ | 方法标识，便于日志搜索 | `[CREATE_APP]` |
| `args` | ✅ | 请求参数（脱敏后） | `{name=测试应用}` |
| `result` | ✅ | 响应结果 | `{code=000000, data=app_123}` |
| `elapsed` | ✅ | 执行时间（毫秒） | `elapsed=150ms` |
| `error` | ❌ | 异常消息（失败时） | `error=应用不存在` |

**日志级别要求**：

| 场景 | 日志级别 | 说明 |
|------|:--------:|------|
| 入口日志 | INFO | 记录请求参数 |
| 出口日志（成功） | INFO | 记录响应结果和执行时间 |
| 出口日志（异常） | ERROR | 记录异常信息和执行时间 |
| 执行时间超长（>1s） | WARN | 记录慢请求告警 |

**慢请求告警示例**：

```java
// 出口日志添加慢请求告警
long elapsed = System.currentTimeMillis() - startTime;

if (elapsed > 1000) {
    // 慢请求告警
    log.warn("[CREATE_APP] Slow request detected, elapsed={}ms, appId={}", elapsed, appId);
} else {
    log.info("[CREATE_APP] Request completed, appId={}, elapsed={}ms", appId, elapsed);
}
```

**日志脱敏要求**：

```java
// ✅ 正确示例 - 敏感信息脱敏
@PostMapping("/login")
public ApiResponse<String> login(@RequestBody LoginRequest request) {
    long startTime = System.currentTimeMillis();
    
    // 入口日志：密码脱敏
    log.info("[LOGIN] Request received, username={}, password=******", request.getUsername());
    
    try {
        String token = authService.login(request);
        
        // 出口日志：Token 脱敏（只记录长度）
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[LOGIN] Request completed, tokenLength={}, elapsed={}ms", token.length(), elapsed);
        
        return ApiResponse.success(token);
    } catch (Exception e) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.error("[LOGIN] Request failed, username={}, elapsed={}ms", 
            request.getUsername(), elapsed, e);
        throw e;
    }
}
```

**原因**：
1. **问题排查**：完整的入口出口日志便于快速定位问题
2. **性能监控**：记录执行时间，便于发现性能瓶颈
3. **审计追踪**：记录完整的请求响应链路，便于审计
4. **统计分析**：日志数据可用于统计分析（如接口调用量、成功率）
5. **告警监控**：慢请求告警，及时发现系统异常
6. **用户体验**：问题发生时能快速响应和修复

**注意事项**：
- 入口日志必须记录请求参数（敏感信息需脱敏）
- 出口日志必须记录响应结果和执行时间
- 异常情况也必须有出口日志，记录异常信息
- 使用统一的方法标识（如 `[METHOD_NAME]`）便于日志搜索
- 执行时间超过 1 秒的请求应记录 WARN 级别日志
- 生产环境应配置日志输出到文件，便于问题排查

---

#### 11. 【强制】所有 Controller 接口入口必须打日志，入参实体类使用 JSON 格式打印

> **目的**：完整记录每次请求的入参，便于问题排查和数据追溯。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 每个接口入口都打日志，实体类用 JSON | 遗漏入口日志、实体类用 toString |

**规范要点**：

1. **每个 Controller 接口方法的第一行**（参数校验之前）必须打印入口日志
2. **入参为实体类 / DTO / Request 对象**时，必须使用 JSON 格式打印（不要直接 `toString()`）
3. **入参为简单类型**（String、Long 等）可直接打印
4. **敏感字段**（密码、Token、AK/SK）必须脱敏为 `******`
5. **日志格式**统一使用 `[METHOD_NAME] [REQUEST] argName={}` 模式

**代码示例**：

```java
// ✅ 正确示例 - 实体类用 JSON 打印
@Slf4j
@RestController
@RequestMapping("/api/v1/app")
public class AppController {

    /** 创建应用 */
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody @Valid CreateAppRequest request) {
        log.info("[CREATE_APP] [REQUEST] request={}", JsonUtils.toJson(request));
        String appId = appService.createApp(request);
        return ApiResponse.success(appId);
    }

    /** 获取应用详情 - 简单类型直接打印 */
    @GetMapping("/{appId}")
    public ApiResponse<AppDetailVO> getApp(@PathVariable String appId) {
        log.info("[GET_APP] [REQUEST] appId={}", appId);
        AppDetailVO appDetail = appService.getAppById(appId);
        return ApiResponse.success(appDetail);
    }

    /** 多参数场景 - 每个参数都打印 */
    @PutMapping("/{appId}/basic-info")
    public ApiResponse<Void> updateBasicInfo(
            @PathVariable String appId,
            @RequestBody @Valid UpdateAppRequest request) {
        log.info("[UPDATE_BASIC_INFO] [REQUEST] appId={}, request={}",
            appId, JsonUtils.toJson(request));
        appService.updateBasicInfo(appId, request);
        return ApiResponse.success();
    }
}

// ❌ 错误示例 1 - 缺少入口日志
@PostMapping
public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
    // ❌ 没有入口日志，问题无法排查
    String appId = appService.createApp(request);
    return ApiResponse.success(appId);
}

// ❌ 错误示例 2 - 实体类用 toString()，不是 JSON
@PostMapping
public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
    log.info("[CREATE_APP] [REQUEST] request={}", request.toString());  // ❌ 应使用 JSON
    String appId = appService.createApp(request);
    return ApiResponse.success(appId);
}

// ❌ 错误示例 3 - 实体类用占位符 {}，会调用 toString()
@PostMapping
public ApiResponse<String> createApp(@RequestBody CreateAppRequest request) {
    log.info("[CREATE_APP] [REQUEST] request={}", request);  // ❌ 实体类应用 JsonUtils.toJson
    String appId = appService.createApp(request);
    return ApiResponse.success(appId);
}

// ❌ 错误示例 4 - 敏感字段未脱敏
@PostMapping("/login")
public ApiResponse<String> login(@RequestBody LoginRequest request) {
    log.info("[LOGIN] [REQUEST] request={}", JsonUtils.toJson(request));  // ❌ 密码会明文打印
    String token = authService.login(request);
    return ApiResponse.success(token);
}
```

**JsonUtils 工具类**：

```java
// ✅ 正确示例
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);  // 忽略 null 字段

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}
```

**敏感字段脱敏规则**（必须脱敏为 `******`）：

| 字段名 | 说明 |
|--------|------|
| `password` / `pwd` / `oldPassword` / `newPassword` | 密码 |
| `token` / `accessToken` / `refreshToken` | 令牌 |
| `ak` / `sk` / `appKey` / `appSecret` / `apiSecret` | 凭证 |
| `cookie` / `setCookie` | Cookie |

**正确脱敏示例**：

```java
// ✅ 正确示例 - 密码脱敏
@PostMapping("/login")
public ApiResponse<String> login(@RequestBody LoginRequest request) {
    log.info("[LOGIN] [REQUEST] username={}, password=******",
        request.getUsername());
    String token = authService.login(request);
    return ApiResponse.success(token);
}

// ✅ 正确示例 - Token 脱敏（只记录长度）
@PostMapping("/refresh-token")
public ApiResponse<String> refreshToken(@RequestBody RefreshTokenRequest request) {
    log.info("[REFRESH_TOKEN] [REQUEST] tokenLength={}",
        request.getToken() != null ? request.getToken().length() : 0);
    String newToken = authService.refreshToken(request);
    log.info("[REFRESH_TOKEN] [RESPONSE] newTokenLength={}",
        newToken.length());
    return ApiResponse.success(newToken);
}
```

**实施步骤**：

| 步骤 | 操作 | 责任方 |
|------|------|--------|
| 1 | 创建 `JsonUtils` 工具类（全局共享） | 后端 |
| 2 | 在每个 Controller 方法第一行添加 `log.info("[METHOD] [REQUEST] ...")` | 后端 |
| 3 | 实体类入参用 `JsonUtils.toJson()` 序列化 | 后端 |
| 4 | 敏感字段脱敏为 `******` | 后端 |
| 5 | 验证：所有接口都有入口日志 | Code Review |

**原因**：
1. **问题排查**：完整的入参日志便于快速定位问题（谁、什么时候、传了什么参数）
2. **数据追溯**：入参是审计的重要依据，便于事后追溯
3. **统一格式**：所有接口的入参日志格式一致，便于日志搜索和统计
4. **避免 toString 坑**：实体类默认 `toString()` 输出格式不可控，且可能漏字段或泄露内部信息

**注意事项**：
- 入口日志必须在**方法体的第一行**（参数校验之前），保证所有请求都被记录
- 实体类入参**必须**用 `JsonUtils.toJson()`，禁止用 `toString()` 或直接 `{}` 占位符
- 敏感字段（密码、Token、AK/SK）**必须**脱敏为 `******`，不允许明文出现在日志中
- 日志的 `argName` 要有意义（如 `request`、`appId`），不要用 `arg0`、`arg1`
- 打印的 JSON 要**忽略 null 字段**（使用 `JsonInclude.Include.NON_NULL`），减少日志噪音
- 入参很大的列表（如分页查询）可以截断，只打印列表大小和前几条
- 文件上传接口（`MultipartFile`）只打印文件名和大小，不打印文件内容

---

## 三、MySQL 数据库

### （一）建表规约

#### 1. 【强制】表达是与否概念的字段，必须使用 is_xxx 的方式命名，数据类型是 unsigned tinyint（1 表示是，0 表示否）

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `is_deleted` / `is_bound` / `is_active` | `deleted` / `bound` / `active` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    `is_deleted` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    `is_bound` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否绑定：0-未绑定，1-已绑定',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    `deleted` tinyint(1) NOT NULL DEFAULT '0',      -- ❌ 缺少 is_ 前缀
    `bound` tinyint(1) NOT NULL DEFAULT '0',        -- ❌ 缺少 is_ 前缀
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：所有列 with 非负值的必须为 unsigned。

---

#### 2. 【强制】表名、字段名必须使用小写字母或数字，禁止出现数字开头，禁止两个下划线中间只出现数字。数据库字段名的修改代价很大，因为无法进行预发布，所以字段名称需要慎重考虑

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `aliyun_admin` / `rdc_config` / `level3_name` | `AliyunAdmin` / `rdcConfig` / `level_3_name` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    `create_time` datetime NOT NULL,
    `level3_name` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `OpenPlatformAppT` (              -- ❌ 大写字母
    `Id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `AppId` varchar(64) NOT NULL,              -- ❌ 大写字母
    `CreateTime` datetime NOT NULL,            -- ❌ 大写字母
    `level_3_name` varchar(255) NOT NULL,      -- ❌ 两个下划线中间只有数字
    PRIMARY KEY (`Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：MySQL 在 Windows 下不区分大小写，但在 Linux 下默认是区分大小写。因此，数据库名、表名、字段名，都不允许出现任何大写字母，避免节外生枝。

---

#### 3. 【强制】表名不使用复数名词

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `app` / `member` / `permission` | `apps` / `members` / `permissions` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_apps_t` (           -- ❌ 使用复数
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：表名应该仅仅表示表里面的实体内容，不应该表示实体数量，对应于 DO 类名也是单数形式，符合表达习惯。

---

#### 4. 【强制】禁用保留字，如 desc、range、match、delayed 等，请参考 MySQL 官方保留字

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `description` / `range_type` / `match_status` | `desc` / `range` / `match` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `description` text COMMENT '描述',          -- 使用 description 替代 desc
    `range_type` int(11) NOT NULL,             -- 使用 range_type 替代 range
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `desc` text,                               -- ❌ 使用保留字
    `range` int(11) NOT NULL,                  -- ❌ 使用保留字
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 5. 【强制】主键索引名为 pk_字段名；唯一索引名为 uk_字段名；普通索引名则为 idx_字段名

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `pk_id` / `uk_app_id` / `idx_create_time` | `PRIMARY` / `app_id_unique` / `create_time_index` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    `create_time` datetime NOT NULL,
    PRIMARY KEY (`id`),                                 -- pk_id
    UNIQUE KEY `uk_app_id` (`app_id`),                  -- uk_app_id
    KEY `idx_create_time` (`create_time`)               -- idx_create_time
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    `create_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `app_id_unique` (`app_id`),              -- ❌ 应使用 uk_app_id
    KEY `create_time_index` (`create_time`)             -- ❌ 应使用 idx_create_time
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：pk_ 即 primary key；uk_ 即 unique key；idx_ 即 index 的简称。

---

#### 6. 【强制】小数类型为 decimal，禁止使用 float 和 double

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `decimal(10,2)` | `float` / `double` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `price` decimal(10,2) NOT NULL COMMENT '价格',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `price` float NOT NULL,                       -- ❌ 使用 float
    `amount` double NOT NULL,                     -- ❌ 使用 double
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：float 和 double 在存储的时候，存在精度损失的问题，很可能在值的比较时，得到不正确的结果。如果存储的数据范围超过 decimal 的范围，建议将数据拆成整数和小数分开存储。

---

#### 7. 【强制】如果存储的字符串长度几乎相等，使用 char 定长字符串类型

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `char(32)` / `char(64)` | `varchar(32)` / `varchar(64)` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `status` char(1) NOT NULL COMMENT '状态：0-待审批，1-已通过，2-已驳回',
    `app_type` char(1) NOT NULL COMMENT '应用类型：0-个人，1-业务',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `status` varchar(1) NOT NULL,               -- ❌ 固定长度应使用 char
    `app_type` varchar(1) NOT NULL,             -- ❌ 固定长度应使用 char
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 8. 【强制】varchar 是可变长字符串，不预先分配存储空间，长度不要超过 5000，如果存储长度大于此值，定义字段类型为 text，独立出来一张表，用主键来对应，避免影响其它字段索引效率

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `varchar(255)` / `text` | `varchar(10000)` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL COMMENT '应用名称',
    `description` text COMMENT '应用描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `description` varchar(10000) NOT NULL,      -- ❌ 超过 5000 应使用 text
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 9. 【强制】表必备三字段：id, create_time, update_time

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 id, create_time, update_time | 缺少必备字段 |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id` varchar(64) NOT NULL COMMENT '应用 ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `openplatform_app_t` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `app_id` varchar(64) NOT NULL,
    -- ❌ 缺少 create_time
    -- ❌ 缺少 update_time
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明**：其中 id 必为主键，类型为 bigint unsigned、单表时自增、步长为 1。create_time, update_time 的类型均为 datetime 类型，前者现在时表示主动式创建，后者过去分词表示被动式更新。

---

#### 10. 【推荐】表的命名最好是加上"业务名称_表的作用"

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `alipay_task` / `force_project` / `trade_config` | `task` / `project` / `config` |

```sql
-- ✅ 正确示例
CREATE TABLE `openplatform_app_t` (             -- openplatform_ 表示业务，app 表示表的作用
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `openplatform_permission_t` (      -- openplatform_ 表示业务，permission 表示表的作用
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误示例
CREATE TABLE `app` (                            -- ❌ 缺少业务前缀
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### （二）SQL 语句

#### 1. 【强制】不要使用 count(列名) 或 count(常量) 来替代 count(*)，count(*) 是 SQL92 定义的标准统计行数的语法，跟数据库无关，跟 NULL 和非 NULL 无关

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `count(*)` | `count(id)` / `count(1)` |

```sql
-- ✅ 正确示例
SELECT count(*) FROM openplatform_app_t WHERE status = 1;

-- ❌ 错误示例
SELECT count(id) FROM openplatform_app_t WHERE status = 1;      -- ❌ 不应使用 count(列名)
SELECT count(1) FROM openplatform_app_t WHERE status = 1;       -- ❌ 不应使用 count(常量)
```

**说明**：count(*) 会统计值为 NULL 的行，而 count(列名) 不会统计此列为 NULL 值的行。

---

#### 2. 【强制】当某一列的值全是 NULL 时，count(col) 的返回结果为 0，但 sum(col) 的返回结果为 NULL，因此使用 sum() 时需注意 NPE 问题

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `SELECT IFNULL(SUM(g),0) FROM table;` | `SELECT SUM(g) FROM table;` |

```sql
-- ✅ 正确示例
SELECT IFNULL(SUM(amount), 0) FROM openplatform_order_t WHERE user_id = 123;

-- ❌ 错误示例
SELECT SUM(amount) FROM openplatform_order_t WHERE user_id = 123;  -- ❌ 可能返回 NULL
```

**说明**：可以使用如下方式来避免 sum 的 NPE 问题：`SELECT IFNULL(SUM(column), 0) FROM table;`

---

#### 3. 【强制】使用 ISNULL() 来判断是否为 NULL 值

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `ISNULL(column)` | `column IS NULL` |

```sql
-- ✅ 正确示例
SELECT * FROM openplatform_app_t WHERE ISNULL(deleted_at);

-- ❌ 错误示例
SELECT * FROM openplatform_app_t WHERE deleted_at IS NULL;      -- ❌ 应使用 ISNULL()
SELECT * FROM openplatform_app_t WHERE deleted_at = NULL;       -- ❌ NULL 不能用 = 比较
```

**说明**：NULL 与任何值的直接比较都为 NULL。NULL<>NULL 的返回结果是 NULL，而不是 false。NULL=NULL 的返回结果是 NULL，而不是 true。NULL<>1 的返回结果是 NULL，而不是 true。

---

#### 4. 【项目特定】禁止使用 SELECT *，必须明确指定字段名

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `SELECT id, name, scope FROM permission` | `SELECT * FROM permission` |

```xml
<!-- ✅ 正确示例 - MyBatis Mapper -->
<select id="findByCategoryId" resultType="Permission">
    SELECT id, name_cn, name_en, scope, status, create_time
    FROM openplatform_permission_t
    WHERE category_id = #{categoryId}
</select>

<!-- ❌ 错误示例 -->
<select id="findByCategoryId" resultType="Permission">
    SELECT *
    FROM openplatform_permission_t
    WHERE category_id = #{categoryId}
</select>
```

**原因**：
- 避免查询不需要的字段，影响性能
- 表结构变更时不会影响现有查询
- 明确字段列表，提高代码可读性

---

#### 5. 【项目特定】模糊查询禁止使用 % 开头，避免索引失效

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `WHERE name LIKE 'api%'` | `WHERE name LIKE '%api%'` |

```xml
<!-- ✅ 正确示例 - 前缀匹配 -->
<select id="searchByName" resultType="Permission">
    SELECT id, name_cn, scope
    FROM openplatform_permission_t
    WHERE name_cn LIKE CONCAT(#{keyword}, '%')
</select>

<!-- ❌ 错误示例 - 全模糊匹配 -->
<select id="searchByName" resultType="Permission">
    SELECT id, name_cn, scope
    FROM openplatform_permission_t
    WHERE name_cn LIKE CONCAT('%', #{keyword}, '%')
</select>
```

**原因**：
- `LIKE '%keyword%'` 无法使用索引，导致全表扫描
- `LIKE 'keyword%'` 可以使用索引（前缀匹配）
- 如需全模糊查询，应使用全文索引或搜索引擎

---

### （三）ORM 映射

#### 1. 【强制】在表查询中，一律不要使用 * 作为查询的字段列表，需要哪些字段必须明确写明

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 明确指定字段名 | 使用 `SELECT *` |

```xml
<!-- ✅ 正确示例 -->
<select id="selectById" resultType="AppDO">
    SELECT id, app_id, name, status, create_time, update_time
    FROM openplatform_app_t
    WHERE id = #{id}
</select>

<!-- ❌ 错误示例 -->
<select id="selectById" resultType="AppDO">
    SELECT *
    FROM openplatform_app_t
    WHERE id = #{id}
</select>
```

**说明**：
1. 增加查询分析器解析成本。
2. 增减字段容易与 resultMap 配置不一致。

---

#### 2. 【强制】POJO 类的布尔属性不能加 is，而数据库字段必须加 is_，要求在 resultMap 中进行字段与属性之间的映射

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| resultMap 中映射 is_xxx 到 xxx | 缺少映射 |

```xml
<!-- ✅ 正确示例 -->
<resultMap id="AppResultMap" type="AppDO">
    <id column="id" property="id"/>
    <result column="app_id" property="appId"/>
    <result column="is_deleted" property="deleted"/>      <!-- is_deleted 映射到 deleted -->
    <result column="is_bound" property="bound"/>          <!-- is_bound 映射到 bound -->
</resultMap>

<!-- ❌ 错误示例 -->
<resultMap id="AppResultMap" type="AppDO">
    <id column="id" property="id"/>
    <result column="app_id" property="appId"/>
    <!-- ❌ 缺少 is_deleted 和 is_bound 的映射 -->
</resultMap>
```

---

#### 3. 【强制】sql.xml 配置参数使用：#{}，#param# 不要使用 ${} 此种方式容易出现 SQL 注入

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 使用 `#{}` | 使用 `${}` |

```xml
<!-- ✅ 正确示例 -->
<select id="selectById" resultType="AppDO">
    SELECT id, app_id, name
    FROM openplatform_app_t
    WHERE id = #{id}                    <!-- 使用 #{} -->
</select>

<!-- ❌ 错误示例 -->
<select id="selectById" resultType="AppDO">
    SELECT id, app_id, name
    FROM openplatform_app_t
    WHERE id = ${id}                    <!-- ❌ 使用 ${} 容易 SQL 注入 -->
</select>
```

---

#### 4. 【强制】更新数据表记录时，必须同时更新记录对应的 update_time 字段值为当前时间

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 更新 update_time | 未更新 update_time |

```xml
<!-- ✅ 正确示例 -->
<update id="updateApp">
    UPDATE openplatform_app_t
    SET name = #{name},
        status = #{status},
        update_time = NOW()             <!-- 更新 update_time -->
    WHERE id = #{id}
</update>

<!-- ❌ 错误示例 -->
<update id="updateApp">
    UPDATE openplatform_app_t
    SET name = #{name},
        status = #{status}
        <!-- ❌ 缺少 update_time 更新 -->
    WHERE id = #{id}
</update>
```

---

#### 5. 【强制】MyBatis SQL 查询入参必须指定 jdbcType，确保类型安全

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 指定 `jdbcType` | 未指定 `jdbcType` |

```xml
<!-- ✅ 正确示例 - 指定 jdbcType -->
<select id="selectById" resultType="AppDO">
    SELECT id, app_id, name, status
    FROM openplatform_app_t
    WHERE id = #{id,jdbcType=BIGINT}
</select>

<select id="selectByAppId" resultType="AppDO">
    SELECT id, app_id, name, status
    FROM openplatform_app_t
    WHERE app_id = #{appId,jdbcType=VARCHAR}
</select>

<select id="selectByStatus" resultType="AppDO">
    SELECT id, app_id, name, status
    FROM openplatform_app_t
    WHERE status = #{status,jdbcType=INTEGER}
</select>

<insert id="insertApp">
    INSERT INTO openplatform_app_t (app_id, name, status, is_deleted)
    VALUES (#{appId,jdbcType=VARCHAR}, 
            #{name,jdbcType=VARCHAR}, 
            #{status,jdbcType=INTEGER},
            #{deleted,jdbcType=TINYINT})
</insert>

<!-- ❌ 错误示例 - 未指定 jdbcType -->
<select id="selectById" resultType="AppDO">
    SELECT id, app_id, name, status
    FROM openplatform_app_t
    WHERE id = #{id}                    <!-- ❌ 缺少 jdbcType -->
</select>

<select id="selectByAppId" resultType="AppDO">
    SELECT id, app_id, name, status
    FROM openplatform_app_t
    WHERE app_id = #{appId}             <!-- ❌ 缺少 jdbcType -->
</select>

<insert id="insertApp">
    INSERT INTO openplatform_app_t (app_id, name, status)
    VALUES (#{appId}, #{name}, #{status})  <!-- ❌ 所有参数都缺少 jdbcType -->
</insert>
```

**常用 jdbcType 对照表**：

| Java 类型 | jdbcType | 说明 |
|-----------|----------|------|
| `Long` / `long` | `BIGINT` | 长整型 |
| `Integer` / `int` | `INTEGER` | 整型 |
| `String` | `VARCHAR` | 字符串 |
| `Boolean` / `boolean` | `TINYINT` | 布尔值（对应 MySQL 的 tinyint(1)） |
| `Date` / `LocalDateTime` | `TIMESTAMP` | 时间戳 |
| `Date` / `LocalDate` | `DATE` | 日期 |
| `BigDecimal` | `DECIMAL` | 高精度小数 |
| `Double` / `double` | `DOUBLE` | 双精度浮点数 |
| `Float` / `float` | `FLOAT` | 单精度浮点数 |

**原因**：
1. **类型安全**：明确指定类型，避免类型转换错误
2. **性能优化**：MyBatis 无需进行类型推断，提高执行效率
3. **可读性强**：代码意图清晰，便于维护和审查
4. **避免 NULL 问题**：当参数为 NULL 时，jdbcType 帮助 MyBatis 正确处理
5. **统一风格**：团队代码风格统一，避免混用

**注意事项**：
- 所有 SQL 参数（WHERE 条件、INSERT、UPDATE）都必须指定 jdbcType
- 即使参数不可能为 NULL，也必须指定 jdbcType
- 对于可能为 NULL 的参数，jdbcType 尤为重要

---

## 四、项目特定规范

### （一）代码格式

#### 1. 【强制】每行只能声明一个变量，禁止在一行声明多个变量

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 每行一个变量声明 | 一行声明多个变量 |

```java
// ✅ 正确示例 - 每行一个变量
int success = 0;
int failed = 0;
int skipped = 0;

String name = "test";
String scope = "api:test";

// ❌ 错误示例 - 一行声明多个变量
int success = 0, failed = 0, skipped = 0;

String name = "test", scope = "api:test";
```

**原因**：
- 提高代码可读性，每个变量独立一行
- 方便添加注释说明每个变量的用途
- 便于代码审查和调试
- 符合 Java 编码最佳实践

---

#### 2. 【强制】禁止空代码块，所有代码块（if/else/for/while/try/catch 等）必须有实际代码

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 代码块内有实际逻辑或日志 | 只有注释的空代码块 |

```java
// ✅ 正确示例
if (status == null) {
    log.warn("Status is null, using default value");
    status = 0;
}

try {
    processItem(item);
} catch (Exception e) {
    log.error("Failed to process item: {}", item, e);
}

// ❌ 错误示例
if (status == null) {
    // 忽略
}

try {
    processItem(item);
} catch (Exception e) {
    // 忽略异常
}

else if (type.equals("event")) {
    // 类似逻辑
}
```

**原因**：
- 空代码块通常表示未完成的功能
- 空的 catch 块会吞掉异常，导致问题难以排查
- 降低代码可维护性

**建议处理方式**：
- 空 if/else：添加日志记录或抛出异常
- 空 catch：至少添加日志记录异常信息
- 未实现功能：添加 `throw new UnsupportedOperationException("TODO: implement")`

---

#### 3. 【强制】禁止代码行尾有多余空格（Trailing Whitespace）

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 行尾无多余空格 | 行尾有 1 个或多个空格 |

```java
// ✅ 正确示例
String name = "test";
log.info("Processing item: {}", itemId);

// ❌ 错误示例（注意行尾空格）
String name = "test";    
log.info("Processing item: {}", itemId);   
```

**原因**：
- 多余空格增加代码体积，无实际意义
- 在 Git diff 中显示为无意义的修改
- 不同编辑器可能显示不一致
- 影响代码整洁度

**IDE 配置**：
- IntelliJ IDEA: Settings → Editor → General → On Save → Remove trailing spaces
- VS Code: Settings → Files: Trim Trailing Whitespace
- 可配置保存时自动删除行尾空格

---

### （二）字符串处理

#### 1. 【强制】使用 String.format() 必须指定 Locale.ROOT，避免不同环境下的格式化差异

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `String.format(Locale.ROOT, "id=%d", id)` | `String.format("id=%d", id)` |

```java
// ✅ 正确示例
public String buildCacheKey(String appId, Long permissionId) {
    return String.format(Locale.ROOT, "permission:%s:%d", appId, permissionId);
}

public String formatScope(String type, String module, String resource) {
    return String.format(Locale.ROOT, "%s:%s:%s", type, module, resource);
}

// ❌ 错误示例
public String buildCacheKey(String appId, Long permissionId) {
    return String.format("permission:%s:%d", appId, permissionId);
}
```

**原因**：
- 不同地区的数字格式不同（如千分位分隔符）
- 指定 `Locale.ROOT` 确保格式化结果一致性
- 避免在不同服务器环境下产生不同结果

---

#### 2. 【强制】使用 toLowerCase() 和 toUpperCase() 必须指定 Locale.ROOT，避免不同地区的转换差异

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 指定 Locale.ROOT | 不指定 Locale |

```java
// ✅ 正确示例
String normalized = input.toLowerCase(Locale.ROOT);
String upperValue = code.toUpperCase(Locale.ROOT);

if (type.toLowerCase(Locale.ROOT).equals("api")) {
    // 处理 API 类型
}

// ❌ 错误示例
String normalized = input.toLowerCase();  // 使用默认 Locale，可能导致不一致
String upperValue = code.toUpperCase();   // 在土耳其环境下 'i' → 'İ' 而非 'I'

if (type.toLowerCase().equals("api")) {
    // 在某些 Locale 下可能不匹配
}
```

**原因**：
- 不同地区的字母转换规则不同（如土耳其语 'i' → 'İ'）
- 不指定 Locale 可能导致字符串匹配失败
- 确保在不同服务器环境下转换结果一致

**典型案例**：
- 某系统在土耳其服务器上，`"title".toLowerCase()` → `"tıtle"`（非 `"title"`）
- 导致字符串匹配失败，功能异常

---

#### 3. 【强制】判断空值或空字符串时，必须使用工具类方法，禁止手动拼接 null 和空串判断

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| `StringUtils.isBlank(str)` / `StringUtils.isEmpty(str)` | `str == null \|\| str.isEmpty()` |
| `StringUtils.isNotBlank(str)` / `StringUtils.isNotEmpty(str)` | `str != null && !str.isEmpty()` |
| `Objects.isNull(obj)` / `Objects.nonNull(obj)` | `obj == null` / `obj != null` |
| `CollectionUtils.isEmpty(list)` | `list == null \|\| list.isEmpty()` |

```java
// ✅ 正确示例
import org.apache.commons.lang3.StringUtils;
import java.util.Objects;
import org.springframework.util.CollectionUtils;

public class AppService {
    public void process(String name, App app, List<String> ids) {
        // 字符串空判断
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("名称不能为空");
        }
        
        if (StringUtils.isNotBlank(app.getRemark())) {
            // 处理非空备注
        }
        
        // 对象空判断
        if (Objects.isNull(app)) {
            throw new BusinessException("应用不存在");
        }
        
        if (Objects.nonNull(app.getIcon())) {
            // 处理图标
        }
        
        // 集合空判断
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
    }
}

// ❌ 错误示例
public class AppService {
    public void process(String name, App app, List<String> ids) {
        // ❌ 手动拼接 null 和空串判断
        if (name == null || name.isEmpty()) {
            throw new BusinessException("名称不能为空");
        }
        
        // ❌ 手动拼接非空判断
        if (name != null && !name.isEmpty()) {
            // 处理非空名称
        }
        
        // ❌ 直接使用 == null（应使用 Objects.isNull）
        if (app == null) {
            throw new BusinessException("应用不存在");
        }
        
        // ❌ 手动拼接集合空判断
        if (ids == null || ids.isEmpty()) {
            return;
        }
    }
}
```

**推荐工具类**：

| 工具类 | 常用方法 | 说明 |
|--------|---------|------|
| `org.apache.commons.lang3.StringUtils` | `isBlank()`, `isNotBlank()`, `isEmpty()`, `isNotEmpty()` | 字符串空判断（`isBlank` 同时判断 null、空串和纯空格） |
| `java.util.Objects` | `isNull()`, `nonNull()` | 对象空判断（语义更清晰，可读性更强） |
| `org.springframework.util.CollectionUtils` | `isEmpty()`, `isNotEmpty()` | 集合空判断 |
| `org.springframework.util.ObjectUtils` | `isEmpty()`, `isEmpty(Object[])` | 通用对象空判断（支持数组、集合、字符串、Optional） |

**原因**：
- 工具类方法语义清晰，一行代码即可表达完整含义，避免手动拼接遗漏边界条件
- `StringUtils.isBlank()` 同时处理 null、空串 `""` 和纯空格 `"   "` 三种情况，比手动判断更安全
- `Objects.isNull()` / `Objects.nonNull()` 可用于 Stream filter，比 lambda 中写 `== null` 更优雅
- 统一使用工具类，团队代码风格一致，降低遗漏 null 判断的风险

---

### （三）圈复杂度与嵌套深度

#### 1. 【强制】方法圈复杂度必须维持在 15 以下，超过时必须重构拆分

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 圈复杂度 ≤ 15，逻辑清晰 | 圈复杂度 > 15，难以维护 |

```java
// ✅ 正确示例 - 圈复杂度 5
public void updateUser(Long id, UserUpdateRequest request) {
    User user = userMapper.selectById(id);
    
    if (user == null) {
        throw BusinessException.notFound("用户不存在", "User not found");
    }
    
    // 提取到独立方法，降低复杂度
    updateUserFields(user, request);
    updatePermissionIfNeeded(user, request);
    
    userMapper.update(user);
}

private void updateUserFields(User user, UserUpdateRequest request) {
    if (request.getName() != null) {
        user.setName(request.getName());
    }
    if (request.getEmail() != null) {
        user.setEmail(request.getEmail());
    }
}

// ❌ 错误示例 - 圈复杂度 20+
public void updateUser(Long id, UserUpdateRequest request) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException(...);
    }
    if (request.getName() != null && !request.getName().isEmpty()) {
        user.setName(request.getName());
    }
    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
        user.setEmail(request.getEmail());
    }
    if (request.getPhone() != null && !request.getPhone().isEmpty()) {
        user.setPhone(request.getPhone());
    }
    // ... 更多 if 检查，圈复杂度累积超过 15
}
```

**重构策略**：
1. **提取方法**：将复杂逻辑拆分为多个小方法
2. **早返回（Guard Clause）**：用早返回减少嵌套层级
3. **策略模式**：替代复杂的 switch-case 结构
4. **使用 Optional**：减少 null 检查的嵌套

**工具检测**：
- Maven: `mvn checkstyle:check` 配置 CyclomaticComplexity 规则
- IntelliJ IDEA: 安装 MetricsReloaded 插件查看方法复杂度
- PMD: 配置 `CyclomaticComplexity` 规则

---

#### 2. 【强制】方法最大嵌套深度不得超过 5 层，超过时必须重构

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 嵌套深度 ≤ 5，层次清晰 | 嵌套深度 > 5，难以理解 |

```java
// ✅ 正确示例 - 最大深度 2
public void processOrder(Long orderId) {
    Order order = orderMapper.selectById(orderId);
    
    if (order == null) {
        throw BusinessException.notFound("订单不存在", "Order not found");
    }
    
    // 提取方法，避免嵌套
    validateOrderItems(order);
    calculateOrderTotal(order);
    saveOrder(order);
}

private void validateOrderItems(Order order) {
    for (OrderItem item : order.getItems()) {
        // 深度 = 1（for循环）
        if (item.getQuantity() <= 0) {
            // 深度 = 2（if）
            throw BusinessException.badRequest("商品数量无效", "Invalid quantity");
        }
    }
}

// ❌ 错误示例 - 最大深度 6
public void processOrder(Long orderId) {
    Order order = orderMapper.selectById(orderId);
    if (order != null) {                          // 深度 = 1
        if (order.getStatus() == 1) {             // 深度 = 2
            for (OrderItem item : order.getItems()) {  // 深度 = 3
                if (item.getType() == "normal") {       // 深度 = 4
                    if (item.getQuantity() > 0) {       // 深度 = 5
                        if (item.getPrice() > 0) {      // 深度 = 6 ❌
                            // 处理逻辑...
                        }
                    }
                }
            }
        }
    }
}
```

**重构策略**：
1. **早返回（Guard Clause）**：用 return 或 throw 提前退出，减少嵌套
2. **提取方法**：将嵌套逻辑拆分为独立方法
3. **使用 continue/break**：减少循环内的嵌套
4. **使用 Optional**：减少 null 检查的嵌套
5. **策略模式**：替代多层条件判断

---

#### 2. 【强制】方法实现的结构必须清晰，复杂逻辑必须抽取为独立方法，禁止"上帝方法"

> **目的**：保证每个方法职责单一、长度可控、结构清晰，便于阅读、测试和维护。

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 方法职责单一、长度合理、抽取私有方法 | 一个方法几十上百行、什么逻辑都堆在一起 |

**抽取方法的原则**：

| 原则 | 说明 | 触发场景 |
|------|------|----------|
| **单一职责** | 一个方法只做一件事 | 方法超过 30 行 或 圈复杂度 > 10 |
| **可复用** | 重复出现的逻辑必须抽取 | 同样的代码在多处出现 ≥ 2 次 |
| **可命名** | 抽取的方法必须有明确的业务含义 | 抽取后能用动词短语命名 |
| **就近原则** | 抽取的方法放在原方法下方 | 私有方法集中放在类底部 |
| **参数 ≤ 5** | 抽取的方法参数不超过 5 个 | 参数过多时封装为参数对象 |
| **不嵌套 3 层** | 控制流嵌套不超过 3 层 | 嵌套超过 3 层必须抽取 |

**代码示例**：

```java
// ✅ 正确示例 - 入口方法简洁清晰，复杂逻辑全部抽取
@Slf4j
@Service
public class AppServiceImpl implements AppService {

    @Override
    public String createApp(CreateAppRequest request) {
        validateCreateAppRequest(request);
        
        App app = buildAppFromRequest(request);
        saveApp(app);
        bindEamapIfNeeded(app, request);
        addCreatorAsOwner(app, request.getOperatorId());
        
        log.info("[CREATE_APP] App created, appId={}", app.getAppId());
        return app.getAppId();
    }
    
    /** 校验创建应用请求 */
    private void validateCreateAppRequest(CreateAppRequest request) {
        BusinessException.assertNotNull(request, "请求参数不能为空");
        BusinessException.assertNotBlank(request.getNameCn(), "中文名称不能为空");
        BusinessException.assertNotBlank(request.getNameEn(), "英文名称不能为空");
        BusinessException.assertNotNull(request.geteamapAppCode(), "EAMAP不能为空");
        checkAppNameDuplicate(request.getNameCn(), request.getNameEn());
    }
    
    /** 从请求构建 App 实体 */
    private App buildAppFromRequest(CreateAppRequest request) {
        App app = new App();
        BeanUtils.copyProperties(request, app);
        app.setAppId(IdGenerator.nextId("app"));
        app.setAppType(AppType.BUSINESS);
        app.setAppSubType(AppSubType.STANDARD);
        app.setStatus(AppStatus.ACTIVE);
        app.setCreateTime(new Date());
        app.setUpdateTime(new Date());
        return app;
    }
    
    /** 保存 App */
    private void saveApp(App app) {
        appMapper.insert(app);
    }
    
    /** 绑定 EAMAP（如果需要） */
    private void bindEamapIfNeeded(App app, CreateAppRequest request) {
        if (request.geteamapAppCode() != null) {
            eamapService.bind(app.getAppId(), request.geteamapAppCode());
        }
    }
    
    /** 添加创建者为 Owner */
    private void addCreatorAsOwner(App app, String operatorId) {
        AppMember owner = AppMember.createOwner(app.getAppId(), operatorId);
        appMemberMapper.insert(owner);
    }
    
    /** 检查应用名称是否重复 */
    private void checkAppNameDuplicate(String nameCn, String nameEn) {
        if (appMapper.countByName(nameCn, nameEn) > 0) {
            throw new BusinessException("APP_NAME_DUPLICATE", "应用名称已存在");
        }
    }
}

// ❌ 错误示例 - 上帝方法，什么都堆在一起
@Slf4j
@Service
public class AppServiceImpl implements AppService {

    @Override
    public String createApp(CreateAppRequest request) {
        // 100+ 行的逻辑全堆在一个方法里
        if (request == null) {
            throw new BusinessException("PARAM_NULL", "请求参数不能为空");
        }
        if (request.getNameCn() == null || request.getNameCn().isEmpty()) {
            throw new BusinessException("NAME_CN_NULL", "中文名称不能为空");
        }
        if (request.getNameEn() == null || request.getNameEn().isEmpty()) {
            throw new BusinessException("NAME_EN_NULL", "英文名称不能为空");
        }
        if (request.geteamapAppCode() == null) {
            throw new BusinessException("EAMAP_NULL", "EAMAP不能为空");
        }
        if (appMapper.countByName(request.getNameCn(), request.getNameEn()) > 0) {
            throw new BusinessException("APP_NAME_DUPLICATE", "应用名称已存在");
        }
        
        App app = new App();
        app.setAppId(IdGenerator.nextId("app"));
        app.setNameCn(request.getNameCn());
        app.setNameEn(request.getNameEn());
        app.setDescCn(request.getDescCn());
        app.setDescEn(request.getDescEn());
        app.setIcon(request.getIcon());
        app.setAppType(1);
        app.setAppSubType(4);
        app.setStatus(1);
        app.seteamapAppCode(request.geteamapAppCode());
        app.setEamapBound(true);
        app.setCreatorId(request.getOperatorId());
        app.setCreateTime(new Date());
        app.setUpdateTime(new Date());
        
        appMapper.insert(app);
        
        if (request.geteamapAppCode() != null) {
            EamapBindRecord record = new EamapBindRecord();
            record.setAppId(app.getAppId());
            record.seteamapAppCode(request.geteamapAppCode());
            record.setOperatorId(request.getOperatorId());
            record.setBindTime(new Date());
            eamapBindMapper.insert(record);
        }
        
        AppMember owner = new AppMember();
        owner.setAppId(app.getAppId());
        owner.setUserId(request.getOperatorId());
        owner.setMemberType(1);
        owner.setJoinTime(new Date());
        appMemberMapper.insert(owner);
        
        log.info("[CREATE_APP] App created, appId={}", app.getAppId());
        return app.getAppId();
    }
}
```

**Controller 层方法抽取**：

```java
// ✅ 正确示例 - Controller 方法简洁
@Slf4j
@RestController
@RequestMapping("/api/v1/app")
public class AppController {

    @Autowired
    private AppService appService;
    
    /** 创建应用 */
    @PostMapping
    public ApiResponse<String> createApp(@RequestBody @Valid CreateAppRequest request) {
        log.info("[CREATE_APP] [REQUEST] request={}", JsonUtils.toJson(request));
        String appId = appService.createApp(request);
        return ApiResponse.success(appId);
    }
    
    /** 更新应用 */
    @PutMapping("/{appId}")
    public ApiResponse<Void> updateApp(@PathVariable String appId,
                                     @RequestBody @Valid UpdateAppRequest request) {
        log.info("[UPDATE_APP] [REQUEST] appId={}, request={}", appId, JsonUtils.toJson(request));
        appService.updateApp(appId, request);
        return ApiResponse.success();
    }
}
```

**方法命名的最佳实践**：

| 动词 | 适用场景 | 示例 |
|------|----------|------|
| `validate*` | 参数/数据校验 | `validateCreateAppRequest` |
| `check*` | 业务规则检查 | `checkAppNameDuplicate` |
| `build*` | 从 X 构建 Y | `buildAppFromRequest` |
| `convert*` | 类型/格式转换 | `convertToAppDetailVO` |
| `save*` / `insert*` / `update*` / `delete*` | 数据持久化 | `saveApp`、`updateMember` |
| `get*` / `find*` / `query*` / `list*` | 数据查询 | `getAppById`、`listMembers` |
| `bind*` / `unbind*` | 关联/解绑操作 | `bindEamap`、`unbindEamap` |
| `add*` / `remove*` | 增删子项 | `addOwner`、`removeMember` |
| `send*` / `publish*` | 发送/发布 | `sendNotification`、`publishVersion` |
| `parse*` / `format*` | 解析/格式化 | `parseRequest`、`formatResponse` |
| `calc*` / `compute*` | 计算/统计 | `calcElapsed`、`computeScore` |

**什么情况下必须抽取方法**：

| 触发条件 | 抽取策略 |
|----------|----------|
| 方法超过 30 行 | 抽取独立子方法 |
| 圈复杂度 > 10 | 抽取分支逻辑 |
| 嵌套层级 ≥ 3 层 | 抽取内层逻辑 |
| 多层 if-else | 用卫语句（Guard Clause）+ 策略模式 |
| 重复出现的代码块 | 抽取为工具方法 |
| 复杂的表达式/条件 | 抽取为有名字的 `private boolean` 方法 |
| 一段代码有独立的业务含义 | 抽取为业务方法（`build*`、`validate*`、`save*`） |

**抽取方法的命名技巧**：

```java
// ✅ 好的命名 - 描述"做什么"
validateCreateAppRequest(request);
buildAppFromRequest(request);
saveApp(app);
bindEamapIfNeeded(app, request);
addCreatorAsOwner(app, operatorId);
notifyAppCreated(app);

// ❌ 不好的命名 - 描述"怎么做"
processData(request);
handleStuff(app);
doSomething(map);
method1();
temp();
```

**原因**：
1. **可读性**：主方法（入口方法）像一个目录/提纲，一眼看出业务流程
2. **可测试性**：抽取后的方法可以单独写单元测试
3. **可复用性**：抽取的方法可以被其他方法复用
4. **可维护性**：修改一个子逻辑只影响一个方法，不会牵动整个文件
5. **代码评审**：评审者更容易理解整体设计

**注意事项**：
- 抽取的方法**必须在同一个类里**（`private` 方法），不要为了抽取而抽到别的类
- 抽取的方法名**必须有业务含义**，不要用 `method1`、`temp`、`doSomething` 这类无意义名字
- 抽取的方法**不能太长**（不超过 30 行），否则会陷入"无限抽取"
- 抽取时要保持**参数的合理性**（≤ 5 个），参数过多时考虑封装为参数对象
- 抽取后**注意日志的统一性**，在入口方法打日志，私有方法可以不重复打日志
- **不要过度抽取**——两三行的小逻辑不用抽取，过度抽取反而降低可读性

---

### （四）Shell 脚本规范

#### 1. 【强制】Shell 脚本必须以 `#!/bin/bash` 和 `set -ex` 开头，确保异常退出和调试输出

| ✅ 正确示例 | ❌ 错误示例 |
|------------|------------|
| 包含 `set -ex` | 缺少 `set -ex` |

```bash
#!/bin/bash
set -ex

# 脚本内容
echo "Starting deployment"
./deploy.sh

# ❌ 错误示例
#!/bin/bash

# 缺少 set -ex，脚本出错时会继续执行
echo "Starting deployment"
./deploy.sh
```

**`set -ex` 参数说明**：
- `set -e`：脚本中任何命令返回非零状态码时立即退出，防止错误累积
- `set -x`：在执行命令前打印命令，便于调试和日志追踪

**原因**：
- 避免脚本在错误状态下继续执行
- 提供调试信息，便于排查问题
- 符合 Shell 脚本最佳实践
- 提高脚本可靠性

---

## 附录：规则优先级说明

本规范采用三级分类：

| 级别 | 标识 | 含义 | 违反后果 |
|------|------|------|----------|
| **强制** | 【强制】 | 必须遵守，不可违反 | 代码审查不通过，必须修改 |
| **推荐** | 【推荐】 | 建议遵守，但不是强制 | 代码审查时会提出建议 |
| **参考** | 【参考】 | 可选遵守，根据实际情况 | 代码审查时可选提出 |

---

**版本历史**：

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-06-05 | 初始版本，整合阿里巴巴规范与项目特定规范 |

---

**参考资料**：

1. 《阿里巴巴 Java 开发手册》（嵩山版）
2. 项目特定规范文档（plan-code.md）
3. Google Java Style Guide
4. Oracle Java Coding Conventions

---

*最后更新: 2026-06-05*
