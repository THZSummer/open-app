package com.xxx.it.works.wecode.v2.modules.flow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 连接流缓存清理异步监听器
 * <p>
 * 监听 {@link FlowCacheEvictEvent}, 在事务提交后 ({@code AFTER_COMMIT}) 异步执行 Redis 缓存清理:
 * - 保证语义: 仅 DB 事务成功提交后才清理 (避免 "DB 回滚但缓存已清" 不一致)
 * - 异步执行: 使用 cacheEvictExecutor 线程池, 不阻塞事务提交线程
 * - 异常兜底: 清理失败仅 log.warn, 不影响主业务; TTL 过期兜底最终一致
 * </p>
 */
@Slf4j
@Component
public class FlowCacheEvictListener {

    @Autowired
    private FlowCacheEvictor flowCacheEvictor;

    @Async("cacheEvictExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvict(FlowCacheEvictEvent event) {
        try {
            if (event.getFlowId() == null) return;
            if (event.includes(FlowCacheEvictEvent.SCOPE_FLOW_CONFIG)) {
                flowCacheEvictor.evictFlowConfig(event.getFlowId());
            }
            if (event.includes(FlowCacheEvictEvent.SCOPE_FLOW_ENTITY)) {
                flowCacheEvictor.evictFlowEntity(event.getFlowId());
            }
            if (event.includes(FlowCacheEvictEvent.SCOPE_EXECUTION_RESULTS)) {
                flowCacheEvictor.evictExecutionResults(event.getFlowId());
            }
        } catch (Exception ex) {
            log.error("Async cache evict failed for flowId={}: {}", event.getFlowId(), ex.getMessage(), ex);
        }
    }
}
