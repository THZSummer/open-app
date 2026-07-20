package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityCreateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AdminAbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminAbilityService 创建接口单元测试
 *
 * <p>覆盖正常创建、编码唯一性校验、URL 格式校验、loadType 联动校验。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("AdminAbilityService — 创建能力业务逻辑")
@ExtendWith(MockitoExtension.class)
class AdminAbilityCreateServiceTest {

    private AdminAbilityServiceImpl adminAbilityService;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private CommonFileService commonFileService;

    @Captor
    private ArgumentCaptor<AbilityEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<AbilityProperty> propertyCaptor;

    @BeforeEach
    void setUp() {
        adminAbilityService = new AdminAbilityServiceImpl(abilityMapper, abilityPropertyMapper, commonFileService);
    }

    @Test
    @DisplayName("创建能力 — 正常创建应返回 200")
    void testCreate_Success() {
        // 准备
        AdminAbilityCreateRequest request = buildValidRequest();

        when(abilityMapper.selectByAbilityType(100)).thenReturn(null);
        when(abilityMapper.selectMaxOrderNum()).thenReturn(5);
        when(abilityMapper.insert(any())).thenReturn(1);
        when(abilityPropertyMapper.insert(any())).thenReturn(1);

        // 执行
        ApiResponse<Void> result = adminAbilityService.create(request);

        // 断言
        assertEquals("200", result.getCode());

        verify(abilityMapper).insert(entityCaptor.capture());
        AbilityEntity capturedEntity = entityCaptor.getValue();
        assertEquals(Integer.valueOf(100), capturedEntity.getAbilityType());
        assertEquals("测试能力", capturedEntity.getAbilityNameCn());
        assertEquals("TestAbility", capturedEntity.getAbilityNameEn());
        assertEquals("这是一个测试能力的详细描述", capturedEntity.getAbilityDescCn());
        assertEquals("This is a test ability description", capturedEntity.getAbilityDescEn());
        assertEquals(Integer.valueOf(6), capturedEntity.getOrderNum()); // max(5) + 1
        assertEquals(Integer.valueOf(1), capturedEntity.getStatus());
        assertEquals(Integer.valueOf(0), capturedEntity.getHidden());
        assertEquals(Integer.valueOf(1), capturedEntity.getLoadType());

        verify(abilityPropertyMapper, times(1)).insert(propertyCaptor.capture());
        AbilityProperty capturedProperty = propertyCaptor.getValue();
        assertEquals("icon", capturedProperty.getPropertyName());
        assertEquals("batch_icon_001", capturedProperty.getPropertyValue());
        assertEquals(Integer.valueOf(1), capturedProperty.getStatus());
    }

    @Test
    @DisplayName("创建能力 — 编码唯一性冲突应返回 409")
    void testCreate_DuplicateAbilityType() {
        // 准备
        AdminAbilityCreateRequest request = buildValidRequest();

        AbilityEntity existing = new AbilityEntity();
        existing.setId(1L);
        existing.setAbilityType(100);
        when(abilityMapper.selectByAbilityType(100)).thenReturn(existing);

        // 执行
        ApiResponse<Void> result = adminAbilityService.create(request);

        // 断言
        assertEquals("409", result.getCode());
        assertTrue(result.getMessageZh().contains("编码已被占用"));

        verify(abilityMapper, never()).insert(any());
        verify(abilityPropertyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建能力 — entryUrl 格式不正确应返回 400")
    void testCreate_InvalidEntryUrl() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setEntryUrl("ftp://invalid-protocol.com");

        ApiResponse<Void> result = adminAbilityService.create(request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("访问地址格式不正确"));

        verify(abilityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建能力 — entryUrl 超过1000字符应返回 400")
    void testCreate_EntryUrlTooLong() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setEntryUrl("http://" + "a".repeat(1000));

        ApiResponse<Void> result = adminAbilityService.create(request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("访问地址不能超过1000字符"));

        verify(abilityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建能力 — loadType=2 缺少三要素应返回 400")
    void testCreate_LoadType2MissingFields() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setLoadType(2);
        request.setEntryUrl(null);
        request.setRoutePath(null);
        request.setAliasName(null);

        ApiResponse<Void> result = adminAbilityService.create(request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("三要素必填"));

        verify(abilityMapper, never()).insert(any());
        verify(abilityPropertyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建能力 — loadType=2 三要素完整应成功")
    void testCreate_LoadType2Success() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setLoadType(2);
        request.setEntryUrl("https://subapp.example.com");
        request.setRoutePath("/sub-app");
        request.setAliasName("my-sub-app");

        when(abilityMapper.selectByAbilityType(100)).thenReturn(null);
        when(abilityMapper.selectMaxOrderNum()).thenReturn(null); // 无记录
        when(abilityMapper.insert(any())).thenReturn(1);
        when(abilityPropertyMapper.insert(any())).thenReturn(1);

        ApiResponse<Void> result = adminAbilityService.create(request);

        assertEquals("200", result.getCode());

        verify(abilityMapper).insert(entityCaptor.capture());
        assertEquals(Integer.valueOf(2), entityCaptor.getValue().getLoadType());
        assertEquals("https://subapp.example.com", entityCaptor.getValue().getEntryUrl());
        assertEquals("/sub-app", entityCaptor.getValue().getRoutePath());
        assertEquals("my-sub-app", entityCaptor.getValue().getAliasName());
        assertEquals(Integer.valueOf(1), entityCaptor.getValue().getOrderNum()); // max null → default 1
    }

    @Test
    @DisplayName("创建能力 — 含示意图 batchId 应写两条属性表")
    void testCreate_WithDiagram() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setDiagramBatchId("batch_diagram_001");

        when(abilityMapper.selectByAbilityType(100)).thenReturn(null);
        when(abilityMapper.insert(any())).thenAnswer(invocation -> {
            AbilityEntity entity = invocation.getArgument(0);
            entity.setId(1L); // 模拟 useGeneratedKeys 回填 ID
            return 1;
        });
        when(abilityPropertyMapper.insert(any())).thenReturn(1);

        ApiResponse<Void> result = adminAbilityService.create(request);

        assertEquals("200", result.getCode());

        verify(abilityPropertyMapper, times(2)).insert(any());
    }

    @Test
    @DisplayName("创建能力 — 自定义 orderNum 应使用传入值")
    void testCreate_CustomOrderNum() {
        AdminAbilityCreateRequest request = buildValidRequest();
        request.setOrderNum(10);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(null);
        when(abilityMapper.insert(any())).thenReturn(1);
        when(abilityPropertyMapper.insert(any())).thenReturn(1);

        adminAbilityService.create(request);

        verify(abilityMapper).insert(entityCaptor.capture());
        assertEquals(Integer.valueOf(10), entityCaptor.getValue().getOrderNum());
        // 不应当查询 maxOrderNum
        verify(abilityMapper, never()).selectMaxOrderNum();
    }

    /**
     * 构建有效的创建请求
     */
    private AdminAbilityCreateRequest buildValidRequest() {
        AdminAbilityCreateRequest request = new AdminAbilityCreateRequest();
        request.setAbilityType(100);
        request.setNameCn("测试能力");
        request.setNameEn("TestAbility");
        request.setDescCn("这是一个测试能力的详细描述");
        request.setDescEn("This is a test ability description");
        request.setIconBatchId("batch_icon_001");
        return request;
    }
}
