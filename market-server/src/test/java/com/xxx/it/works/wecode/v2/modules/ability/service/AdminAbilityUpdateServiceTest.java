package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.common.AbilityPropertyEnum;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityUpdateRequest;
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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * AdminAbilityService 编辑接口单元测试
 *
 * <p>覆盖正常更新、404、loadType 联动校验、乐观锁冲突、图标更新等场景。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("AdminAbilityService — 编辑能力业务逻辑")
@ExtendWith(MockitoExtension.class)
class AdminAbilityUpdateServiceTest {

    private AdminAbilityServiceImpl adminAbilityService;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private CommonFileService commonFileService;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @Captor
    private ArgumentCaptor<AbilityEntity> entityCaptor;

    private Date lastUpdateTime;
    private AbilityEntity existingEntity;

    @BeforeEach
    void setUp() {
        adminAbilityService = new AdminAbilityServiceImpl(
                abilityMapper, abilityPropertyMapper, commonFileService, idGenerator);
        lenient().when(idGenerator.nextId()).thenReturn(1L);

        lastUpdateTime = new Date(1689000000000L);
        existingEntity = buildExistingEntity();
    }

    @Test
    @DisplayName("编辑能力 — 更新名称应成功")
    void testUpdate_Success_NameCn() {
        // 准备
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setNameCn("新中文名");
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        // 执行
        ApiResponse<Void> result = adminAbilityService.update(100, request);

        // 断言
        assertEquals("200", result.getCode());

        verify(abilityMapper).updateByPrimaryKeySelective(entityCaptor.capture());
        AbilityEntity captured = entityCaptor.getValue();
        assertEquals(Long.valueOf(1L), captured.getId());
        assertEquals("新中文名", captured.getAbilityNameCn());
        // 未传的字段应为 null（updateByPrimaryKeySelective 不会更新 null 字段）
        assertNull(captured.getAbilityNameEn());
        assertNull(captured.getAbilityDescCn());
        assertNull(captured.getOrderNum());
    }

