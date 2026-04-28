package com.xxx.it.works.wecode.v2.common.exception;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * 
 * <p>Unified exception handler that returns standard error format: {code, messageZh, messageEn, data: null, page: null}</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerV2 {

    /**
     * Handle business exception
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, messageZh={}, messageEn={}", e.getCode(), e.getMessageZh(), e.getMessageEn());
        return ApiResponse.error(e.getCode(), e.getMessageZh(), e.getMessageEn());
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
        return ApiResponse.error("400", "Parameter error: " + errorMessage, "Bad Request: " + errorMessage);
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
        return ApiResponse.error("400", "Parameter error: " + errorMessage, "Bad Request: " + errorMessage);
    }

    /**
     * Handle unknown exception
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("System exception", e);
        return ApiResponse.error("500", "Internal server error", "Internal Server Error");
    }
}