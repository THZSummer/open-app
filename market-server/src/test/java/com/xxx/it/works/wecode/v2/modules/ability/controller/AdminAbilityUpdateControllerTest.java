package com.xxx.it.works.wecode.v2.modules.ability.controller;

import com.xxx.it.works.wecode.v2.common.exception.GlobalExceptionHandlerV2;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminAbilityController 编辑接口单元测试
 *
 * <p>覆盖正常更新、字段校验失败、404、loadType 联动校验等场景。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAbilityController — 编辑能力接口")
class AdminAbilityUpdateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAbilityService adminAbilityService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AdminAbilityController controller = new AdminAbilityController(adminAbilityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandlerV2())
                .build();
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - 正常更新应返回 200")
    void testUpdate_Success() throws Exception {
        when(adminAbilityService.update(anyLong(), any())).thenReturn(ApiResponse.success());

        String body = """
                {
                    "nameCn": "新中文名",
                    "lastUpdateTime": "2023-07-10T10:00:00.000+00:00"
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - nameCn 小于2字符应返回 400")
    void testUpdate_NameCnTooShort() throws Exception {
        String body = """
                {
                    "nameCn": "新"
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - nameCn 超过30字符应返回 400")
    void testUpdate_NameCnTooLong() throws Exception {
        String body = """
                {
                    "nameCn": "%s"
                }
                """.formatted("超长中文名测试".repeat(8)); // 7*8 = 56 chars

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - descCn 小于5字符应返回 400")
    void testUpdate_DescCnTooShort() throws Exception {
        String body = """
                {
                    "descCn": "描述"
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - descCn 超过200字符应返回 400")
    void testUpdate_DescCnTooLong() throws Exception {
        // Build a 208-character string: 13 chars × 16 = 208
        String desc = "这是一个超长的描述用于测试".repeat(16);
        String body = """
                {
                    "descCn": "%s"
                }
                """.formatted(desc);

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - orderNum 小于1应返回 400")
    void testUpdate_OrderNumTooSmall() throws Exception {
        String body = """
                {
                    "orderNum": 0
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - id 不存在返回 404")
    void testUpdate_NotFound() throws Exception {
        when(adminAbilityService.update(anyLong(), any()))
                .thenReturn(ApiResponse.error("404", "能力记录不存在", "Ability record not found"));

        String body = """
                {
                    "nameCn": "新中文名"
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.messageZh").value("能力记录不存在"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - 乐观锁冲突返回 409")
    void testUpdate_OptimisticLockConflict() throws Exception {
        when(adminAbilityService.update(anyLong(), any()))
                .thenReturn(ApiResponse.error("409", "数据已被修改，请刷新后重试",
                        "Data has been modified, please refresh and try again"));

        String body = """
                {
                    "nameCn": "新中文名",
                    "lastUpdateTime": "2023-07-10T10:00:00.000+00:00"
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.messageZh").value("数据已被修改，请刷新后重试"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - 空请求体可接受（无字段更新）")
    void testUpdate_EmptyBody() throws Exception {
        when(adminAbilityService.update(anyLong(), any())).thenReturn(ApiResponse.success());

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("PUT /ability/admin/{id} - loadType=2 缺少三要素时应触发业务校验")
    void testUpdate_LoadType2MissingFields_ServiceLevel() throws Exception {
        when(adminAbilityService.update(anyLong(), any()))
                .thenReturn(ApiResponse.error("400",
                        "微前端加载模式下 entryUrl/routePath/aliasName 三要素必填",
                        "entryUrl/routePath/aliasName are required when loadType=2"));

        String body = """
                {
                    "nameCn": "新名称",
                    "loadType": 2
                }
                """;

        mockMvc.perform(put("/service/open/v2/ability/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.messageZh").value("微前端加载模式下 entryUrl/routePath/aliasName 三要素必填"));
    }
}
