package com.xxx.it.works.wecode.v2.modules.approval.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalListRequest;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalProcessRequest;
import com.xxx.it.works.wecode.v2.modules.approval.vo.ApprovalListVo;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;

/**
 * 审批服务接口
 */
public interface ApprovalService {

    /**
     * 查询待审批应用列表
     *
     * @param request 分页请求参数
     * @return 分页结果
     */
    ApiResponse<PageVO<ApprovalListVo>> getPendingList(ApprovalListRequest request);

    /**
     * 查询已上架应用列表
     *
     * @param request 分页请求参数
     * @return 分页结果
     */
    ApiResponse<PageVO<ApprovalListVo>> getPublishedList(ApprovalListRequest request);

    /**
     * 审批操作（通过/驳回）
     *
     * @param request 审批操作请求
     * @return 操作结果
     */
    ApiResponse<Void> processApproval(ApprovalProcessRequest request);
}
