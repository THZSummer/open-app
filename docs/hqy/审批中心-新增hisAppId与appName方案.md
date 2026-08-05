# 审批中心列表/详情 - 新增 hisAppId / appName 字段（最终方案 v2）

> **核心思路**：
> 1. Subscription：改造SubscriptionMapper，LEFT JOIN App + AppProperty
> 2. AppVersion：改造AppMapper（不是AppVersionMapper），LEFT JOIN AppProperty
> 3. FlowVersion：改造FlowMapper，LEFT JOIN App + AppProperty
> 4. 这样都能用LEFT JOIN，不增加SQL查询次数

---

## 改动文件总览

| # | 层 | 文件 | 改动 |
|---|---|---|---|
| 1 | 后端 | `SubscriptionMapper.xml` | 新增 `selectByIdWithApp` SQL + resultMap |
| 2 | 后端 | `SubscriptionMapper.java` | 新增 `selectByIdWithApp` 方法 |
| 3 | 后端 | `SubscriptionWithAppVO.java` | **新建**，继承 `Subscription` |
| 4 | 后端 | `AppMapper.xml` | 新增 `selectByIdWithHisAppId` SQL + resultMap |
| 5 | 后端 | `AppMapper.java` | 新增 `selectByIdWithHisAppId` 方法 |
| 6 | 后端 | `AppWithHisAppIdVO.java` | **新建**，继承 `App` |
| 7 | 后端 | `FlowMapper.xml` | 新增 `selectByIdWithApp` SQL + resultMap |
| 8 | 后端 | `FlowMapper.java` | 新增 `selectByIdWithApp` 方法 |
| 9 | 后端 | `FlowWithAppVO.java` | **新建**，继承 `Flow` |
| 10 | 后端 | `PermissionApplyHandler.java` | 改用 `selectByIdWithApp` |
| 11 | 后端 | `AppVersionPublishHandler.java` | 改用 `selectByIdWithHisAppId` |
| 12 | 后端 | `FlowVersionPublishHandler.java` | 改用 `selectByIdWithApp` |
| 13 | 后端 | `ApprovalService.java` | 从 businessData 取值 set 到响应 |
| 14 | 后端 | `ApprovalPendingListResponse.java` | 新增 appName / hisAppId 字段 |
| 15 | 后端 | `ApprovalDetailResponse.java` | 新增 appName / hisAppId 字段 |
| 16 | 前端 | `constants.jsx` | 列表加 2 列 |
| 17 | 前端 | `ApprovalDetailModal.jsx` | 详情加 2 项 |

---

## 第 1 步 · Subscription 相关改动

### 1.1 SubscriptionMapper.xml

在文件末尾 `</mapper>` 之前追加：

```xml
    <!-- 新增：订阅详情（含应用信息），用于审批中心 -->
    <resultMap id="SubscriptionWithAppResultMap" type="com.xxx.it.works.wecode.v2.modules.permission.vo.SubscriptionWithAppVO" extends="BaseResultMap">
        <result column="app_name_cn" property="appName"/>
        <result column="eamap_app_code" property="hisAppId"/>
    </resultMap>

    <select id="selectByIdWithApp" resultMap="SubscriptionWithAppResultMap">
        SELECT s.id,
               s.app_id,
               s.permission_id,
               s.status,
               s.channel_type,
               s.channel_address,
               s.auth_type,
               s.create_time,
               s.last_update_time,
               s.create_by,
               s.last_update_by,
               s.approved_at,
               s.approved_by,
               a.app_name_cn,
               p.property_value AS eamap_app_code
        FROM openplatform_v2_subscription_t s
        LEFT JOIN openplatform_app_t a
               ON a.id = s.app_id
              AND a.status = 1
        LEFT JOIN openplatform_app_p_t p
               ON p.parent_id = a.id
              AND p.property_name = 'eamap_app_code'
              AND p.status = 1
        WHERE s.id = #{id}
    </select>
```

### 1.2 SubscriptionMapper.java

```java
    SubscriptionWithAppVO selectByIdWithApp(@Param("id") Long id);
```

