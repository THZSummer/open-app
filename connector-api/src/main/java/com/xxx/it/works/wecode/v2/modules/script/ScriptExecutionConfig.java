package com.xxx.it.works.wecode.v2.modules.script;

import lombok.Data;

import java.util.List;

/**
 * 脚本节点执行配置
 * <p>
 * 对应 React Flow 节点 data 字段中的脚本相关配置,
 * 由编排配置解析后传入 ScriptNodeExecutor
 * </p>
 *
 * @author SDDU Build Agent
 */
@Data
public class ScriptExecutionConfig {

    /** 脚本源码 (最大 10000 字符) */
    private String scriptSource;

    /** 超时时间(毫秒), 默认 5000, 最大 30000 */
    private int timeoutMs = 5000;

    /** 上游节点 ID 列表 (用于 ctx 数据组装) */
    private List<String> upstreamNodeIds;

    /** 当前节点 ID */
    private String nodeId;
}
