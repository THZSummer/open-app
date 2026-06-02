package com.xxx.it.works.wecode.v2.common.exception;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * V2 模块全局异常处理器
 *
 * <p>限定处理范围：仅处理 com.xxx.it.works.wecode.v2 包下的异常</p>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>open-server 作为独立模块集成到现有 Spring Boot 工程</li>
 *   <li>通过 basePackages 限定异常处理范围，避免影响其他模块</li>
 *   <li>统一返回标准错误格式：{code, messageZh, messageEn, data: null, page: null}</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.xxx.it.works.wecode.v2")
public class GlobalExceptionHandlerV2 {

    /**
     * Handle business exception
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, messageZh={}, messageEn={}", e.getCode(), e.getMessageZh(), e.getMessageEn());
        return ApiResponse.<Void>error(e.getCode(), e.getMessageZh(), e.getMessageEn());
    }

    /**
     * Handle parameter validation exception (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Parameter validation failed: {}", errorMessage);
        return ApiResponse.<Void>error(ResponseCodeEnum.PARAM_ERROR, "Parameter error: " + errorMessage, "Bad Request: " + errorMessage);
    }

    /**
     * Handle parameter validation exception (@Validated)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Parameter validation failed: {}", errorMessage);
        return ApiResponse.<Void>error(ResponseCodeEnum.PARAM_ERROR, "Parameter error: " + errorMessage, "Bad Request: " + errorMessage);
    }

    /**
     * Handle illegal argument exception
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResponse.error(ResponseCodeEnum.PARAM_ERROR, "Parameter error: " + e.getMessage(), "Bad Request: " + e.getMessage());
    }

    /**
     * Handle JSON parse error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求体格式错误";
        Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) cause;
            message = "请求体中包含未知字段: " + upe.getPropertyName();
        }
        log.warn("JSON parse error: {}", e.getMessage());
        return ApiResponse.error(ResponseCodeEnum.PARAM_ERROR, message, "Bad Request: " + message);
    }

    /**
     * Handle unknown exception
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("System exception", e);
        return ApiResponse.error(ResponseCodeEnum.INTERNAL_ERROR, "Internal server error", "Internal Server Error");
    }
}