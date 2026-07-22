package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.modules.commonfile.service.CommonFileService;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AbilityVO;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AbilityServiceImpl.getAbilityList() 单元测试
 *
 * <p>验证：
 * <ul>
 *   <li>hidden=1 的能力不返回（由 Mapper XML 过滤，本处验证 mapper 返回的数据被正确处理）</li>
 *   <li>新增 5 字段正确映射到 VO</li>
 *   <li>自定义类型（≥100）正常返回</li>
 *   <li>已订阅标记逻辑不变</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbilityServiceImpl.getAbilityList() Test")
class AbilityListServiceTest {

    @Mock
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private AppContextResolver appContextResolver;

    @Mock
    private CommonFileService commonFileService;

    @InjectMocks
    private AbilityServiceImpl abilityService;

    private static final String TEST_APP_ID = "app_001";
    private static final Long INTERNAL_APP_ID = 100L;

    private Ability presetAbility;
    private Ability customAbility;
    private Ability hiddenAbility;

    @BeforeEach
    void setUp() {
        // 预设类型（type=1，群置顶服务）
        presetAbility = new Ability();
        presetAbility.setId(1L);
        presetAbility.setAbilityType(1);
        presetAbility.setAbilityNameCn("群置顶服务");
        presetAbility.setAbilityNameEn("Group Top");
        presetAbility.setAbilityDescCn("群置顶服务描述");
        presetAbility.setAbilityDescEn("Group top description");
        presetAbility.setOrderNum(1);
        presetAbility.setStatus(1);
        presetAbility.setHidden(0);
        presetAbility.setEntryUrl("http://example.com/group-top");
        presetAbility.setRoutePath("/group-top");
        presetAbility.setAliasName("groupTopApp");
        presetAbility.setRequireRelease(0);
        presetAbility.setLoadType(2);

        // 自定义类型（type=100，平台面创建）
        customAbility = new Ability();
        customAbility.setId(2L);
        customAbility.setAbilityType(100);
        customAbility.setAbilityNameCn("自定义能力");
        customAbility.setAbilityNameEn("Custom Ability");
        customAbility.setAbilityDescCn("自定义能力描述");
        customAbility.setAbilityDescEn("Custom ability description");
        customAbility.setOrderNum(2);
        customAbility.setStatus(1);
        customAbility.setHidden(0);
        customAbility.setEntryUrl(null);
        customAbility.setRoutePath(null);
        customAbility.setAliasName(null);
        customAbility.setRequireRelease(0);
        customAbility.setLoadType(1);

        // 隐藏类型（hidden=1，不应出现在结果中，但在某些场景中可能存在）
        hiddenAbility = new Ability();
        hiddenAbility.setId(3L);
        hiddenAbility.setAbilityType(6);
        hiddenAbility.setAbilityNameCn("应用入群通知");
        hiddenAbility.setAbilityNameEn("Group Join Notification");
        hiddenAbility.setOrderNum(3);
        hiddenAbility.setStatus(1);
        hiddenAbility.setHidden(1);
        hiddenAbility.setRequireRelease(0);
        hiddenAbility.setLoadType(1);
    }

    @Test
    @DisplayName("正常返回 — 预设类型 + 自定义类型，无 hidden 类型")
    void testGetAbilityList_Normal() {
        // Given: mapper 只返回 visible 的能力（hidden=0 已在 SQL 层过滤）
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility, customAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证预设类型
        AbilityVO vo1 = result.get(0);
        assertEquals("1", vo1.getAbilityId());
        assertEquals(Integer.valueOf(1), vo1.getAbilityType());
        assertEquals("群置顶服务", vo1.getNameCn());
        assertEquals("Group Top", vo1.getNameEn());
        assertEquals("http://example.com/group-top", vo1.getEntryUrl());
        assertEquals("/group-top", vo1.getRoutePath());
        assertEquals("groupTopApp", vo1.getAliasName());
        assertEquals(Integer.valueOf(0), vo1.getRequireRelease());
        assertEquals(Integer.valueOf(2), vo1.getLoadType());
        assertFalse(vo1.getSubscribed());

        // 验证自定义类型
        AbilityVO vo2 = result.get(1);
        assertEquals("2", vo2.getAbilityId());
        assertEquals(Integer.valueOf(100), vo2.getAbilityType());
        assertEquals("自定义能力", vo2.getNameCn());
        assertNull(vo2.getEntryUrl());
        assertNull(vo2.getRoutePath());
        assertNull(vo2.getAliasName());
        assertEquals(Integer.valueOf(0), vo2.getRequireRelease());
        assertEquals(Integer.valueOf(1), vo2.getLoadType());
    }

