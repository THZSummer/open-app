package com.xxx.it.works.wecode.v2.modules.approval.constant;

public final class ApprovalConstants {

    // 审批记录状态
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_CANCELLED = 3;

    // 审批操作
    public static final int ACTION_APPROVE = 0;
    public static final int ACTION_REJECT = 1;
    public static final int ACTION_CANCEL = 2;
    public static final int ACTION_TRANSFER = 3;
    public static final int ACTION_URGE = 4;

    // 业务类型
    public static final String BUSINESS_TYPE_APP_VERSION_PUBLISH = "app_version_publish";

    // 第三方应用ID属性名
    public static final String PROPERTY_THIRD_PARTY_APP_ID = "third_party_app_id";

    private ApprovalConstants() {
    }
}