    @Test
    @DisplayName("编辑能力 — id 不存在应返回 404")
    void testUpdate_NotFound() {
        // 准备
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setNameCn("新名称");

        when(abilityMapper.selectByAbilityType(999)).thenReturn(null);

        // 执行
        ApiResponse<Void> result = adminAbilityService.update(999, request);

        // 断言
        assertEquals("404", result.getCode());
        assertTrue(result.getMessageZh().contains("不存在"));

        verify(abilityMapper, never()).updateByPrimaryKeySelective(any());
        verify(abilityPropertyMapper, never()).insert(any());
        verify(abilityPropertyMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("编辑能力 — entryUrl 格式不正确应返回 400")
    void testUpdate_InvalidEntryUrl() {
        // 准备
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setEntryUrl("ftp://invalid-protocol.com");
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);

        // 执行
        ApiResponse<Void> result = adminAbilityService.update(100, request);

        // 断言
        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("访问地址格式不正确"));

        verify(abilityMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("编辑能力 — entryUrl 超过1000字符应返回 400")
    void testUpdate_EntryUrlTooLong() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setEntryUrl("http://" + "a".repeat(1000));
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("不能超过1000字符"));

        verify(abilityMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("编辑能力 — loadType 改为 2 缺少三要素应返回 400")
    void testUpdate_LoadType2MissingFields() {
        // 准备
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setLoadType(2);
        // 不传 entryUrl/routePath/aliasName
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);

        // 执行（数据库原有数据不满足 loadType=2 三要素）
        ApiResponse<Void> result = adminAbilityService.update(100, request);

        // 断言
        assertEquals("400", result.getCode());
        assertTrue(result.getMessageZh().contains("三要素必填"));

        verify(abilityMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("编辑能力 — loadType 改为 2 且三要素完整应成功")
    void testUpdate_LoadType2Success() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setLoadType(2);
        request.setEntryUrl("https://subapp.example.com");
        request.setRoutePath("/sub-app");
        request.setAliasName("my-sub-app");
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("200", result.getCode());

        verify(abilityMapper).updateByPrimaryKeySelective(entityCaptor.capture());
        assertEquals(Integer.valueOf(2), entityCaptor.getValue().getLoadType());
        assertEquals("https://subapp.example.com", entityCaptor.getValue().getEntryUrl());
        assertEquals("/sub-app", entityCaptor.getValue().getRoutePath());
        assertEquals("my-sub-app", entityCaptor.getValue().getAliasName());
    }

    @Test
    @DisplayName("编辑能力 — 更新图标 batchId 应 upsert 属性表")
    void testUpdate_IconBatchId() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setIconBatchId("new_icon_batch");
        request.setLastUpdateTime(lastUpdateTime);

        AbilityProperty existingIcon = new AbilityProperty();
        existingIcon.setId(10L);
        existingIcon.setParentId(1L);
        existingIcon.setPropertyName(AbilityPropertyEnum.ICON.getPropertyName());
        existingIcon.setPropertyValue("old_icon_batch");

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(abilityPropertyMapper.selectByParentIdAndPropertyName(1L,
                AbilityPropertyEnum.ICON.getPropertyName())).thenReturn(existingIcon);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("200", result.getCode());

        // 验证更新了已有属性（update 而非 insert）
        verify(abilityPropertyMapper).updateByPrimaryKeySelective(any());
        verify(abilityPropertyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("编辑能力 — 更新示意图 batchId 应 upsert 属性表")
    void testUpdate_DiagramBatchId() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setDiagramBatchId("new_diagram_batch");
        request.setLastUpdateTime(lastUpdateTime);

        // 无已有示意图 → 应 insert
        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(abilityPropertyMapper.selectByParentIdAndPropertyName(1L,
                AbilityPropertyEnum.EXAMPLE_DIAGRAM.getPropertyName())).thenReturn(null);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("200", result.getCode());

        verify(abilityPropertyMapper).insert(any());
        verify(abilityPropertyMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("编辑能力 — 乐观锁冲突应返回 409")
    void testUpdate_OptimisticLockConflict() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setNameCn("新名称");
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        // 模拟更新 0 行（last_update_time 不匹配）
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(0);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("409", result.getCode());
        assertTrue(result.getMessageZh().contains("数据已被修改"));
    }

    @Test
    @DisplayName("编辑能力 — 未传 lastUpdateTime 应跳过乐观锁检查")
    void testUpdate_WithoutLastUpdateTime() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setNameCn("新名称");
        // 不传 lastUpdateTime

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("200", result.getCode());
    }

    @Test
    @DisplayName("编辑能力 — 不传任何业务字段返回 200（无更新）")
    void testUpdate_NoFieldsToUpdate() {
        AdminAbilityUpdateRequest request = new AdminAbilityUpdateRequest();
        request.setLastUpdateTime(lastUpdateTime);

        when(abilityMapper.selectByAbilityType(100)).thenReturn(existingEntity);
        // update 返回值 0 但因无字段要更新，不应报 409
        when(abilityMapper.updateByPrimaryKeySelective(any())).thenReturn(0);

        ApiResponse<Void> result = adminAbilityService.update(100, request);

        assertEquals("200", result.getCode());
    }

    /**
     * 构建已有能力实体
     */
    private AbilityEntity buildExistingEntity() {
        AbilityEntity entity = new AbilityEntity();
        entity.setId(1L);
        entity.setAbilityNameCn("原始中文名");
        entity.setAbilityNameEn("OriginalName");
        entity.setAbilityDescCn("这是原始中文描述，足够长度");
        entity.setAbilityDescEn("This is the original English description");
        entity.setAbilityType(100);
        entity.setOrderNum(1);
        entity.setStatus(1);
        entity.setLoadType(1);
        entity.setHidden(1);
        entity.setRequireRelease(0);
        entity.setEntryUrl("https://example.com");
        entity.setLastUpdateTime(lastUpdateTime);
        return entity;
    }
}