### 1.3 SubscriptionWithAppVO.java（新建）

路径：`open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/permission/vo/SubscriptionWithAppVO.java`

```java
package com.xxx.it.works.wecode.v2.modules.permission.vo;

import com.xxx.it.works.wecode.v2.modules.permission.entity.Subscription;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionWithAppVO extends Subscription {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String hisAppId;
}
```

---

## 第 2 步 · App 相关改动（用于 AppVersionPublishHandler）

### 2.1 AppMapper.xml

在文件末尾 `</mapper>` 之前追加：

```xml
    <!-- 新增：应用详情（含 hisAppId），用于审批中心 -->
    <resultMap id="AppWithHisAppIdResultMap" type="com.xxx.it.works.wecode.v2.modules.app.vo.AppWithHisAppIdVO" extends="AppResultMap">
        <result column="eamap_app_code" property="hisAppId"/>
    </resultMap>

    <select id="selectByIdWithHisAppId" resultMap="AppWithHisAppIdResultMap">
        SELECT a.id,
               a.app_id,
               a.tenant_id,
               a.icon_id,
               a.app_name_cn,
               a.app_name_en,
               a.app_desc_cn,
               a.app_desc_en,
               a.app_type,
               a.app_sub_type,
               a.status,
               a.create_by,
               a.create_time,
               a.last_update_by,
               a.last_update_time,
               p.property_value AS eamap_app_code
        FROM openplatform_app_t a
        LEFT JOIN openplatform_app_p_t p
               ON p.parent_id = a.id
              AND p.property_name = 'eamap_app_code'
              AND p.status = 1
        WHERE a.id = #{id}
    </select>
```

### 2.2 AppMapper.java

```java
    AppWithHisAppIdVO selectByIdWithHisAppId(@Param("id") Long id);
```

### 2.3 AppWithHisAppIdVO.java（新建）

路径：`open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/app/vo/AppWithHisAppIdVO.java`

```java
package com.xxx.it.works.wecode.v2.modules.app.vo;

import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppWithHisAppIdVO extends App {

    private static final long serialVersionUID = 1L;

    private String hisAppId;
}
```

---

## 第 3 步 · Flow 相关改动（用于 FlowVersionPublishHandler）

### 3.1 FlowMapper.xml

在文件末尾 `</mapper>` 之前追加：

```xml
    <!-- 新增：连接流详情（含应用信息），用于审批中心 -->
    <resultMap id="FlowWithAppResultMap" type="com.xxx.it.works.wecode.v2.modules.flow.vo.FlowWithAppVO" extends="FlowResultMap">
        <result column="app_name_cn" property="appName"/>
        <result column="eamap_app_code" property="hisAppId"/>
    </resultMap>

    <select id="selectByIdWithApp" resultMap="FlowWithAppResultMap">
        SELECT f.id,
               f.app_id,
               f.name_cn,
               f.name_en,
               f.desc_cn,
               f.desc_en,
               f.status,
               f.create_by,
               f.create_time,
               f.last_update_by,
               f.last_update_time,
               a.app_name_cn,
               p.property_value AS eamap_app_code
        FROM openplatform_cp_flow_t f
        LEFT JOIN openplatform_app_t a
               ON a.id = f.app_id
              AND a.status = 1
        LEFT JOIN openplatform_app_p_t p
               ON p.parent_id = a.id
              AND p.property_name = 'eamap_app_code'
              AND p.status = 1
        WHERE f.id = #{id}
    </select>
```

### 3.2 FlowMapper.java

```java
    FlowWithAppVO selectByIdWithApp(@Param("id") Long id);
```

### 3.3 FlowWithAppVO.java（新建）

路径：`open-server/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/vo/FlowWithAppVO.java`

```java
package com.xxx.it.works.wecode.v2.modules.flow.vo;

import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowWithAppVO extends Flow {

    private static final long serialVersionUID = 1L;

    private String appName;
    private String hisAppId;
}
```

---

## 第 4 步 · Handler 改动

### 4.1 PermissionApplyHandler.java

**第 92 行改**：

