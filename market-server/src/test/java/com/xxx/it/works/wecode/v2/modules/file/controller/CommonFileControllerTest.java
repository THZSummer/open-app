package com.xxx.it.works.wecode.v2.modules.file.controller;

import com.xxx.it.works.wecode.v2.modules.file.dto.UploadResult;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommonFileController — 通用文件上传接口")
class CommonFileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommonFileService commonFileService;

    @BeforeEach
    void setUp() {
        CommonFileController controller = new CommonFileController();
        ReflectionTestUtils.setField(controller, "commonFileService", commonFileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /service/open/v2/file/upload - 成功上传应返回 batchId 和 showUrl")
    void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[100]);
        UploadResult result =
                new UploadResult("batch123", "/ability-files/test.png");
        when(commonFileService.upload(any(), eq(1))).thenReturn(result);

        mockMvc.perform(multipart("/service/open/v2/file/upload")
                        .file(file)
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.batchId").value("batch123"))
                .andExpect(jsonPath("$.data.showUrl").value("/ability-files/test.png"));
    }

    @Test
    @DisplayName("POST /service/open/v2/file/upload - 缺少 bizType 应返回 400")
    void testUpload_MissingBizType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[100]);

        mockMvc.perform(multipart("/service/open/v2/file/upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /service/open/v2/file/upload - 缺少文件应返回 400")
    void testUpload_MissingFile() throws Exception {
        mockMvc.perform(multipart("/service/open/v2/file/upload")
                        .param("bizType", "1"))
                .andExpect(status().isBadRequest());
    }
}
