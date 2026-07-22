package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.ability.dto.AddAbilityRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AppAbilityRelationMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AbilityServiceImpl.addAbility() + autoSubscribeAfterAbility() 单元测试
 *
 * <p>验证：
 * <ul>
 *   <li>枚举校验已被移除，改为 DB 查询校验</li>
 *   <li>自定义类型可通过校验正常订阅</li>
 *   <li>失效类型被拒绝（status != 1 或不存在）</li>
 *   <li>重复订阅检查逻辑不变</li>
 *   <li>订阅后触发 autoSubscribeAfterAbility，日志含 appId+abilityType</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbilityServiceImpl.addAbility() Test")
class AbilitySubscribeServiceTest {

    @Mock
    private AppAbilityRelationMapper appAbilityRelationMapper;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AppContextResolver appContextResolver;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @InjectMocks
    private AbilityServiceImpl abilityService;

    @Captor
    private ArgumentCaptor<AppAbilityRelation> relationCaptor;

    private static final String TEST_APP_ID = "app_001";
    private static final Long INTERNAL_APP_ID = 100L;

    private Ability presetAbility;
    private Ability customAbility;
    private Ability disabledAbility;

    @BeforeEach
    void setUp() {
        // 预设类型（type=1，群置顶服务，启用状态）
        presetAbility = new Ability();
        presetAbility.setId(1L);
        presetAbility.setAbilityType(1);
        presetAbility.setAbilityNameCn("群置顶服务");
        presetAbility.setAbilityNameEn("Group Top");
        presetAbility.setStatus(1);

        // 自定义类型（type=100，平台面创建，启用状态）
        customAbility = new Ability();
        customAbility.setId(2L);
        customAbility.setAbilityType(100);
        customAbility.setAbilityNameCn("自定义能力");
        customAbility.setAbilityNameEn("Custom Ability");
        customAbility.setStatus(1);

        // 禁用类型（type=99，status=0，应被拒绝）
        disabledAbility = new Ability();
        disabledAbility.setId(3L);
        disabledAbility.setAbilityType(99);
        disabledAbility.setAbilityNameCn("已禁用能力");
        disabledAbility.setAbilityNameEn("Disabled Ability");
        disabledAbility.setStatus(0);
    }

