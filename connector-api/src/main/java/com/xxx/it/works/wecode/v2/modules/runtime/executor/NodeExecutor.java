package com.xxx.it.works.wecode.v2.modules.runtime.executor;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import reactor.core.publisher.Mono;

/**
 * 节点执行器接口
 * <p>
 * 所有节点类型(入口/连接器/数据处理/出口)的统一执行契约
 * 全部返回 Mono<NodeOutput>, 无阻塞 API
 * </p>
 */
public interface NodeExecutor {

    /**
     * 执行节点
     *
     * @param context    执行上下文 (含触发数据/节点输出/凭证)
     * @param nodeConfig 节点配置 (deserialized as Map)
     * @return Mono<NodeOutput> 节点执行输出
     */
    Mono<NodeOutput> execute(ExecutionContext context, Object nodeConfig);

    /**
     * 获取当前节点类型标识
     */
    String getNodeType();
}