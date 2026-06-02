package com.xxx.api.approval.handler;

import com.xxx.api.approval.entity.ApprovalRecord;

/**
 * 审批回调策略接口
 *
 * <p>不同 businessType 的审批通过后业务副作用由各自的 Handler 实现。
 * Spring 自动注册所有实现到 {@link ApprovalCallbackHandlerFactory}。</p>
 *
 * <p>扩展方式：新增业务类型时，只需实现此接口并添加 @Component 注解，
 * 工厂自动注册，Service 无需修改。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface ApprovalCallbackHandler {

    /**
     * 返回该 Handler 支持的 businessType
     *
     * <p>对应 ApprovalEngine.BusinessType 常量，如 "api_permission_apply"</p>
     *
     * @return businessType 标识
     */
    String getBusinessType();

    /**
     * 审批最终通过后的业务副作用
     *
     * <p>最后一个审批节点通过时调用（record.status 已更新为 APPROVED）。
     * 负责更新订阅状态、触发通知等业务逻辑。</p>
     *
     * @param record 审批记录
     */
    void onApproved(ApprovalRecord record);

    /**
     * 审批驳回后的业务副作用
     *
     * <p>任意节点驳回时调用（record.status 已更新为 REJECTED）。
     * 负责更新订阅状态等业务逻辑。</p>
     *
     * @param record 审批记录
     */
    void onRejected(ApprovalRecord record);
}
