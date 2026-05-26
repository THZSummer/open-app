package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计日志统一操作枚举
 *
 * <p>每个枚举值代表一个具体的审计操作场景，包含 operateType / operateObject / descCn / descEn 四个属性，
 * 写入 openplateform_operate_log_t 表</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Getter
@AllArgsConstructor
public enum OperateEnum {

    // ===== API 权限订阅 =====
    SUBSCRIBE_API_PERMISSION("SUBSCRIBE", "API_PERMISSION",
            "申请API权限", "Subscribe API Permission"),
    WITHDRAW_API_PERMISSION("WITHDRAW", "API_PERMISSION",
            "撤回API权限申请", "Withdraw API Permission"),
    DELETE_API_PERMISSION("DELETE", "API_PERMISSION",
            "删除API权限订阅", "Delete API Permission"),

    // ===== 事件权限订阅 =====
    SUBSCRIBE_EVENT_PERMISSION("SUBSCRIBE", "EVENT_PERMISSION",
            "申请事件权限", "Subscribe Event Permission"),
    CONFIG_EVENT_PERMISSION("CONFIG", "EVENT_PERMISSION",
            "配置事件消费参数", "Configure Event Subscription"),
    WITHDRAW_EVENT_PERMISSION("WITHDRAW", "EVENT_PERMISSION",
            "撤回事件权限申请", "Withdraw Event Permission"),
    DELETE_EVENT_PERMISSION("DELETE", "EVENT_PERMISSION",
            "删除事件权限订阅", "Delete Event Permission"),

    // ===== 回调权限订阅 =====
    SUBSCRIBE_CALLBACK_PERMISSION("SUBSCRIBE", "CALLBACK_PERMISSION",
            "申请回调权限", "Subscribe Callback Permission"),
    CONFIG_CALLBACK_PERMISSION("CONFIG", "CALLBACK_PERMISSION",
            "配置回调消费参数", "Configure Callback Subscription"),
    WITHDRAW_CALLBACK_PERMISSION("WITHDRAW", "CALLBACK_PERMISSION",
            "撤回回调权限申请", "Withdraw Callback Permission"),
    DELETE_CALLBACK_PERMISSION("DELETE", "CALLBACK_PERMISSION",
            "删除回调权限订阅", "Delete Callback Permission");

    /** DB operate_type 字段值 */
    private final String operateType;

    /** DB operate_object 字段值 */
    private final String operateObject;

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
