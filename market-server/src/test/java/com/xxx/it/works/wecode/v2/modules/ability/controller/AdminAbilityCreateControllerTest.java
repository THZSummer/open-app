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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminAbilityController 创建接口单元测试
 *
 * <p>覆盖正常创建、参数校验失败、编码唯一性冲突等场景。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAbilityController — 创建能力接口")
class AdminAbilityCreateControllerTest {

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
    @DisplayName("POST /ability/admin - 正常创建应返回 200 含 data")
    void testCreate_Success() throws Exception {
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("abilityType", 100);
        resultData.put("nameCn", "测试能力");
        when(adminAbilityService.create(any())).thenReturn(ApiResponse.success(resultData));

        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测试能力",
                    "nameEn": "TestAbility",
                    "descCn": "这是一个测试能力的详细描述",
                    "descEn": "This is a test ability description",
                    "orderNum": 1,
                    "iconBatchId": "batch_icon_001"
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.abilityType").value(100))
                .andExpect(jsonPath("$.data.nameCn").value("测试能力"));
    }

    @Test
    @DisplayName("POST /ability/admin - 缺少必填字段应返回 400")
    void testCreate_MissingRequiredFields() throws Exception {
        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测试"
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("POST /ability/admin - nameCn 小于2字符应返回 400")
    void testCreate_NameCnTooShort() throws Exception {
        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测",
                    "nameEn": "TestEn",
                    "descCn": "这是一个测试能力的详细描述",
                    "descEn": "This is a test ability description in English",
                    "iconBatchId": "batch_icon_001"
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("POST /ability/admin - descCn 小于5字符应返回 400")
    void testCreate_DescCnTooShort() throws Exception {
        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测试能力",
                    "nameEn": "TestEn",
                    "descCn": "描述",
                    "descEn": "This is a test ability description in English",
                    "iconBatchId": "batch_icon_001"
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("POST /ability/admin - 能力类型编码冲突应返回 409")
    void testCreate_DuplicateAbilityType() throws Exception {
        when(adminAbilityService.create(any()))
                .thenReturn(ApiResponse.error("409", "编码已被占用", "Ability type already exists"));

        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测试能力",
                    "nameEn": "TestEn",
                    "descCn": "这是一个测试能力的详细描述",
                    "descEn": "This is a test ability description in English",
                    "orderNum": 1,
                    "iconBatchId": "batch_icon_001"
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.messageZh").value("编码已被占用"));
    }

    @Test
    @DisplayName("POST /ability/admin - 空请求体应返回 400")
    void testCreate_EmptyBody() throws Exception {
        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    @DisplayName("POST /ability/admin - 缺少 iconBatchId 应返回 400")
    void testCreate_MissingIconBatchId() throws Exception {
        String body = """
                {
                    "abilityType": 100,
                    "nameCn": "测试能力",
                    "nameEn": "TestEn",
                    "descCn": "这是一个测试能力的详细描述",
                    "descEn": "This is a test ability description in English",
                    "orderNum": 1
                }
                """;

        mockMvc.perform(post("/service/open/v2/ability/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"));
    }
}
