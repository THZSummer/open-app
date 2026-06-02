package com.xxx.api.approval.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批回调策略工厂
 *
 * <p>自动收集所有 {@link ApprovalCallbackHandler} 实现，
 * 根据 businessType 路由到对应 Handler。</p>
 *
 * <p>参考 open-server 的 EntitySnapshotLoaderFactory 模式：
 * List 注入 + @PostConstruct 构建 Map + O(1) 查找</p>
 *
 * <p>扩展方式：新增 Handler 实现后，工厂自动注册，无需修改此类。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalCallbackHandlerFactory {

    private final List<ApprovalCallbackHandler> handlers;
    private final Map<String, ApprovalCallbackHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ApprovalCallbackHandler handler : handlers) {
            handlerMap.put(handler.getBusinessType(), handler);
            log.debug("[CALLBACK] Registered handler: {} -> {}",
                    handler.getBusinessType(), handler.getClass().getSimpleName());
        }
        log.info("[CALLBACK] Initialized {} approval callback handlers", handlerMap.size());
    }

    /**
     * 根据 businessType 获取对应的策略处理器
     *
     * @param businessType 业务类型（如 api_permission_apply）
     * @return 对应的 Handler，未注册返回 null
     */
    public ApprovalCallbackHandler getHandler(String businessType) {
        return handlerMap.get(businessType);
    }
}
