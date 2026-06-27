package com.xxx.it.works.wecode.v2.common.constants;

/**
 * 通用常量
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // ===== 通用 =====
    public static final String DEFAULT_TENANT_ID = "default";
    public static final int STATUS_ACTIVE = 1;
    public static final Long ROOT_CATEGORY_PARENT_ID = 0L;
    public static final int NEED_APPROVAL_YES = 1;
    public static final int NEED_APPROVAL_NO = 0;

    // ===== 权限资源类型（permission.resource_type）=====
    public static final String RESOURCE_TYPE_API = "api";
    public static final String RESOURCE_TYPE_CALLBACK = "callback";
    public static final String RESOURCE_TYPE_EVENT = "event";

    // ===== 通用属性名 =====
    public static final String PROP_DOC_URL = "docUrl";

    // ===== 卡片服务事件类型 =====
    public static final String EVENT_CREATE = "CREATE";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_UPDATE_VERIFY_TYPE = "UPDATE_VERIFY_TYPE";
    public static final String EVENT_BIND_EAMAP = "BIND_EAMAP";
    public static final String EVENT_TRANSFER_OWNER = "TRANSFER_OWNER";

    // ===== 认证方式默认值 =====
    public static final String DEFAULT_VERIFY_TYPE = "0";

    // ===== 凭证前缀 =====
    public static final String AK_PREFIX = "AK_";
    public static final String SK_PREFIX = "SK_";

    // ===== ID 前缀 =====
    public static final String APP_ID_PREFIX = "app_";
    public static final String FILE_ID_PREFIX = "file_";

    // ===== 日期格式 =====
    public static final String APP_ID_DATE_FORMAT = "yyyyMMddHHmmss";

    // ===== 字典查询参数 =====
    public static final String DICT_CATEGORY_APP = "app";
    public static final String DICT_VERIFY_TYPE_MULTI_SWITCH = "verify_type_multi_switch";

    // ===== 通用标志值 =====
    public static final String FLAG_TRUE = "true";
    public static final String FLAG_FALSE = "false";

    // ===== 日志兜底值 =====
    public static final String UNKNOWN = "unknown";

    // ===== 通用字段名（JSON 字段 / 方法参数名）=====
    /** JSON 驼峰字段名 / Java 方法参数名 */
    public static final String FIELD_APP_ID = "appId";
}
