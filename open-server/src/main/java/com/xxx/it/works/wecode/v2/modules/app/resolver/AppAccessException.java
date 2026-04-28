package com.xxx.it.works.wecode.v2.modules.app.resolver;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;

/**
 * 应用访问异常
 * 
 * <p>用于应用ID无效或无访问权限的场景</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public class AppAccessException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AppAccessException(String code, String messageZh, String messageEn) {
        super(code, messageZh, messageEn);
    }

    /**
     * 应用不存在
     */
    public static AppAccessException notFound(String appId) {
        return new AppAccessException(
            "APP_NOT_FOUND",
            String.format("应用不存在: %s", appId),
            String.format("Application not found: %s", appId)
        );
    }

    /**
     * 无访问权限
     */
    public static AppAccessException noPermission(String appId) {
        return new AppAccessException(
            "APP_NO_PERMISSION",
            String.format("无权访问应用: %s", appId),
            String.format("No permission to access application: %s", appId)
        );
    }
}