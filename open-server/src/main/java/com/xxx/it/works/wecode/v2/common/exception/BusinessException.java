package com.xxx.it.works.wecode.v2.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * 
 * <p>用于业务逻辑中抛出的可预期异常</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 中文错误消息
     */
    private final String messageZh;

    /**
     * 英文错误消息
     */
    private final String messageEn;

    public BusinessException(String code, String messageZh, String messageEn) {
        super(messageZh);
        this.code = code;
        this.messageZh = messageZh;
        this.messageEn = messageEn;
    }

    public BusinessException(String code, String messageZh, String messageEn, Throwable cause) {
        super(messageZh, cause);
        this.code = code;
        this.messageZh = messageZh;
        this.messageEn = messageEn;
    }

    /**
     * 参数错误
     */
    public static BusinessException badRequest(String messageZh, String messageEn) {
        return new BusinessException("400", messageZh, messageEn);
    }

    /**
     * 未授权
     */
    public static BusinessException unauthorized(String messageZh, String messageEn) {
        return new BusinessException("401", messageZh, messageEn);
    }

    /**
     * 禁止访问
     */
    public static BusinessException forbidden(String messageZh, String messageEn) {
        return new BusinessException("403", messageZh, messageEn);
    }

    /**
     * 资源未找到
     */
    public static BusinessException notFound(String messageZh, String messageEn) {
        return new BusinessException("404", messageZh, messageEn);
    }

    /**
     * 内部服务器错误
     */
    public static BusinessException internalError(String messageZh, String messageEn) {
        return new BusinessException("500", messageZh, messageEn);
    }
}
