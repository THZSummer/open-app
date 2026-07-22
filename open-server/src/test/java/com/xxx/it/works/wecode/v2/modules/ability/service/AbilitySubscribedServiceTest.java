package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.file.service.FileV2Service;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.ability.vo.AppAbilityDetailVO;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AbilityServiceImpl.getSubscribedAbilities() Test")
class AbilitySubscribedServiceTest {

    @Mock
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private AppContextResolver appContextResolver;

    @Mock
    private FileV2Service fileV2Service;

    @InjectMocks
    private AbilityServiceImpl abilityService;

    private static final String TEST_APP_ID = "app_001";
    private static final Long INTERNAL_APP_ID = 100L;

    private Ability presetAbility;
    private Ability type6Ability;
    private Ability customAbility;

    @BeforeEach
    void setUp() {
        presetAbility = new Ability();
        presetAbility.setId(1L);
        presetAbility.setAbilityType(1);
        presetAbility.setAbilityNameCn("群置顶服务");
        presetAbility.setAbilityNameEn("Group Top");
        presetAbility.setOrderNum(1);
        presetAbility.setStatus(1);
        presetAbility.setEntryUrl("http://example.com/group-top");
        presetAbility.setRoutePath("/group-top");
        presetAbility.setAliasName("groupTopApp");
        presetAbility.setRequireRelease(0);
        presetAbility.setLoadType(2);

        type6Ability = new Ability();
        type6Ability.setId(3L);
        type6Ability.setAbilityType(6);
        type6Ability.setAbilityNameCn("应用入群通知");
        type6Ability.setAbilityNameEn("Group Join Notification");
        type6Ability.setOrderNum(3);
        type6Ability.setStatus(1);
        type6Ability.setEntryUrl(null);
        type6Ability.setRoutePath(null);
        type6Ability.setAliasName(null);
        type6Ability.setRequireRelease(0);
        type6Ability.setLoadType(1);

        customAbility = new Ability();
        customAbility.setId(2L);
        customAbility.setAbilityType(100);
        customAbility.setAbilityNameCn("自定义能力");
        customAbility.setAbilityNameEn("Custom Ability");
        customAbility.setOrderNum(2);
        customAbility.setStatus(1);
        customAbility.setEntryUrl(null);
        customAbility.setRoutePath(null);
        customAbility.setAliasName(null);
        customAbility.setRequireRelease(0);
        customAbility.setLoadType(1);
    }

    private AppAbilityRelation createRelation(Long id, Long appId, Long abilityId, Integer abilityType) {
        AppAbilityRelation r = new AppAbilityRelation();
        r.setId(id);
        r.setAppId(appId);
        r.setAbilityId(abilityId);
        r.setAbilityType(abilityType);
        return r;
    }

    @Test
    @DisplayName("正常返回 — 包含新字段映射")
    void testGetSubscribedAbilities_Normal() {
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());

        List<AppAbilityRelation> relations = Arrays.asList(
                createRelation(1L, INTERNAL_APP_ID, 1L, 1)
        );
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(relations);
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility, customAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertEquals(1, result.size());

        AppAbilityDetailVO vo = result.get(0);
        assertEquals("1", vo.getId());
        assertEquals(String.valueOf(1L), vo.getAbilityId());
        assertEquals(Integer.valueOf(1), vo.getAbilityType());
        assertEquals("群置顶服务", vo.getNameCn());
        assertEquals("Group Top", vo.getNameEn());
        assertEquals(Integer.valueOf(1), vo.getOrderNum());
        assertEquals("http://example.com/group-top", vo.getEntryUrl());
        assertEquals("/group-top", vo.getRoutePath());
        assertEquals("groupTopApp", vo.getAliasName());
        assertEquals(Integer.valueOf(0), vo.getRequireRelease());
        assertEquals(Integer.valueOf(2), vo.getLoadType());
    }

    @Test
    @DisplayName("新字段 null 安全 — 实体中为 null 时 VO 中也为 null")
    void testGetSubscribedAbilities_NullSafe() {
        Ability minimalAbility = new Ability();
        minimalAbility.setId(10L);
        minimalAbility.setAbilityType(2);
        minimalAbility.setAbilityNameCn("群通知服务");
        minimalAbility.setAbilityNameEn("Group Notification");
        minimalAbility.setOrderNum(5);
        minimalAbility.setStatus(1);

        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());

        List<AppAbilityRelation> relations = Arrays.asList(
                createRelation(1L, INTERNAL_APP_ID, 10L, 2)
        );
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(relations);
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(minimalAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        AppAbilityDetailVO vo = result.get(0);
        assertNull(vo.getEntryUrl(), "entryUrl 为 null 应返回 null");
        assertNull(vo.getRoutePath(), "routePath 为 null 应返回 null");
        assertNull(vo.getAliasName(), "aliasName 为 null 应返回 null");
        assertNull(vo.getRequireRelease(), "requireRelease 为 null 应返回 null");
        assertNull(vo.getLoadType(), "loadType 为 null 应返回 null");
    }

    @Test
    @DisplayName("type=6 已订阅的不再被过滤 — 应正常返回")
    void testGetSubscribedAbilities_Type6NotFiltered() {
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());

        List<AppAbilityRelation> relations = Arrays.asList(
                createRelation(1L, INTERNAL_APP_ID, 3L, 6)
        );
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(relations);
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility, type6Ability));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertEquals(1, result.size(), "type=6 的已订阅能力应正常返回，不被过滤");
        assertEquals(Integer.valueOf(6), result.get(0).getAbilityType());
    }

    @Test
    @DisplayName("空列表 — 无已订阅能力")
    void testGetSubscribedAbilities_Empty() {
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("按 orderNum 排序")
    void testGetSubscribedAbilities_Ordered() {
        Ability abilityA = new Ability();
        abilityA.setId(1L);
        abilityA.setAbilityType(1);
        abilityA.setAbilityNameCn("能力A");
        abilityA.setAbilityNameEn("Ability A");
        abilityA.setOrderNum(5);
        abilityA.setStatus(1);

        Ability abilityB = new Ability();
        abilityB.setId(2L);
        abilityB.setAbilityType(2);
        abilityB.setAbilityNameCn("能力B");
        abilityB.setAbilityNameEn("Ability B");
        abilityB.setOrderNum(1);
        abilityB.setStatus(1);

        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());

        List<AppAbilityRelation> relations = Arrays.asList(
                createRelation(1L, INTERNAL_APP_ID, 1L, 1),
                createRelation(2L, INTERNAL_APP_ID, 2L, 2)
        );
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(relations);
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(abilityA, abilityB));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("能力B", result.get(0).getNameCn());
        assertEquals("能力A", result.get(1).getNameCn());
    }

    @Test
    @DisplayName("ability 不存在时跳过")
    void testGetSubscribedAbilities_SkipMissingAbility() {
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());

        List<AppAbilityRelation> relations = Arrays.asList(
                createRelation(1L, INTERNAL_APP_ID, 1L, 1),
                createRelation(2L, INTERNAL_APP_ID, 99L, 99)
        );
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID)).thenReturn(relations);
        when(abilityMapper.selectAll()).thenReturn(Arrays.asList(presetAbility));
        when(abilityPropertyMapper.selectByParentIds(anyList())).thenReturn(new ArrayList<>());

        List<AppAbilityDetailVO> result = abilityService.getSubscribedAbilities(TEST_APP_ID);

        assertNotNull(result);
        assertEquals(1, result.size(), "不存在的 ability 应被跳过");
        assertEquals(Integer.valueOf(1), result.get(0).getAbilityType());
    }
}
