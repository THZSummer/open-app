package com.xxx.api.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String messageZh;
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

    public static BusinessException badRequest(String messageZh, String messageEn) {
        return new BusinessException("400", messageZh, messageEn);
    }

    public static BusinessException unauthorized(String messageZh, String messageEn) {
        return new BusinessException("401", messageZh, messageEn);
    }

    public static BusinessException forbidden(String messageZh, String messageEn) {
        return new BusinessException("403", messageZh, messageEn);
    }

    public static BusinessException notFound(String messageZh, String messageEn) {
        return new BusinessException("404", messageZh, messageEn);
    }

    public static BusinessException internalError(String messageZh, String messageEn) {
        return new BusinessException("500", messageZh, messageEn);
    }
}
