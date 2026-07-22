package com.xxx.it.works.wecode.v2.modules.version.service.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.version.constants.VersionPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.entity.VersionProperty;
import com.xxx.it.works.wecode.v2.modules.version.mapper.AppVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VersionServiceImpl.createVersion() 过滤逻辑单元测试
 *
 * <p>验证：
 * <ul>
 *   <li>requireRelease=1 的能力被纳入版本发布检查</li>
 *   <li>requireRelease=0 的能力被跳过</li>
 *   <li>type=6（应用入群通知）requireRelease=0，行为与旧硬编码一致</li>
 *   <li>空订阅列表返回空字符串</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VersionServiceImpl.createVersion() Filter Test")
class VersionServiceImplTest {

    @Mock
    private AppVersionMapper appVersionMapper;

    @Mock
    private AppContextResolver appContextResolver;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @Mock
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Mock
    private AbilityMapper abilityMapper;

    @InjectMocks
    private VersionServiceImpl versionService;

    @Captor
    private ArgumentCaptor<VersionProperty> propertyCaptor;

    private static final String TEST_APP_ID = "app_001";
    private static final Long INTERNAL_APP_ID = 100L;
    private static final Long VERSION_ID = 500L;
    private static final String VERSION_CODE = "1.0.0";

    private Ability abilityRequireRelease1;
    private Ability abilityRequireRelease0;
    private Ability abilityType6;

    @BeforeEach
    void setUp() {
        // requireRelease=1 的能力（应将 ID 纳入版本）
        abilityRequireRelease1 = new Ability();
        abilityRequireRelease1.setId(10L);
        abilityRequireRelease1.setAbilityType(1);
        abilityRequireRelease1.setAbilityNameCn("群置顶服务");
        abilityRequireRelease1.setRequireRelease(1);

        // requireRelease=0 的能力（应跳过）
        abilityRequireRelease0 = new Ability();
        abilityRequireRelease0.setId(20L);
        abilityRequireRelease0.setAbilityType(2);
        abilityRequireRelease0.setAbilityNameCn("群通知服务");
        abilityRequireRelease0.setRequireRelease(0);

        // type=6（应用入群通知），requireRelease=0（默认值），应被排除 — 等价旧逻辑
        abilityType6 = new Ability();
        abilityType6.setId(30L);
        abilityType6.setAbilityType(6);
        abilityType6.setAbilityNameCn("应用入群通知");
        abilityType6.setRequireRelease(0);
    }

