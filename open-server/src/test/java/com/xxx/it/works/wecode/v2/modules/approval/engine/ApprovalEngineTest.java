package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalFlowMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalLogMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ApprovalEngineTest {

    @Mock
    private ApprovalFlowMapper flowMapper;
    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private ApprovalLogMapper logMapper;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.event.mapper.PermissionMapper permissionMapper;
    @Mock
    private IdGeneratorStrategy idGenerator;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private com.xxx.it.works.wecode.v2.modules.api.mapper.ApiMapper apiMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.event.mapper.EventMapper eventMapper;
    @Mock
    private com.xxx.it.works.wecode.v2.modules.callback.mapper.CallbackMapper callbackMapper;

    @InjectMocks
    private ApprovalEngine approvalEngine;

    @Test
    void validateResourceApprovalNodesIfRequired_ThrowsWhenApprovalEnabledWithoutNodes() {
        assertThrows(BusinessException.class,
                () -> approvalEngine.validateResourceApprovalNodesIfRequired(1, null));
        assertThrows(BusinessException.class,
                () -> approvalEngine.validateResourceApprovalNodesIfRequired(1, "[]"));
        assertThrows(BusinessException.class,
                () -> approvalEngine.validateResourceApprovalNodesIfRequired(1, "invalid"));
    }

    @Test
    void validateResourceApprovalNodesIfRequired_AllowsDisabledOrConfiguredApproval() {
        String nodes = "[{\"type\":\"approver\",\"userId\":\"u1\",\"userName\":\"User One\",\"order\":1}]";

        assertDoesNotThrow(() -> approvalEngine.validateResourceApprovalNodesIfRequired(0, null));
        assertDoesNotThrow(() -> approvalEngine.validateResourceApprovalNodesIfRequired(null, null));
        assertDoesNotThrow(() -> approvalEngine.validateResourceApprovalNodesIfRequired(1, nodes));
    }
}
