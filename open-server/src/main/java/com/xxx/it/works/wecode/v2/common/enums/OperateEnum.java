package com.xxx.it.works.wecode.v2.common.enums;

import com.xxx.it.works.wecode.v2.common.interceptor.DiffConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志统一操作枚举
 *
 * <p>每个枚举值代表一个具体的审计操作场景，包含 operateType / operateObject / descCn / descEn / templateCn / templateEn
 * 六个属性，写入 openplatform_operate_log_t 表</p>
 *
 * <p>templateCn / templateEn 使用 ${xxx} 占位符，由 OperateLogV2Aspect TemplateRenderer
 * 基于 beforeData / afterData JSON 渲染为最终描述文案，写入 desc_cn / desc_en 字段。
 * 若 template 为 null，则回退使用静态 descCn / descEn。</p>
 *
 * @author SDDU Build Agent
 * @version 3.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateEnum {

    // ===== API 权限订阅 =====
    SUBSCRIBE_API_PERMISSION("SUBSCRIBE", "API_PERMISSION", "API权限",
            "申请API权限", "Subscribe API Permission",
            null, null),
    WITHDRAW_API_PERMISSION("WITHDRAW", "API_PERMISSION", "API权限",
            "撤回API权限申请", "Withdraw API Permission",
            null, null),
    DELETE_API_PERMISSION("DELETE", "API_PERMISSION", "API权限",
            "删除API权限订阅", "Delete API Permission",
            null, null),

    // ===== 事件权限订阅 =====
    SUBSCRIBE_EVENT_PERMISSION("SUBSCRIBE", "EVENT_PERMISSION", "事件权限",
            "申请事件权限", "Subscribe Event Permission",
            null, null),
    CONFIG_EVENT_PERMISSION("CONFIG", "EVENT_PERMISSION", "事件权限",
            "配置事件消费参数", "Configure Event Subscription",
            null, null),
    WITHDRAW_EVENT_PERMISSION("WITHDRAW", "EVENT_PERMISSION", "事件权限",
            "撤回事件权限申请", "Withdraw Event Permission",
            null, null),
    DELETE_EVENT_PERMISSION("DELETE", "EVENT_PERMISSION", "事件权限",
            "删除事件权限订阅", "Delete Event Permission",
            null, null),

    // ===== 回调权限订阅 =====
    SUBSCRIBE_CALLBACK_PERMISSION("SUBSCRIBE", "CALLBACK_PERMISSION", "回调权限",
            "申请回调权限", "Subscribe Callback Permission",
            null, null),
    CONFIG_CALLBACK_PERMISSION("CONFIG", "CALLBACK_PERMISSION", "回调权限",
            "配置回调消费参数", "Configure Callback Subscription",
            null, null),
    WITHDRAW_CALLBACK_PERMISSION("WITHDRAW", "CALLBACK_PERMISSION", "回调权限",
            "撤回回调权限申请", "Withdraw Callback Permission",
            null, null),
    DELETE_CALLBACK_PERMISSION("DELETE", "CALLBACK_PERMISSION", "回调权限",
            "删除回调权限订阅", "Delete Callback Permission",
            null, null),

    // ===== 应用管理 =====
    CREATE_APP("CREATE", "APP", "应用基础信息",
            "新增应用", "Create application",
            "新增应用:${appNameCn}", "Create application:${appNameEn}"),
    UPDATE_APP("UPDATE", "APP", "应用基础信息",
            "更新应用基本信息", "Update Application Basic Info",
            "修改基础信息:\n" + DiffConfig.DIFF_FIELDS_PLACEHOLDER, "Update basic info:\n" + DiffConfig.DIFF_FIELDS_PLACEHOLDER) {
        @Override
        public DiffConfig diffConfig() {
            return DiffConfig.builder()
                    .labelOnlyField("iconId", "应用图标", "App icon")
                    .field("appNameCn", "中文名", "Chinese name")
                    .field("appNameEn", "英文名", "English name")
                    .field("appDescCn", "中文描述", "Chinese description")
                    .field("appDescEn", "英文描述", "English description")
                    .labelOnlyField("funcImgId", "功能示意图", "Function diagram")
                    .build();
        }
    },
    UPDATE_APP_VERIFY_TYPE("UPDATE", "APP_VERIFY_TYPE", "认证方式",
            "更新应用认证方式", "Update Application Verify Type",
            "修改认证方式为${verifyTypeDesc}", "Update verify type to ${verifyTypeDesc}"),
    BIND_APP_EAMAP("UPDATE", "APP", "应用基础信息",
            "绑定EAMAP升级业务应用", "Bind EAMAP to Application",
            "绑定应用服务${eamapAppCode}", "Bind application service ${eamapAppCode}"),
    ADD_APP_MEMBER("CREATE", "APP_MEMBER", "成员管理",
            "添加应用成员", "Add Application Member",
            "新增${memberTypeDesc}:${accountId}", "Add ${memberTypeDesc}:${accountId}"),
    DELETE_APP_MEMBER("DELETE", "APP_MEMBER", "成员管理",
            "删除应用成员", "Delete Application Member",
            "删除人员:${accountId}", "Delete member:${accountId}"),
    TRANSFER_APP_OWNER("UPDATE", "APP_MEMBER", "成员管理",
            "转移应用Owner", "Transfer Application Owner",
            "转移Owner给${accountId}", "Transfer Owner to ${accountId}"),
    ADD_APP_ABILITY("CREATE", "APP_ABILITY", "应用能力",
            "添加应用能力", "Add Application Ability",
            "新增${abilityTypeDesc}", "Add ${abilityTypeDesc}"),
    CREATE_APP_VERSION("CREATE", "APP_VERSION", "版本",
            "创建应用版本", "Create Application Version",
            "新增版本${versionCode}", "Create version ${versionCode}"),
    UPDATE_APP_VERSION("UPDATE", "APP_VERSION", "版本",
            "更新应用版本", "Update Application Version",
            "更新版本${versionCode}", "Update version ${versionCode}"),
    PUBLISH_APP_VERSION("UPDATE", "APP_VERSION", "版本",
            "发布应用版本", "Publish Application Version",
            "发布版本${versionCode}", "Publish version ${versionCode}"),
    WITHDRAW_APP_VERSION("UPDATE", "APP_VERSION", "版本",
            "撤回应用版本", "Withdraw Application Version",
            "撤回版本${versionCode}", "Withdraw version ${versionCode}"),
    DELETE_APP_VERSION("DELETE", "APP_VERSION", "版本",
            "删除应用版本", "Delete Application Version",
            "删除版本${versionCode}", "Delete version ${versionCode}"),

    // ===== 连接器管理 =====
    CREATE_CONNECTOR("CREATE", "CONNECTOR", "连接器",
            "创建连接器", "Create Connector",
            "创建连接器:${nameCn}", "Create Connector:${nameEn}"),
    UPDATE_CONNECTOR("UPDATE", "CONNECTOR", "连接器",
            "更新连接器", "Update Connector",
            "更新连接器:${nameCn}", "Update Connector:${nameEn}"),
    DELETE_CONNECTOR("DELETE", "CONNECTOR", "连接器",
            "删除连接器", "Delete Connector",
            "删除连接器:${nameCn}", "Delete Connector:${nameEn}"),

    // ===== 连接流管理 =====
    CREATE_FLOW("CREATE", "FLOW", "连接流",
            "创建连接流", "Create Flow",
            "创建连接流:${nameCn}", "Create Flow:${nameEn}"),
    UPDATE_FLOW("UPDATE", "FLOW", "连接流",
            "更新连接流", "Update Flow",
            "更新连接流:${nameCn}", "Update Flow:${nameEn}"),
    DELETE_FLOW("DELETE", "FLOW", "连接流",
            "删除连接流", "Delete Flow",
            "删除连接流:${nameCn}", "Delete Flow:${nameEn}"),
    CONNECTOR_FLOW_VERSION_PUBLISH("PUBLISH", "FLOW_VERSION", "流版本",
            "发布流版本", "Publish Flow Version",
            "发布版本${versionCode}", "Publish version ${versionCode}"),
    START_FLOW("START", "FLOW", "连接流",
            "启动连接流", "Start Flow",
            "启动连接流:${nameCn}", "Start Flow:${nameEn}"),
    STOP_FLOW("STOP", "FLOW", "连接流",
            "停止连接流", "Stop Flow",
            "停止连接流:${nameCn}", "Stop Flow:${nameEn}"),
    DEPLOY_FLOW("DEPLOY", "FLOW", "连接流",
            "部署连接流", "Deploy Flow",
            "部署连接流:${nameCn}", "Deploy Flow:${nameEn}"),
    INVALIDATE_FLOW("INVALIDATE", "FLOW", "连接流",
            "失效连接流", "Invalidate Flow",
            "失效连接流:${nameCn}", "Invalidate Flow:${nameEn}"),
    RECOVER_FLOW("RECOVER", "FLOW", "连接流",
            "恢复连接流", "Recover Flow",
            "恢复连接流:${nameCn}", "Recover Flow:${nameEn}");

    /**
     * DB operate_type 字段值
     */
    private final String operateType;

    /**
     * 快照路由标识（英文），匹配 EntitySnapshotLoader.supportedObjects()
     */
    private final String operateObject;

    /**
     * DB operate_object 字段值（中文）
     */
    private final String operateObjectCn;

    /**
     * DB operate_desc_cn 字段值（静态短描述，templateCn 为 null 时回退使用）
     */
    private final String descCn;

    /**
     * DB operate_desc_en 字段值（静态短描述，templateEn 为 null 时回退使用）
     */
    private final String descEn;

    /**
     * 审计描述模板（中文），含 ${xxx} 占位符，由 TemplateRenderer 渲染后写入 desc_cn；null 时回退 descCn
     */
    private final String templateCn;

    /**
     * 审计描述模板（英文），含 ${xxx} 占位符，由 TemplateRenderer 渲染后写入 desc_en；null 时回退 descEn
     */
    private final String templateEn;

    /**
     * 判断是否需要加载 before_data 实体快照
     */
    public boolean needsBeforeData() {
        return "UPDATE".equals(operateType)
                || "DELETE".equals(operateType)
                || "WITHDRAW".equals(operateType)
                || "CONFIG".equals(operateType);
    }

    /**
     * 判断是否需要加载 after_data 实体快照
     *
     * <p>SUBSCRIBE: 从 ApiResponse.data 提取创建的订阅记录</p>
     * <p>DELETE: 操作后实体已删除，无需加载</p>
     */
    public boolean needsAfterData() {
        return !"DELETE".equals(operateType);
    }

    /**
     * 获取渲染后的中文描述；若 templateCn 为 null 则回退 descCn
     */
    public String getDescCnOrDefault() {
        return templateCn != null ? templateCn : descCn;
    }

    /**
     * 获取渲染后的英文描述；若 templateEn 为 null 则回退 descEn
     */
    public String getDescEnOrDefault() {
        return templateEn != null ? templateEn : descEn;
    }

    /**
     * diff 字段配置（仅当操作模板含 ${diffFields} 时需要配置）
     *
     * @return diff 配置，null 表示无 diff 需求
     */
    public DiffConfig diffConfig() {
        return null;
    }
}
