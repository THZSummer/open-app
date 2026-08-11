package com.xxx.it.works.wecode.v2.modules.flow.service;

import lombok.Getter;

/**
 * 连接流缓存清理事件
 * <p>
 * 生命周期操作 (部署/停止/失效/删除) 提交事务后发布,
 * 由 {@link FlowCacheEvictListener} 异步监听执行 Redis 缓存清理。
 * 事件包含 flowId 与所需清理的缓存范围。
 * </p>
 */
@Getter
public class FlowCacheEvictEvent {

    /** 清理范围: 执行结果缓存 (cp:cache:flow:*) */
    public static final String SCOPE_EXECUTION_RESULTS = "executionResults";
    /** 清理范围: flow 实体缓存 (cp:entity:flow:) */
    public static final String SCOPE_FLOW_ENTITY = "flowEntity";
    /** 清理范围: flow 配置缓存 (cp:flow:config:) */
    public static final String SCOPE_FLOW_CONFIG = "flowConfig";

    private final Long flowId;

    /** 逗号分隔的清理范围 (SCOPE_* 常量) */
    private final String scopes;

    public FlowCacheEvictEvent(Long flowId, String... scopes) {
        this.flowId = flowId;
        this.scopes = String.join(",", scopes);
    }

    public boolean includes(String scope) {
        return scopes.contains(scope);
    }
}
