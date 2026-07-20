package com.xxx.it.works.wecode.v2.modules.file.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import com.xxx.it.works.wecode.v2.modules.file.service.impl.CommonFileServiceImpl;
import com.xxx.it.works.wecode.v2.modules.file.storage.FileStorageStrategy;
import com.xxx.it.works.wecode.v2.modules.file.validator.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommonFileService — 通用文件上传服务")
@ExtendWith(MockitoExtension.class)
class CommonFileServiceTest {

    private CommonFileServiceImpl service;

    @Mock
    private FileValidator iconValidator;

    @Mock
    private FileValidator diagramValidator;

    @Mock
    private FileStorageStrategy storageStrategy;

    @BeforeEach
    void setUp() {
        service = new CommonFileServiceImpl();
        lenient().when(iconValidator.supportedBizType()).thenReturn(1);
        lenient().when(diagramValidator.supportedBizType()).thenReturn(2);
        ReflectionTestUtils.setField(service, "validators", List.of(iconValidator, diagramValidator));
        ReflectionTestUtils.setField(service, "storageStrategy", storageStrategy);
    }

    @Test
    @DisplayName("上传应返回 storageStrategy 的结果")
    void testUpload_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[100]);
        UploadResult expected = new UploadResult("batch123", "/ability-files/test.png");
        when(storageStrategy.store(any(), anyInt(), anyString(), anyString())).thenReturn(expected);

        UploadResult result = service.upload(file, 1);

        assertEquals("batch123", result.getBatchId());
        assertEquals("/ability-files/test.png", result.getShowUrl());
        verify(iconValidator).validate(file);
        verify(storageStrategy).store(same(file), eq(1), anyString(), eq("png"));
    }

    @Test
    @DisplayName("Dev 模式上传应返回本地 showUrl")
    void testUpload_DevMode() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "icon.png", "image/png", "fake-image-data".getBytes());
        UploadResult expected = new UploadResult("batch456", "/ability-files/icon.png");
        when(storageStrategy.store(any(), anyInt(), anyString(), anyString())).thenReturn(expected);

        UploadResult result = service.upload(file, 1);

        assertNotNull(result);
        assertEquals("batch456", result.getBatchId());
        assertTrue(result.getShowUrl().startsWith("/ability-files/"));
        verify(iconValidator).validate(file);
    }

    @Test
    @DisplayName("空的文件应抛出 BusinessException")
    void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(file, 1));
        assertTrue(ex.getMessageZh().contains("不能为空"));
        verify(iconValidator, never()).validate(any());
        verify(storageStrategy, never()).store(any(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("null bizType 应抛出 BusinessException")
    void testNullBizType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[100]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(file, null));
        assertTrue(ex.getMessageZh().contains("业务类型"));
        verify(iconValidator, never()).validate(any());
        verify(storageStrategy, never()).store(any(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("不支持的业务类型应抛出 BusinessException")
    void testUnsupportedBizType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[100]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(file, 99));
        assertTrue(ex.getMessageZh().contains("不支持的业务类型"));
        verifyNoInteractions(storageStrategy);
    }

    @Test
    @DisplayName("校验不通过应抛出 BusinessException")
    void testValidationFailure() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.gif", "image/gif", new byte[100]);
        doThrow(BusinessException.badRequest("图标仅支持 PNG/SVG 格式", "Icon only supports PNG/SVG"))
                .when(iconValidator).validate(file);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(file, 1));
        assertTrue(ex.getMessageZh().contains("PNG/SVG"));
        verify(iconValidator).validate(file);
        verify(storageStrategy, never()).store(any(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("getShowUrl 应委托 storageStrategy")
    void testGetShowUrl() {
        when(storageStrategy.getShowUrl("batch123")).thenReturn("/ability-files/test.png");

        String showUrl = service.getShowUrl("batch123");

        assertEquals("/ability-files/test.png", showUrl);
        verify(storageStrategy).getShowUrl("batch123");
    }
}
