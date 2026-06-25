package com.xxx.it.works.wecode.v2.modules.approval.handler;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;

/**
 * 审批处理器策略接口
 *
 * <p>不同业务类型实现各自的审批通过后/驳回后处理逻辑</p>
 */
public interface ApprovalHandler {

    /**
     * 获取业务类型标识
     *
     * @return 业务类型字符串
     */
    String getBusinessType();

    /**
     * 审批通过回调
     *
     * @param record 审批记录
     */
    void onApproved(ApprovalRecord record);

    /**
     * 审批驳回回调
     *
     * @param record 审批记录
     */
    void onRejected(ApprovalRecord record);
}
