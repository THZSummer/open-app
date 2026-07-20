package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AdminAbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminAbilityService 列表接口单元测试
 *
 * <p>覆盖分页查询、关键字搜索、排序校验、属性表关联查询。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("AdminAbilityService — 能力列表业务逻辑")
@ExtendWith(MockitoExtension.class)
class AdminAbilityListServiceTest {

    private AdminAbilityServiceImpl adminAbilityService;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private CommonFileService commonFileService;

    @BeforeEach
    void setUp() {
        adminAbilityService = new AdminAbilityServiceImpl(abilityMapper, abilityPropertyMapper, commonFileService);
    }

    @Test
    @DisplayName("列表查询 — 正常分页返回数据")
    void testList_Success() {
        // 准备数据
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setCurPage(1);
        request.setPageSize(20);

        List<AbilityEntity> entities = createMockEntities();
        when(abilityMapper.countByKeyword(null)).thenReturn(1L);
        when(abilityMapper.selectPage(null, "order_num", "asc", 0, 20))
                .thenReturn(entities);

        // 模拟属性表返回
        when(commonFileService.getShowUrl("ability_icon_1")).thenReturn("/ability-files/ability_icon_1");
        when(commonFileService.getShowUrl("ability_diagram_1")).thenReturn("/ability-files/ability_diagram_1");

        List<AbilityProperty> properties = createMockProperties(1L);
        when(abilityPropertyMapper.selectByParentIds(anyList()))
                .thenReturn(properties);

        // 执行
        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        // 断言
        assertEquals("200", result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());

        AdminAbilityVO vo = result.getData().get(0);
        assertEquals(100, vo.getAbilityType());
        assertEquals("测试能力", vo.getNameCn());
        assertEquals("TestAbility", vo.getNameEn());
        assertEquals("中文描述", vo.getDescCn());
        assertEquals("English Desc", vo.getDescEn());
        assertEquals("ability_icon_1", vo.getIcon());
        assertEquals("/ability-files/ability_icon_1", vo.getIconUrl());
        assertEquals("ability_diagram_1", vo.getExampleDiagram());
        assertEquals("/ability-files/ability_diagram_1", vo.getExampleDiagramUrl());
        assertEquals(1, vo.getOrderNum());
        assertEquals("http://example.com", vo.getEntryUrl());
        assertEquals(0, vo.getHidden());
        assertEquals("/test", vo.getRoutePath());
        assertEquals("test-app", vo.getAliasName());
        assertEquals(0, vo.getRequireRelease());
        assertEquals(1, vo.getLoadType());

        // 分页信息
        assertNotNull(result.getPage());
        assertEquals(1, result.getPage().getCurPage());
        assertEquals(20, result.getPage().getPageSize());
        assertEquals(1L, result.getPage().getTotal());
    }

    @Test
    @DisplayName("列表查询 — 关键字搜索按中文名")
    void testList_KeywordSearch() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setCurPage(1);
        request.setPageSize(20);
        request.setKeyword("测试");

        List<AbilityEntity> entities = createMockEntities();
        when(abilityMapper.countByKeyword("测试")).thenReturn(1L);
        when(abilityMapper.selectPage("测试", "order_num", "asc", 0, 20))
                .thenReturn(entities);
        when(abilityPropertyMapper.selectByParentIds(anyList()))
                .thenReturn(createMockProperties(1L));

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("200", result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("测试能力", result.getData().get(0).getNameCn());
    }

    @Test
    @DisplayName("列表查询 — 动态排序（orderNum desc）")
    void testList_SortByOrderNumDesc() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setCurPage(1);
        request.setPageSize(20);
        request.setSortField("order_num");
        request.setSortOrder("desc");

        List<AbilityEntity> entities = createMockEntities();
        when(abilityMapper.countByKeyword(null)).thenReturn(1L);
        when(abilityMapper.selectPage(null, "order_num", "desc", 0, 20))
                .thenReturn(entities);
        when(abilityPropertyMapper.selectByParentIds(anyList()))
                .thenReturn(createMockProperties(1L));

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("200", result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("列表查询 — 非法的排序字段应返回 400")
    void testList_InvalidSortField() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setSortField("evil_column");

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("不支持的排序字段"));
        verify(abilityMapper, never()).countByKeyword(any());
        verify(abilityMapper, never()).selectPage(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("列表查询 — 非法的排序方向应返回 400")
    void testList_InvalidSortOrder() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setSortField("order_num");
        request.setSortOrder("evil");

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("不支持的排序方向"));
        verify(abilityMapper, never()).countByKeyword(any());
        verify(abilityMapper, never()).selectPage(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("列表查询 — 空结果返回空列表")
    void testList_EmptyResult() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setCurPage(1);
        request.setPageSize(20);

        when(abilityMapper.countByKeyword(null)).thenReturn(0L);
        when(abilityMapper.selectPage(null, "order_num", "asc", 0, 20))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("200", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
        assertEquals(0L, result.getPage().getTotal());
    }

    @Test
    @DisplayName("列表查询 — 驼峰排序字段自动转换下划线")
    void testList_CamelCaseSortField() {
        AdminAbilityListRequest request = new AdminAbilityListRequest();
        request.setSortField("abilityNameCn");
        request.setCurPage(1);
        request.setPageSize(20);

        List<AbilityEntity> entities = createMockEntities();
        when(abilityMapper.countByKeyword(null)).thenReturn(1L);
        when(abilityMapper.selectPage(null, "ability_name_cn", "asc", 0, 20))
                .thenReturn(entities);
        when(abilityPropertyMapper.selectByParentIds(anyList()))
                .thenReturn(createMockProperties(1L));

        ApiResponse<List<AdminAbilityVO>> result = adminAbilityService.list(request);

        assertEquals("200", result.getCode());
        // 验证请求的 sortField 被转换
        assertEquals("ability_name_cn", request.getSortField());
    }

    /**
     * 创建测试用的 AbilityEntity 列表
     */
    private List<AbilityEntity> createMockEntities() {
        AbilityEntity entity = new AbilityEntity();
        entity.setId(1L);
        entity.setAbilityNameCn("测试能力");
        entity.setAbilityNameEn("TestAbility");
        entity.setAbilityDescCn("中文描述");
        entity.setAbilityDescEn("English Desc");
        entity.setAbilityType(100);
        entity.setOrderNum(1);
        entity.setEntryUrl("http://example.com");
        entity.setHidden(0);
        entity.setRoutePath("/test");
        entity.setAliasName("test-app");
        entity.setRequireRelease(0);
        entity.setLoadType(1);
        entity.setStatus(1);
        entity.setCreateBy("admin");
        entity.setCreateTime(new Date());
        entity.setLastUpdateBy("admin");
        entity.setLastUpdateTime(new Date());
        return Arrays.asList(entity);
    }

    /**
     * 创建测试用的 AbilityProperty 列表
     */
    private List<AbilityProperty> createMockProperties(Long parentId) {
        AbilityProperty iconProp = new AbilityProperty();
        iconProp.setId(1L);
        iconProp.setParentId(parentId);
        iconProp.setPropertyName("icon");
        iconProp.setPropertyValue("ability_icon_1");
        iconProp.setStatus(1);

        AbilityProperty diagramProp = new AbilityProperty();
        diagramProp.setId(2L);
        diagramProp.setParentId(parentId);
        diagramProp.setPropertyName("example_diagram");
        diagramProp.setPropertyValue("ability_diagram_1");
        diagramProp.setStatus(1);

        return Arrays.asList(iconProp, diagramProp);
    }
}