    @Test
    @DisplayName("已订阅标记 — 订阅的能力标记为 true")
    void testGetAbilityList_SubscribedMark() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility, customAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        // 应用已订阅 presetAbility (type=1)
        AppAbilityRelation relation = new AppAbilityRelation();
        relation.setId(1L);
        relation.setAppId(INTERNAL_APP_ID);
        relation.setAbilityId(1L);
        relation.setAbilityType(1);
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID))
                .thenReturn(Arrays.asList(relation));

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getSubscribed(), "已订阅的能力应标记为 true");
        assertFalse(result.get(1).getSubscribed(), "未订阅的能力应标记为 false");
    }

    @Test
    @DisplayName("自定义类型（type=100）正常返回")
    void testGetAbilityList_CustomType() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(customAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(100), result.get(0).getAbilityType());
        assertEquals("自定义能力", result.get(0).getNameCn());
    }

    @Test
    @DisplayName("空列表 — 无可用的能力")
    void testGetAbilityList_Empty() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(new ArrayList<>());
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("属性（icon/diagram）正确映射")
    void testGetAbilityList_WithProperties() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility));
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        // Mock 属性查询
        AbilityProperty iconProp = new AbilityProperty();
        iconProp.setId(1L);
        iconProp.setParentId(1L);
        iconProp.setPropertyName("icon");
        iconProp.setPropertyValue("file_001");

        AbilityProperty diagramProp = new AbilityProperty();
        diagramProp.setId(2L);
        diagramProp.setParentId(1L);
        diagramProp.setPropertyName("example_diagram");
        diagramProp.setPropertyValue("file_002");

        when(abilityPropertyMapper.selectByParentIds(anyList()))
                .thenReturn(Arrays.asList(iconProp, diagramProp));
        when(commonFileService.getShowUrl(anyString()))
                .thenAnswer(invocation -> "http://file.example.com/" + invocation.getArgument(0));

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("http://file.example.com/file_001", result.get(0).getIconUrl());
        assertEquals("http://file.example.com/file_002", result.get(0).getDiagramUrl());
    }

    @Test
    @DisplayName("新字段 null 安全 — 实体中为 null 时 VO 中也为 null")
    void testGetAbilityList_NullSafe() {
        // Given: 能力无 entryUrl/routePath/aliasName
        Ability minimalAbility = new Ability();
        minimalAbility.setId(10L);
        minimalAbility.setAbilityType(2);
        minimalAbility.setAbilityNameCn("群通知服务");
        minimalAbility.setAbilityNameEn("Group Notification");
        minimalAbility.setOrderNum(5);
        minimalAbility.setStatus(1);
        minimalAbility.setHidden(0);
        // entryUrl, routePath, aliasName, requireRelease, loadType 均为 null

        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(minimalAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        // When
        List<AbilityVO> result = abilityService.getAbilityList(TEST_APP_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        AbilityVO vo = result.get(0);
        assertNull(vo.getEntryUrl(), "entryUrl 为 null 应返回 null");
        assertNull(vo.getRoutePath(), "routePath 为 null 应返回 null");
        assertNull(vo.getAliasName(), "aliasName 为 null 应返回 null");
        assertNull(vo.getRequireRelease(), "requireRelease 为 null 应返回 null");
        assertNull(vo.getLoadType(), "loadType 为 null 应返回 null");
    }
}
