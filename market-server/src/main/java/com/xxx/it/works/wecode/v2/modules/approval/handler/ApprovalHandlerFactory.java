package com.xxx.it.works.wecode.v2.modules.approval.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批处理器工厂
 *
 * <p>自动收集所有 ApprovalHandler 实现，按 businessType 注册到 Map 中</p>
 */
@Slf4j
@Component
public class ApprovalHandlerFactory {

    @Autowired
    private List<ApprovalHandler> handlers;

    private final Map<String, ApprovalHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ApprovalHandler handler : handlers) {
            handlerMap.put(handler.getBusinessType(), handler);
            log.info("Registered ApprovalHandler for businessType: {}", handler.getBusinessType());
        }
    }

    /**
     * 根据业务类型获取对应的处理器
     *
     * @param businessType 业务类型
     * @return 处理器实例，不存在时返回 null
     */
    public ApprovalHandler getHandler(String businessType) {
        return handlerMap.get(businessType);
    }
}
