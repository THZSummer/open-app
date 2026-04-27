package com.xxx.it.works.wecode.v2.common.exception;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("handleBusinessException 测试")
    class HandleBusinessExceptionTest {

        @Test
        @DisplayName("处理业务异常，返回 ApiResponse.error")
        void handleBusinessException_shouldReturnErrorResponse() {
            BusinessException exception = new BusinessException("1001", "用户不存在", "User not found");

            ApiResponse<Void> response = handler.handleBusinessException(exception);

            assertEquals("1001", response.getCode());
            assertEquals("用户不存在", response.getMessageZh());
            assertEquals("User not found", response.getMessageEn());
            assertNull(response.getData());
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValidException 测试")
    class HandleMethodArgumentNotValidExceptionTest {

        @Test
        @DisplayName("Mock MethodArgumentNotValidException，验证返回错误响应")
        void handleMethodArgumentNotValidException_shouldReturnErrorResponse() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError1 = new FieldError("object", "name", "名称不能为空");
            FieldError fieldError2 = new FieldError("object", "age", "年龄必须大于0");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ApiResponse<Void> response = handler.handleMethodArgumentNotValidException(exception);

            assertEquals("400", response.getCode());
            assertTrue(response.getMessageZh().contains("名称不能为空"));
            assertTrue(response.getMessageZh().contains("年龄必须大于0"));
            assertTrue(response.getMessageEn().contains("名称不能为空"));
            assertTrue(response.getMessageEn().contains("年龄必须大于0"));
        }
    }

    @Nested
    @DisplayName("handleConstraintViolationException 测试")
    class HandleConstraintViolationExceptionTest {

        @Test
        @DisplayName("Mock ConstraintViolationException，验证返回错误响应")
        void handleConstraintViolationException_shouldReturnErrorResponse() {
            ConstraintViolationException exception = mock(ConstraintViolationException.class);
            ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
            ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);

            when(violation1.getMessage()).thenReturn("邮箱格式不正确");
            when(violation2.getMessage()).thenReturn("手机号格式不正确");
            when(exception.getConstraintViolations()).thenReturn(Set.of(violation1, violation2));

            ApiResponse<Void> response = handler.handleConstraintViolationException(exception);

            assertEquals("400", response.getCode());
            assertTrue(response.getMessageZh().contains("邮箱格式不正确"));
            assertTrue(response.getMessageZh().contains("手机号格式不正确"));
            assertTrue(response.getMessageEn().contains("邮箱格式不正确"));
            assertTrue(response.getMessageEn().contains("手机号格式不正确"));
        }
    }

    @Nested
    @DisplayName("handleException 测试")
    class HandleExceptionTest {

        @Test
        @DisplayName("处理通用异常，返回 500 错误")
        void handleException_shouldReturn500ErrorResponse() {
            Exception exception = new RuntimeException("数据库连接失败");

            ApiResponse<Void> response = handler.handleException(exception);

            assertEquals("500", response.getCode());
            assertEquals("系统内部错误", response.getMessageZh());
            assertEquals("Internal Server Error", response.getMessageEn());
            assertNull(response.getData());
        }
    }
}