```java
// 原：Subscription subscription = subscriptionMapper.selectById(businessId);
SubscriptionWithAppVO subscription = subscriptionMapper.selectByIdWithApp(businessId);
```

**第 97 行后追加**：

```java
data.put("appId", subscription.getAppId());
data.put("appName", subscription.getAppName());
data.put("hisAppId", subscription.getHisAppId());
```

**import 追加**：

```java
import com.xxx.it.works.wecode.v2.modules.permission.vo.SubscriptionWithAppVO;
```

---

### 4.2 AppVersionPublishHandler.java

**第 81 行改**：

```java
// 原：App app = appMapper.selectById(version.getAppId());
AppWithHisAppIdVO app = appMapper.selectByIdWithHisAppId(version.getAppId());
```

**第 88 行后追加**：

```java
                data.put("appName", app.getAppNameCn());
                data.put("hisAppId", app.getHisAppId());
```

**import 追加**：

```java
import com.xxx.it.works.wecode.v2.modules.app.vo.AppWithHisAppIdVO;
```

---

### 4.3 FlowVersionPublishHandler.java

**第 95 行改**：

```java
// 原：Flow flow = flowMapper.selectById(version.getFlowId());
FlowWithAppVO flow = flowMapper.selectByIdWithApp(version.getFlowId());
```

**第 102 行后追加**：

```java
            data.put("appId", flow.getAppId());
            data.put("appName", flow.getAppName());
            data.put("hisAppId", flow.getHisAppId());
```

**import 追加**：

```java
import com.xxx.it.works.wecode.v2.modules.flow.vo.FlowWithAppVO;
```

---

## 第 5 步 · ApprovalService 改动

### 5.1 convertToPendingListResponse（第 206-222 行）

**找到**：

```java
        if (businessData != null && businessData.get("nameCn") != null) {
            response.setBusinessName((String) businessData.get("nameCn"));
        }
```

**改为**：

```java
        if (businessData != null) {
            if (businessData.get("nameCn") != null) {
                response.setBusinessName((String) businessData.get("nameCn"));
            }
            if (businessData.get("appName") != null) {
                response.setAppName((String) businessData.get("appName"));
            }
            if (businessData.get("hisAppId") != null) {
                response.setHisAppId((String) businessData.get("hisAppId"));
            }
        }
```

### 5.2 getApprovalDetail（第 249-334 行）

在 `return response;` 之前插入：

```java
        // 新增：从 businessData 取 appName / hisAppId
        Map<String, Object> bd = response.getBusinessData();
        if (bd != null) {
            if (bd.get("appName") != null) {
                response.setAppName((String) bd.get("appName"));
            }
            if (bd.get("hisAppId") != null) {
                response.setHisAppId((String) bd.get("hisAppId"));
            }
        }
```

---

## 第 6 步 · DTO 新增字段

### 6.1 ApprovalPendingListResponse.java

```java
    private String appName;
    private String hisAppId;
```

### 6.2 ApprovalDetailResponse.java

同上。

---

## 第 7 步 · 前端改动

### 7.1 constants.jsx

```jsx
  {
    title: '应用名称',
    dataIndex: 'appName',
    key: 'appName',
    width: 150,
    ellipsis: true,
    render: (v) => v || '-',
  },
  {
    title: 'AppID',
    dataIndex: 'hisAppId',
    key: 'hisAppId',
    width: 160,
    ellipsis: true,
    render: (v) => v || '-',
  },
```

### 7.2 ApprovalDetailModal.jsx

```jsx
  <Descriptions.Item label="应用名称">{detail.appName || '-'}</Descriptions.Item>
  <Descriptions.Item label="AppID">{detail.hisAppId || '-'}</Descriptions.Item>
```

---

## 改动量总结

| 项目 | 数量 |
|---|---|
| 改动文件 | 17 |
| 新增 Mapper 方法 | 3 |
| 新增 XML SQL | 3 |
| 新增 VO | 3 |
| 新增 DDL | 0 |
| Handler 改动行数 | 约 25 行 |
| Service 改动行数 | 约 20 行 |
| 前端改动行数 | 约 15 行 |