package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批业务处理器（扩展点）
 *
 * <p>每个业务类型实现一个独立的 Handler，封装该场景的全部特定逻辑。
 * 新增业务场景只需新增一个 Handler 并注册为 Spring Bean，无需改动引擎代码，
 * 避免多人并发修改同一个文件导致的 merge 冲突。</p>
 *
 * <p>引擎在 compose 时按 businessType 匹配 Handler，依次调用：</p>
 * <ol>
 *   <li>{@link #resolveResourceNodes(Long)} — ⓪ 资源审批节点</li>
 *   <li>{@link #onApproved} / {@link #onRejected} / {@link #onCancelled} — 审批结果回调</li>
 * </ol>
 */
public interface ApprovalBusinessHandler {

    /**
     * 本 Handler 支持的业务类型
     */
    String supportedBusinessType();

    /**
     * ⓪ 解析资源审批节点（默认不实现，返回空）
     */
    default List<ApprovalNodeDto> resolveResourceNodes(Long businessId) {
        return Collections.emptyList();
    }

    /**
     * 审批全部通过后的回调
     */
    void onApproved(ApprovalRecord record);

    /**
     * 任一节点驳回后的回调
     */
    void onRejected(ApprovalRecord record);

    /**
     * 申请入撤回后的回调
     */
    void onCancelled(ApprovalRecord record);

    /** 获取审批业务数据（用于审批列表/详情展示），默认返回空 Map */
    default Map<String, Object> getBusinessData(Long businessId) {
        return new HashMap<>();
    }
}