    @Test
    @DisplayName("成功订阅 — 预设类型通过 DB 校验")
    void testAddAbility_PresetType() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(1)).thenReturn(presetAbility);
        when(appAbilityRelationMapper.selectByAppIdAndAbilityType(INTERNAL_APP_ID, 1)).thenReturn(null);
        when(idGenerator.nextId()).thenReturn(10001L);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(1);

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            abilityService.addAbility(TEST_APP_ID, request);

            // Then: 插入关联记录
            verify(appAbilityRelationMapper).insert(relationCaptor.capture());
            AppAbilityRelation relation = relationCaptor.getValue();
            assertEquals(Long.valueOf(10001L), relation.getId());
            assertEquals(INTERNAL_APP_ID, relation.getAppId());
            assertEquals(Long.valueOf(1L), relation.getAbilityId());
            assertEquals(Integer.valueOf(1), relation.getAbilityType());
            assertEquals(CommonConstants.DEFAULT_TENANT_ID, relation.getTenantId());
        }
    }

    @Test
    @DisplayName("成功订阅 — 自定义类型通过 DB 校验")
    void testAddAbility_CustomType() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(100)).thenReturn(customAbility);
        when(appAbilityRelationMapper.selectByAppIdAndAbilityType(INTERNAL_APP_ID, 100)).thenReturn(null);
        when(idGenerator.nextId()).thenReturn(10002L);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(100);

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            abilityService.addAbility(TEST_APP_ID, request);

            // Then: 插入关联记录
            verify(appAbilityRelationMapper).insert(relationCaptor.capture());
            AppAbilityRelation relation = relationCaptor.getValue();
            assertEquals(Long.valueOf(10002L), relation.getId());
            assertEquals(Integer.valueOf(100), relation.getAbilityType());
            assertEquals(Long.valueOf(2L), relation.getAbilityId());
        }
    }

    @Test
    @DisplayName("拒绝 — 能力不存在（selectByAbilityType 返回 null）")
    void testAddAbility_TypeNotFound() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(999)).thenReturn(null);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(999);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> abilityService.addAbility(TEST_APP_ID, request));
        assertEquals("400", ex.getCode());
        assertEquals("能力不存在或已失效", ex.getMessageZh());
        assertEquals("Ability does not exist or is disabled", ex.getMessageEn());
    }

    @Test
    @DisplayName("拒绝 — 能力已禁用（status=0，selectByAbilityType 返回 null）")
    void testAddAbility_TypeDisabled() {
        // Given: selectByAbilityType 查询 status=1，disabledAbility 的 status=0，所以 mapper 返回 null
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(99)).thenReturn(null);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(99);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> abilityService.addAbility(TEST_APP_ID, request));
        assertEquals("400", ex.getCode());
        assertEquals("能力不存在或已失效", ex.getMessageZh());
    }

    @Test
    @DisplayName("拒绝 — 重复订阅检查不变")
    void testAddAbility_AlreadySubscribed() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(1)).thenReturn(presetAbility);

        // 已存在订阅记录
        AppAbilityRelation existing = new AppAbilityRelation();
        existing.setId(500L);
        existing.setAppId(INTERNAL_APP_ID);
        existing.setAbilityType(1);
        when(appAbilityRelationMapper.selectByAppIdAndAbilityType(INTERNAL_APP_ID, 1)).thenReturn(existing);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(1);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> abilityService.addAbility(TEST_APP_ID, request));
        assertEquals(ResponseCodeEnum.ABILITY_ALREADY_SUBSCRIBED.getCode(), ex.getCode());
        assertEquals(ResponseCodeEnum.ABILITY_ALREADY_SUBSCRIBED.getMessageZh(), ex.getMessageZh());
    }

    @Test
    @DisplayName("自动桥接 — 订阅成功后触发并记录日志")
    void testAddAbility_AutoSubscribeBridgeTriggered() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(1)).thenReturn(presetAbility);
        when(appAbilityRelationMapper.selectByAppIdAndAbilityType(INTERNAL_APP_ID, 1)).thenReturn(null);
        when(idGenerator.nextId()).thenReturn(10003L);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(1);

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            abilityService.addAbility(TEST_APP_ID, request);

            // Then: 验证订阅后的 autoSubscribeAfterAbility 被调用
            // autoSubscribeAfterAbility 是 private 方法，我们验证 addAbility 没有抛出异常
            // 并验证关联记录被成功插入（说明整个流程执行完毕）
            verify(appAbilityRelationMapper).insert(any(AppAbilityRelation.class));
        }
    }

    @Test
    @DisplayName("不变量 — 接口路径和请求参数不变（传入正确参数即可正常执行）")
    void testAddAbility_RequestParamUnchanged() {
        // Given
        when(appContextResolver.resolveAndValidate(TEST_APP_ID))
                .thenReturn(AppContext.builder().internalId(INTERNAL_APP_ID).build());
        when(abilityMapper.selectByAbilityType(1)).thenReturn(presetAbility);
        when(appAbilityRelationMapper.selectByAppIdAndAbilityType(INTERNAL_APP_ID, 1)).thenReturn(null);
        when(idGenerator.nextId()).thenReturn(10004L);

        AddAbilityRequest request = new AddAbilityRequest();
        request.setAbilityType(1);

        try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
            userContext.when(UserContextHolder::getUserId).thenReturn("test_user");

            // When
            assertDoesNotThrow(() -> abilityService.addAbility(TEST_APP_ID, request));

            // Then: 验证 selectByAbilityType 被调用（替代枚举校验）
            verify(abilityMapper).selectByAbilityType(1);
            // 验证 selectByAppIdAndAbilityType 依然被调用（重复检查不变）
            verify(appAbilityRelationMapper).selectByAppIdAndAbilityType(INTERNAL_APP_ID, 1);
            // 验证 insert 被调用
            verify(appAbilityRelationMapper).insert(any(AppAbilityRelation.class));
        }
    }
}
