package com.xxx.it.works.wecode.v2.modules.flow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 连接流缓存清理异步监听器
 * <p>
 * 监听 {@link FlowCacheEvictEvent}, 事件发布后异步执行 Redis 缓存清理:
 * - 事件驱动解耦: 发布方 (FlowDeployService/FlowService) 不关心清理细节
 * - 异步执行: 使用 cacheEvictExecutor 线程池, 不阻塞事件发布线程
 * - 不绑定事务: 缓存清理是删除操作, 即使事务回滚也仅导致缓存缺失 → 下次读 DB 重建,
 *   无需等待事务提交 (TTL 兜底最终一致)
 * - 异常兜底: 清理失败仅 log.warn, 不影响主业务
 * </p>
 */
@Slf4j
@Component
public class FlowCacheEvictListener {

    @Autowired
    private FlowCacheEvictor flowCacheEvictor;

    @Async("cacheEvictExecutor")
    @EventListener
    public void onEvict(FlowCacheEvictEvent event) {
        try {
            if (event.getFlowId() == null) return;
            log.debug("Flow cache evict event received: flowId={}, scopes={}", event.getFlowId(), event.getScopes());
            if (event.includes(FlowCacheEvictEvent.SCOPE_FLOW_CONFIG)) {
                flowCacheEvictor.evictFlowConfig(event.getFlowId());
            }
            if (event.includes(FlowCacheEvictEvent.SCOPE_FLOW_ENTITY)) {
                flowCacheEvictor.evictFlowEntity(event.getFlowId());
            }
            if (event.includes(FlowCacheEvictEvent.SCOPE_EXECUTION_RESULTS)) {
                flowCacheEvictor.evictExecutionResults(event.getFlowId());
            }
        } catch (Exception e) {
            log.warn("Async cache evict failed for flowId={}: {}", event.getFlowId(), e.getMessage());
        }
    }
}
