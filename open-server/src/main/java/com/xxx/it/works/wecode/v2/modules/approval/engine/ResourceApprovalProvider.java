package com.xxx.it.works.wecode.v2.modules.approval.engine;

import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;

import java.util.Collections;
import java.util.List;

/**
 * 资源审批节点提供者（扩展点）
 *
 * <p>审批引擎 ⓪ 级（资源审批）的通用接口。各场景如需资源审批，
 * 实现此接口并注册为 Spring Bean，无需改动引擎代码。</p>
 *
 * <p>引擎遍历所有实现，取第一个 {@link #supports} 返回 true 的调用 {@link #resolve}。
 * 若无匹配，返回空列表，该级自然跳过。</p>
 */
public interface ResourceApprovalProvider {

    /**
     * 是否支持该业务类型
     */
    boolean supports(String businessType);

    /**
     * 解析该业务的资源审批节点
     *
     * @param businessId 业务对象 ID（由调用方传入，各场景自行解析含义）
     */
    List<ApprovalNodeDto> resolve(Long businessId);

    /**
     * 默认实现：不支持任何类型
     */
    ResourceApprovalProvider NOOP = new ResourceApprovalProvider() {
        @Override
        public boolean supports(String businessType) { return false; }

        @Override
        public List<ApprovalNodeDto> resolve(Long businessId) { return Collections.emptyList(); }
    };
}