    @Test
    @DisplayName("requireRelease=1 的能力被纳入版本发布检查")
    void testCreateVersion_FilterIncludeRequireRelease1() {
        // Given: 两个订阅关系，其中一个 requireRelease=1，另一个 requireRelease=0
        AppAbilityRelation relation1 = new AppAbilityRelation();
        relation1.setId(1L);
        relation1.setAppId(INTERNAL_APP_ID);
        relation1.setAbilityId(10L);
        relation1.setAbilityType(1);

        AppAbilityRelation relation2 = new AppAbilityRelation();
        relation2.setId(2L);
        relation2.setAppId(INTERNAL_APP_ID);
        relation2.setAbilityId(20L);
        relation2.setAbilityType(2);

        mockCreateVersionCommon();
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID))
                .thenReturn(Arrays.asList(relation1, relation2));
        when(abilityMapper.selectAll())
                .thenReturn(Arrays.asList(abilityRequireRelease1, abilityRequireRelease0));

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            versionService.createVersion(TEST_APP_ID, createRequest());

            // Then: 仅 requireRelease=1 的 abilityId 被写入
            verify(appVersionMapper).insertProperty(propertyCaptor.capture());
            VersionProperty prop = propertyCaptor.getValue();
            assertEquals(VersionPropertyConstants.PROP_ABILITY_IDS, prop.getPropertyName());
            // 只有 abilityId=10 被纳入（requireRelease=1），20 被跳过
            assertEquals("10", prop.getPropertyValue());
        }
    }

    @Test
    @DisplayName("requireRelease=0 的能力被跳过")
    void testCreateVersion_FilterSkipRequireRelease0() {
        // Given: 所有订阅关系的 requireRelease 均为 0
        AppAbilityRelation relation = new AppAbilityRelation();
        relation.setId(1L);
        relation.setAppId(INTERNAL_APP_ID);
        relation.setAbilityId(20L);
        relation.setAbilityType(2);

        mockCreateVersionCommon();
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID))
                .thenReturn(Arrays.asList(relation));
        when(abilityMapper.selectAll())
                .thenReturn(Arrays.asList(abilityRequireRelease0));

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            versionService.createVersion(TEST_APP_ID, createRequest());

            // Then: 没有 requireRelease=1 的能力，写入空字符串
            verify(appVersionMapper).insertProperty(propertyCaptor.capture());
            VersionProperty prop = propertyCaptor.getValue();
            assertEquals("", prop.getPropertyValue());
        }
    }

    @Test
    @DisplayName("type=6 requireRelease=0 被跳过（等价旧行为）")
    void testCreateVersion_FilterSkipType6() {
        // Given: 仅订阅了 type=6 的能力（requireRelease=0）
        AppAbilityRelation relation = new AppAbilityRelation();
        relation.setId(1L);
        relation.setAppId(INTERNAL_APP_ID);
        relation.setAbilityId(30L);
        relation.setAbilityType(6);

        mockCreateVersionCommon();
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID))
                .thenReturn(Arrays.asList(relation));
        when(abilityMapper.selectAll())
                .thenReturn(Arrays.asList(abilityType6));

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            versionService.createVersion(TEST_APP_ID, createRequest());

            // Then: type=6 能力被排除（等价旧行为），写入空字符串
            verify(appVersionMapper).insertProperty(propertyCaptor.capture());
            VersionProperty prop = propertyCaptor.getValue();
            assertEquals("", prop.getPropertyValue());
        }
    }

    @Test
    @DisplayName("空订阅列表 — 能力 ID 列表为空字符串")
    void testCreateVersion_EmptyRelations() {
        // Given: 应用未订阅任何能力
        mockCreateVersionCommon();
        when(appAbilityRelationMapper.selectByAppId(INTERNAL_APP_ID))
                .thenReturn(new ArrayList<>());
        when(abilityMapper.selectAll())
                .thenReturn(new ArrayList<>());

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            versionService.createVersion(TEST_APP_ID, createRequest());

            // Then: 空列表，写入空字符串
            verify(appVersionMapper).insertProperty(propertyCaptor.capture());
            VersionProperty prop = propertyCaptor.getValue();
            assertEquals("", prop.getPropertyValue());
        }
    }

    /**
     * 通用 mock 设置：让校验和版本创建通过
     */
    private void mockCreateVersionCommon() {
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        // 版本号唯一性校验通过
        when(appVersionMapper.selectByAppIdAndVersionCode(INTERNAL_APP_ID, VERSION_CODE))
                .thenReturn(null);
        // 版本号递增校验通过
        when(appVersionMapper.selectLatestVersionCodeExcludeId(INTERNAL_APP_ID, null))
                .thenReturn(null);
        // 无待发布/审批中的版本
        when(appVersionMapper.selectByAppIdAndStatuses(eq(INTERNAL_APP_ID), anyList()))
                .thenReturn(new ArrayList<>());
        // ID 生成
        when(idGenerator.nextId()).thenReturn(VERSION_ID);
        // 版本插入
        when(appVersionMapper.insert(any())).thenReturn(1);
    }

    /**
     * 创建默认的 CreateVersionRequest
     */
    private CreateVersionRequest createRequest() {
        CreateVersionRequest request = new CreateVersionRequest();
        request.setVersionCode(VERSION_CODE);
        request.setVersionDescCn("测试版本");
        request.setVersionDescEn("Test Version");
        return request;
    }
}
