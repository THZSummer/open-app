package com.xxx.it.works.wecode.v2.modules.approval.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalListRequest;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalProcessRequest;
import com.xxx.it.works.wecode.v2.modules.approval.vo.ApprovalListVo;

import java.util.List;

/**
 * 审批服务接口
 */
public interface ApprovalService {

    ApiResponse<List<ApprovalListVo>> getPendingList(ApprovalListRequest request);

    ApiResponse<List<ApprovalListVo>> getPublishedList(ApprovalListRequest request);

    ApiResponse<Void> processApproval(ApprovalProcessRequest request);
}
