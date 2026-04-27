package com.xxx.open.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("业务异常测试")
class BusinessExceptionTest {

    @Nested
    @DisplayName("工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("badRequest - 验证 code=400, messageZh, messageEn 正确")
        void badRequest_shouldReturnExceptionWithCode400() {
            BusinessException exception = BusinessException.badRequest("参数错误", "Bad Request");

            assertEquals("400", exception.getCode());
            assertEquals("参数错误", exception.getMessageZh());
            assertEquals("Bad Request", exception.getMessageEn());
            assertEquals("参数错误", exception.getMessage());
        }

        @Test
        @DisplayName("unauthorized - 验证 code=401")
        void unauthorized_shouldReturnExceptionWithCode401() {
            BusinessException exception = BusinessException.unauthorized("未授权", "Unauthorized");

            assertEquals("401", exception.getCode());
            assertEquals("未授权", exception.getMessageZh());
            assertEquals("Unauthorized", exception.getMessageEn());
        }

        @Test
        @DisplayName("forbidden - 验证 code=403")
        void forbidden_shouldReturnExceptionWithCode403() {
            BusinessException exception = BusinessException.forbidden("禁止访问", "Forbidden");

            assertEquals("403", exception.getCode());
            assertEquals("禁止访问", exception.getMessageZh());
            assertEquals("Forbidden", exception.getMessageEn());
        }

        @Test
        @DisplayName("notFound - 验证 code=404")
        void notFound_shouldReturnExceptionWithCode404() {
            BusinessException exception = BusinessException.notFound("资源不存在", "Not Found");

            assertEquals("404", exception.getCode());
            assertEquals("资源不存在", exception.getMessageZh());
            assertEquals("Not Found", exception.getMessageEn());
        }

        @Test
        @DisplayName("internalError - 验证 code=500")
        void internalError_shouldReturnExceptionWithCode500() {
            BusinessException exception = BusinessException.internalError("服务器内部错误", "Internal Server Error");

            assertEquals("500", exception.getCode());
            assertEquals("服务器内部错误", exception.getMessageZh());
            assertEquals("Internal Server Error", exception.getMessageEn());
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用构造函数创建异常，验证所有字段正确")
        void constructor_shouldSetAllFieldsCorrectly() {
            BusinessException exception = new BusinessException("CUSTOM_CODE", "自定义错误", "Custom Error");

            assertEquals("CUSTOM_CODE", exception.getCode());
            assertEquals("自定义错误", exception.getMessageZh());
            assertEquals("Custom Error", exception.getMessageEn());
            assertEquals("自定义错误", exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("带 cause 的构造函数 - 验证 cause 正确传递")
        void constructorWithCause_shouldSetCauseCorrectly() {
            Throwable cause = new RuntimeException("原始异常");
            BusinessException exception = new BusinessException("ERROR_CODE", "业务错误", "Business Error", cause);

            assertEquals("ERROR_CODE", exception.getCode());
            assertEquals("业务错误", exception.getMessageZh());
            assertEquals("Business Error", exception.getMessageEn());
            assertEquals("业务错误", exception.getMessage());
            assertSame(cause, exception.getCause());
        }
    }
}