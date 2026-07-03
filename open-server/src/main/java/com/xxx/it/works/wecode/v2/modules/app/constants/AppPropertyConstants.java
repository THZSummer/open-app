package com.xxx.it.works.wecode.v2.modules.app.constants;

/**
 * 应用属性名常量（openplatform_app_p_t.property_name）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class AppPropertyConstants {

    private AppPropertyConstants() {
    }

    /**
     * EAMAP 编码
     */
    public static final String PROP_EAMAP_CODE = "eamap_app_code";

    /**
     * 认证方式（旧字段，单选值，保留不动）
     */
    public static final String PROP_VERIFY_TYPE = "verify_type";

    /**
     * 认证方式（新字段，多选值，逗号分隔）
     */
    public static final String PROP_VERIFY_TYPE_V2 = "verify_type_v2";

    /**
     * API 密钥
     */
    public static final String PROP_API_SECRET = "api_secret";

    /**
     * 架构图ID列表
     */
    public static final String PROP_DIAGRAM_ID_LIST = "diagram_id_list";

    /**
     * EAMAP 应用名称
     */
    public static final String PROP_EAMAP_APP_NAME = "eamap_app_name";
}
