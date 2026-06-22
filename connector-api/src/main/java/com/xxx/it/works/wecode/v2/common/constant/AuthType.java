package com.xxx.it.works.wecode.v2.common.constant;

/**
 * 连接器平台认证类型常量（运行时侧）
 *
 * <p>定义 connector-api 运行时使用的认证类型整数编码</p>
 * <p>与 open-server AuthTypeEnum 中的 CONNECTOR_* 值保持一致</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §3.3 连接器配置认证类型
 */
public final class AuthType {

    private AuthType() {
        // 常量类，禁止实例化
    }

    /** SOA 认证 */
    public static final int SOA = 1;

    /** APIG 认证 */
    public static final int APIG = 2;

    /** Cookie 认证 */
    public static final int COOKIE = 8;

    /** 数字签名认证 */
    public static final int DIGITAL_SIGN = 9;

    /** 多认证组合 */
    public static final int MULTI_AUTH = 10;

    /**
     * 校验认证类型码是否有效
     *
     * @param authType 认证类型码
     * @return true-有效, false-无效
     */
    public static boolean isValid(int authType) {
        return authType == SOA
                || authType == APIG
                || authType == COOKIE
                || authType == DIGITAL_SIGN
                || authType == MULTI_AUTH;
    }

    /**
     * 获取认证类型名称
     *
     * @param authType 认证类型码
     * @return 认证类型名称，未知返回 "UNKNOWN"
     */
    public static String getName(int authType) {
        switch (authType) {
            case SOA:
                return "SOA";
            case APIG:
                return "APIG";
            case COOKIE:
                return "COOKIE";
            case DIGITAL_SIGN:
                return "DIGITAL_SIGN";
            case MULTI_AUTH:
                return "MULTI_AUTH";
            default:
                return "UNKNOWN";
        }
    }
}
