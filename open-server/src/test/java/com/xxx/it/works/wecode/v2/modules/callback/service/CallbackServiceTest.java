package com.xxx.it.works.wecode.v2.modules.callback.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.callback.dto.CallbackUpdateRequest;
import com.xxx.it.works.wecode.v2.modules.callback.dto.PermissionDefinitionDto;
import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper;
import com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackPropertyMapper;
import com.xxx.it.works.wecode.v2.modules.category.mapper.CategoryMapper;
import com.xxx.it.works.wecode.v2.modules.event.entity.Permission;
import com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private CallbackMapper callbackMapper;

    @Mock
    private CallbackPropertyMapper callbackPropertyMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private IdGeneratorStrategy idGenerator;

    @Mock
    private ApprovalEngine approvalEngine;

    @Mock
    private ApprovalFlowMapper approvalFlowMapper;

    @InjectMocks
    private CallbackService callbackService;

    private Callback callback;
    private Permission permission;

    @BeforeEach
    void setUp() {
        callback = new Callback();
        callback.setId(100L);
        callback.setNameCn("Old Callback");
        callback.setNameEn("Old Callback");
        callback.setCategoryId(2L);
        callback.setStatus(1);
        callback.setCreateTime(new Date());
        callback.setLastUpdateTime(new Date());

        permission = new Permission();
        permission.setId(200L);
        permission.setNameCn("Old Permission");
        permission.setNameEn("Old Permission");
        permission.setScope("callback:test:old");
        permission.setResourceType("callback");
        permission.setResourceId(100L);
        permission.setCategoryId(2L);
        permission.setStatus(1);
    }

    @Test
    @DisplayName("Update callback scope should persist changed scope")
    void testUpdateCallbackScope() {
        CallbackUpdateRequest request = new CallbackUpdateRequest();
        PermissionDefinitionDto permissionRequest = new PermissionDefinitionDto();
        permissionRequest.setScope("callback:test:changed");
        request.setPermission(permissionRequest);

        when(callbackMapper.selectById(100L)).thenReturn(callback);
        when(callbackMapper.update(any(Callback.class))).thenReturn(1);
        when(permissionMapper.selectByResource("callback", 100L)).thenReturn(permission);
        when(permissionMapper.selectByScope("callback:test:changed")).thenReturn(null);
        when(permissionMapper.update(any(Permission.class))).thenReturn(1);

        callbackService.updateCallback(100L, request);

        verify(permissionMapper).update(argThat(updatedPermission ->
                "callback:test:changed".equals(updatedPermission.getScope())));
    }

    @Test
    @DisplayName("Update callback scope should reject duplicate scope")
    void testUpdateCallbackScopeDuplicate() {
        CallbackUpdateRequest request = new CallbackUpdateRequest();
        PermissionDefinitionDto permissionRequest = new PermissionDefinitionDto();
        permissionRequest.setScope("callback:test:duplicate");
        request.setPermission(permissionRequest);

        Permission duplicatePermission = new Permission();
        duplicatePermission.setId(201L);
        duplicatePermission.setScope("callback:test:duplicate");

        when(callbackMapper.selectById(100L)).thenReturn(callback);
        when(callbackMapper.update(any(Callback.class))).thenReturn(1);
        when(permissionMapper.selectByResource("callback", 100L)).thenReturn(permission);
        when(permissionMapper.selectByScope("callback:test:duplicate")).thenReturn(duplicatePermission);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                callbackService.updateCallback(100L, request));

        assertEquals("409", exception.getCode());
        verify(permissionMapper, never()).update(any(Permission.class));
    }
}
