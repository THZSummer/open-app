package com.xxx.it.works.wecode.v2.modules.ability.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminAbilityController 列表接口单元测试
 *
 * <p>覆盖正常分页查询、关键字搜索、排序、以及字段映射校验。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAbilityController — 能力列表接口")
class AdminAbilityListControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAbilityService adminAbilityService;

    @BeforeEach
    void setUp() {
        AdminAbilityController controller = new AdminAbilityController(adminAbilityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /ability/admin/list - 正常分页查询应返回 200")
    void testList_Success() throws Exception {
        AdminAbilityVO vo = AdminAbilityVO.builder()
                .abilityType(100)
                .nameCn("测试能力")
                .nameEn("TestAbility")
                .descCn("中文描述")
                .descEn("English Desc")
                .icon("ability_icon_1")
                .iconUrl("/ability-files/ability_icon_1")
                .exampleDiagram("ability_diagram_1")
                .exampleDiagramUrl("/ability-files/ability_diagram_1")
                .orderNum(1)
                .entryUrl("http://example.com")
                .hidden(0)
                .routePath("/test")
                .aliasName("test-app")
                .requireRelease(0)
                .loadType(1)
                .createTime(new Date())
                .updateBy("admin")
                .updateTime(new Date())
                .build();
        List<AdminAbilityVO> voList = Arrays.asList(vo);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(1L)
                .totalPages(1)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(voList, page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list")
                        .param("curPage", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].abilityType").value(100))
                .andExpect(jsonPath("$.data[0].nameCn").value("测试能力"))
                .andExpect(jsonPath("$.data[0].nameEn").value("TestAbility"))
                .andExpect(jsonPath("$.data[0].descCn").value("中文描述"))
                .andExpect(jsonPath("$.data[0].descEn").value("English Desc"))
                .andExpect(jsonPath("$.data[0].icon").value("ability_icon_1"))
                .andExpect(jsonPath("$.data[0].iconUrl").value("/ability-files/ability_icon_1"))
                .andExpect(jsonPath("$.data[0].exampleDiagram").value("ability_diagram_1"))
                .andExpect(jsonPath("$.data[0].exampleDiagramUrl").value("/ability-files/ability_diagram_1"))
                .andExpect(jsonPath("$.data[0].orderNum").value(1))
                .andExpect(jsonPath("$.data[0].entryUrl").value("http://example.com"))
                .andExpect(jsonPath("$.data[0].hidden").value(0))
                .andExpect(jsonPath("$.data[0].routePath").value("/test"))
                .andExpect(jsonPath("$.data[0].aliasName").value("test-app"))
                .andExpect(jsonPath("$.data[0].requireRelease").value(0))
                .andExpect(jsonPath("$.data[0].loadType").value(1))
                .andExpect(jsonPath("$.page.curPage").value(1))
                .andExpect(jsonPath("$.page.pageSize").value(20))
                .andExpect(jsonPath("$.page.total").value(1));
    }

    @Test
    @DisplayName("GET /ability/admin/list - 关键字搜索中文名")
    void testList_WithKeyword() throws Exception {
        AdminAbilityVO vo = AdminAbilityVO.builder()
                .abilityType(101)
                .nameCn("群置顶")
                .nameEn("GroupPin")
                .descCn("群置顶测试")
                .descEn("Group Pin Test")
                .orderNum(2)
                .createTime(new Date())
                .updateBy("admin")
                .updateTime(new Date())
                .build();
        List<AdminAbilityVO> voList = Arrays.asList(vo);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(1L)
                .totalPages(1)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(voList, page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list")
                        .param("keyword", "群置顶"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].nameCn").value("群置顶"));
    }

    @Test
    @DisplayName("GET /ability/admin/list - 关键字搜索英文名")
    void testList_WithKeywordEn() throws Exception {
        AdminAbilityVO vo = AdminAbilityVO.builder()
                .abilityType(102)
                .nameCn("群通知")
                .nameEn("GroupNotify")
                .descCn("群通知测试")
                .descEn("Group Notify Test")
                .orderNum(3)
                .createTime(new Date())
                .updateBy("admin")
                .updateTime(new Date())
                .build();
        List<AdminAbilityVO> voList = Arrays.asList(vo);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(1L)
                .totalPages(1)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(voList, page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list")
                        .param("keyword", "GroupNotify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].nameEn").value("GroupNotify"));
    }

    @Test
    @DisplayName("GET /ability/admin/list - 空结果应返回空数组")
    void testList_EmptyResult() throws Exception {
        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(0L)
                .totalPages(0)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(Arrays.asList(), page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /ability/admin/list - 翻页参数")
    void testList_WithPagination() throws Exception {
        AdminAbilityVO vo = AdminAbilityVO.builder()
                .abilityType(103)
                .nameCn("链接增强")
                .nameEn("LinkEnhance")
                .descCn("链接增强测试")
                .descEn("Link Enhance Test")
                .orderNum(4)
                .createTime(new Date())
                .updateBy("admin")
                .updateTime(new Date())
                .build();
        List<AdminAbilityVO> voList = Arrays.asList(vo);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(2)
                .pageSize(10)
                .total(11L)
                .totalPages(2)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(voList, page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list")
                        .param("curPage", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.curPage").value(2))
                .andExpect(jsonPath("$.page.pageSize").value(10))
                .andExpect(jsonPath("$.page.total").value(11));
    }

    @Test
    @DisplayName("GET /ability/admin/list - 动态排序参数")
    void testList_WithSort() throws Exception {
        AdminAbilityVO vo = AdminAbilityVO.builder()
                .abilityType(100)
                .nameCn("A开头能力")
                .nameEn("A-Ability")
                .descCn("按名字排序测试")
                .descEn("Sort by name test")
                .orderNum(5)
                .createTime(new Date())
                .updateBy("admin")
                .updateTime(new Date())
                .build();
        List<AdminAbilityVO> voList = Arrays.asList(vo);

        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(1)
                .pageSize(20)
                .total(1L)
                .totalPages(1)
                .build();

        when(adminAbilityService.list(any())).thenReturn(ApiResponse.success(voList, page));

        mockMvc.perform(get("/service/open/v2/ability/admin/list")
                        .param("sortField", "orderNum")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
