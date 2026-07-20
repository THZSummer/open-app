package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.ability.service.impl.AdminAbilityServiceImpl;
import com.xxx.it.works.wecode.v2.modules.file.service.CommonFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminAbilityService 删除接口单元测试
 *
 * <p>覆盖正常删除、不存在的 abilityType 返回 404 等场景。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@DisplayName("AdminAbilityService — 删除能力业务逻辑")
@ExtendWith(MockitoExtension.class)
class AdminAbilityDeleteServiceTest {

    private AdminAbilityServiceImpl adminAbilityService;

    @Mock
    private AbilityMapper abilityMapper;

    @Mock
    private AbilityPropertyMapper abilityPropertyMapper;

    @Mock
    private CommonFileService commonFileService;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @BeforeEach
    void setUp() {
        adminAbilityService = new AdminAbilityServiceImpl(
                abilityMapper, abilityPropertyMapper, commonFileService, idGenerator);
    }

    @Test
    @DisplayName("删除能力 — 成功应删除主表和属性表")
    void testDelete_Success() {
        // 准备
        Integer abilityType = 100;
        AbilityEntity existingEntity = new AbilityEntity();
        existingEntity.setId(1L);
        existingEntity.setAbilityType(abilityType);
        existingEntity.setAbilityNameCn("待删除能力");

        when(abilityMapper.selectByAbilityType(abilityType)).thenReturn(existingEntity);
        when(abilityPropertyMapper.deleteByParentId(1L)).thenReturn(2);
        when(abilityMapper.deleteByAbilityType(abilityType)).thenReturn(1);

        // 执行
        ApiResponse<Void> result = adminAbilityService.delete(abilityType);

        // 断言
        assertEquals("200", result.getCode());

        verify(abilityMapper).selectByAbilityType(abilityType);
        verify(abilityPropertyMapper).deleteByParentId(1L);
        verify(abilityMapper).deleteByAbilityType(abilityType);
    }

    @Test
    @DisplayName("删除能力 — abilityType 不存在应返回 404")
    void testDelete_NotFound() {
        // 准备
        Integer abilityType = 999;
        when(abilityMapper.selectByAbilityType(abilityType)).thenReturn(null);

        // 执行
        ApiResponse<Void> result = adminAbilityService.delete(abilityType);

        // 断言
        assertEquals("404", result.getCode());
        assertTrue(result.getMessageZh().contains("不存在"));

        // 不应执行任何删除操作
        verify(abilityPropertyMapper, never()).deleteByParentId(anyLong());
        verify(abilityMapper, never()).deleteByAbilityType(anyInt());
    }

    @Test
    @DisplayName("删除能力 — 无属性记录的删除也应成功")
    void testDelete_NoProperties() {
        // 准备
        Integer abilityType = 101;
        AbilityEntity existingEntity = new AbilityEntity();
        existingEntity.setId(2L);
        existingEntity.setAbilityType(abilityType);
        existingEntity.setAbilityNameCn("无属性能力");

        when(abilityMapper.selectByAbilityType(abilityType)).thenReturn(existingEntity);
        when(abilityPropertyMapper.deleteByParentId(2L)).thenReturn(0);
        when(abilityMapper.deleteByAbilityType(abilityType)).thenReturn(1);

        // 执行
        ApiResponse<Void> result = adminAbilityService.delete(abilityType);

        // 断言
        assertEquals("200", result.getCode());
        verify(abilityPropertyMapper).deleteByParentId(2L);
        verify(abilityMapper).deleteByAbilityType(abilityType);
    }
}
