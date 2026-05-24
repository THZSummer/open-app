package com.xxx.it.works.wecode.v2.modules.runtime.executor;

import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;

/**
 * ExecutionContext 提供者
 * <p>
 * 定义执行上下文接口, 供 NodeExecutor 实现类获取上下文数据
 * </p>
 */
public interface ExecutionContextProvider {

    /**
     * 获取当前执行上下文
     */
    ExecutionContext getContext();
}