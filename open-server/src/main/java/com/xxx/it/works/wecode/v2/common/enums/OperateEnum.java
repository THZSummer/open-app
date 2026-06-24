package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志统一操作枚举
 *
 * <p>每个枚举值代表一个具体的审计操作场景，包含 operateType / operateObject / descCn / descEn 四个属性，
 * 写入 openplatform_operate_log_t 表</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateEnum {

    // ===== API 权限订阅 =====
    SUBSCRIBE_API_PERMISSION("SUBSCRIBE", "API_PERMISSION", "API权限",
            "申请API权限", "Subscribe API Permission"),
    WITHDRAW_API_PERMISSION("WITHDRAW", "API_PERMISSION", "API权限",
            "撤回API权限申请", "Withdraw API Permission"),
    DELETE_API_PERMISSION("DELETE", "API_PERMISSION", "API权限",
            "删除API权限订阅", "Delete API Permission"),

    // ===== 事件权限订阅 =====
    SUBSCRIBE_EVENT_PERMISSION("SUBSCRIBE", "EVENT_PERMISSION", "事件权限",
            "申请事件权限", "Subscribe Event Permission"),
    CONFIG_EVENT_PERMISSION("CONFIG", "EVENT_PERMISSION", "事件权限",
            "配置事件消费参数", "Configure Event Subscription"),
    WITHDRAW_EVENT_PERMISSION("WITHDRAW", "EVENT_PERMISSION", "事件权限",
            "撤回事件权限申请", "Withdraw Event Permission"),
    DELETE_EVENT_PERMISSION("DELETE", "EVENT_PERMISSION", "事件权限",
            "删除事件权限订阅", "Delete Event Permission"),

    // ===== 回调权限订阅 =====
    SUBSCRIBE_CALLBACK_PERMISSION("SUBSCRIBE", "CALLBACK_PERMISSION", "回调权限",
            "申请回调权限", "Subscribe Callback Permission"),
    CONFIG_CALLBACK_PERMISSION("CONFIG", "CALLBACK_PERMISSION", "回调权限",
            "配置回调消费参数", "Configure Callback Subscription"),
    WITHDRAW_CALLBACK_PERMISSION("WITHDRAW", "CALLBACK_PERMISSION", "回调权限",
            "撤回回调权限申请", "Withdraw Callback Permission"),
    DELETE_CALLBACK_PERMISSION("DELETE", "CALLBACK_PERMISSION", "回调权限",
            "删除回调权限订阅", "Delete Callback Permission"),

    // ===== V3 连接器操作 =====
    CREATE_CONNECTOR("CREATE", "CONNECTOR", "连接器",
            "创建连接器", "Create Connector"),
    UPDATE_CONNECTOR("UPDATE", "CONNECTOR", "连接器",
            "编辑连接器基本信息", "Update Connector"),
    INVALIDATE_CONNECTOR("INVALIDATE", "CONNECTOR", "连接器",
            "失效连接器", "Invalidate Connector"),
    RECOVER_CONNECTOR("RECOVER", "CONNECTOR", "连接器",
            "恢复连接器", "Recover Connector"),
    DELETE_CONNECTOR("DELETE", "CONNECTOR", "连接器",
            "删除连接器", "Delete Connector"),

    // ===== V3 连接器版本操作 =====
    CREATE_CONNECTOR_VERSION_DRAFT("CREATE", "CONNECTOR_VERSION", "连接器版本",
            "创建连接器草稿版本", "Create Connector Version Draft"),
    PUBLISH_CONNECTOR_VERSION("PUBLISH", "CONNECTOR_VERSION", "连接器版本",
            "发布连接器版本", "Publish Connector Version"),
    INVALIDATE_CONNECTOR_VERSION("INVALIDATE", "CONNECTOR_VERSION", "连接器版本",
            "失效连接器版本", "Invalidate Connector Version"),
    RECOVER_CONNECTOR_VERSION("RECOVER", "CONNECTOR_VERSION", "连接器版本",
            "恢复连接器版本", "Recover Connector Version"),
    DELETE_CONNECTOR_VERSION("DELETE", "CONNECTOR_VERSION", "连接器版本",
            "删除连接器版本", "Delete Connector Version"),

    // ===== V3 连接流操作 =====
    CREATE_FLOW("CREATE", "FLOW", "连接流",
            "创建连接流", "Create Flow"),
    UPDATE_FLOW("UPDATE", "FLOW", "连接流",
            "编辑连接流基本信息", "Update Flow"),
    COPY_FLOW("COPY", "FLOW", "连接流",
            "复制连接流", "Copy Flow"),
    DEPLOY_FLOW("DEPLOY", "FLOW", "连接流",
            "部署连接流", "Deploy Flow"),
    START_FLOW("START", "FLOW", "连接流",
            "启动连接流", "Start Flow"),
    STOP_FLOW("STOP", "FLOW", "连接流",
            "停止连接流", "Stop Flow"),
    INVALIDATE_FLOW("INVALIDATE", "FLOW", "连接流",
            "失效连接流", "Invalidate Flow"),
    RECOVER_FLOW("RECOVER", "FLOW", "连接流",
            "恢复连接流", "Recover Flow"),
    DELETE_FLOW("DELETE", "FLOW", "连接流",
            "删除连接流", "Delete Flow"),

    // ===== V3 连接流版本操作 =====
    CREATE_FLOW_VERSION_DRAFT("CREATE", "FLOW_VERSION", "连接流版本",
            "创建连接流草稿版本", "Create Flow Version Draft"),
    PUBLISH_FLOW_VERSION("PUBLISH", "FLOW_VERSION", "连接流版本",
            "提交连接流版本发布审批", "Submit Flow Version Publish Approval"),
    INVALIDATE_FLOW_VERSION("INVALIDATE", "FLOW_VERSION", "连接流版本",
            "失效连接流版本", "Invalidate Flow Version"),
    RECOVER_FLOW_VERSION("RECOVER", "FLOW_VERSION", "连接流版本",
            "恢复连接流版本", "Recover Flow Version"),
    DELETE_FLOW_VERSION("DELETE", "FLOW_VERSION", "连接流版本",
            "删除连接流版本", "Delete Flow Version"),

    // ===== V3 审批操作 =====
    APPROVE_FLOW_VERSION("APPROVE", "FLOW_VERSION_APPROVAL", "连接流版本审批",
            "审批通过", "Approve"),
    REJECT_FLOW_VERSION("REJECT", "FLOW_VERSION_APPROVAL", "连接流版本审批",
            "审批驳回", "Reject"),
    WITHDRAW_FLOW_VERSION_APPROVAL("WITHDRAW", "FLOW_VERSION_APPROVAL", "连接流版本审批",
            "撤回审批", "Withdraw Approval"),
    URGE_FLOW_VERSION_APPROVAL("URGE", "FLOW_VERSION_APPROVAL", "连接流版本审批",
            "催办审批", "Urge Approval");

    /** DB operate_type 字段值 */
    private final String operateType;

    /** 快照路由标识（英文），匹配 EntitySnapshotLoader.supportedObjects() */
    private final String operateObject;

    /** DB operate_object 字段值（中文） */
    private final String operateObjectCn;

    /** DB operate_desc_cn 字段值 */
    private final String descCn;

    /** DB operate_desc_en 字段值 */
    private final String descEn;

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
}
