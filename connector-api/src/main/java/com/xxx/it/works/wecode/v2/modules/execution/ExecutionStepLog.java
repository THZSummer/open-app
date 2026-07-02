package com.xxx.it.works.wecode.v2.modules.execution;

import java.util.Map;

/**
 * 步骤日志数据对象（connector-api 运行时写入侧 DTO）
 *
 * <p>由调用方（如 {@code FlowInvokeService.persistStepLogs}）从 {@code ExecutionContext}
 * 各节点上下文组装后, 通过 {@link ExecutionStepService#logStepsBatch} 批量写入。</p>
 *
 * <p>与持久层实体 {@code ExecutionStepEntity} 的区别: 本对象是运行时内存 DTO,
 * 不含 ORM 注解, 由 Service 层负责脱敏 + 序列化为 JSON 后映射到实体。</p>
 *
 * @author SDDU Build Agent
 * @version 2.1.0
 */
public class ExecutionStepLog {

    /** 步骤ID（雪花ID） */
    public Long stepId;

    /** 节点ID（编排配置中的节点标识） */
    public String nodeId;

    /** 节点类型：1=触发器, 2=连接器, 3=脚本, 4=并行处理, 5=出口 */
    public Integer nodeType;

    /** 节点名称（中文标签） */
    public String nodeName;

    /** 执行状态：0=成功, 1=失败 */
    public Integer status;

    /** 输入数据快照（JSON Map） */
    public Map<String, Object> input;

    /** 输出数据快照（JSON Map） */
    public Map<String, Object> output;

    /** 错误信息 */
    public String error;

    /** 耗时（毫秒） */
    public Integer durationMs;
}
