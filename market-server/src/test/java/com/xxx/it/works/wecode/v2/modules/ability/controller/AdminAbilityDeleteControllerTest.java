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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminAbilityController 删除接口单元测试
 *
 * <p>覆盖正常删除、abilityType 不存在返回 404 等场景。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAbilityController — 删除能力接口")
class AdminAbilityDeleteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAbilityService adminAbilityService;

    @BeforeEach
    void setUp() {
        AdminAbilityController controller = new AdminAbilityController(adminAbilityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandlerV2())
                .build();
    }

    @Test
    @DisplayName("DELETE /ability/admin/{abilityType} - 正常删除应返回 200")
    void testDelete_Success() throws Exception {
        when(adminAbilityService.delete(anyInt())).thenReturn(ApiResponse.success());

        mockMvc.perform(delete("/service/open/v2/ability/admin/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("DELETE /ability/admin/{abilityType} - abilityType 不存在返回 404")
    void testDelete_NotFound() throws Exception {
        when(adminAbilityService.delete(anyInt()))
                .thenReturn(ApiResponse.error("404", "能力记录不存在", "Ability record not found"));

        mockMvc.perform(delete("/service/open/v2/ability/admin/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.messageZh").value("能力记录不存在"));
    }
}
