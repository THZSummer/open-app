package com.xxx.it.works.wecode.v2.modules.runtime.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 节点执行上下文数据
 * <p>
 * v5.5: 使用 {@code input}/{@code output} 双分区替代旧 {@code outputData} 统一字段,
 * 配合 JSON Schema 5.5 的 inputContract/outputContract 分离定义.
 * errorInfo 以结构化 Map {@code {code, messageZh, messageEn}} 替代旧 String errorMessage.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeContext {

    /** 节点ID (对应编排配置中的节点ID) */
    private String nodeId;

    /** 节点类型: entry/connector/data_processor/exit */
    private String nodeType;

    /** 输入数据分区 (来自上游输出或触发数据) */
    private Map<String, Object> input;

    /** 输出数据分区 (本节点执行结果) */
    private Map<String, Object> output;

    /** 执行状态: success/failed/timeout */
    private String status;

    /** 结构化错误信息 (status=failed 时): {code, messageZh, messageEn, ...} */
    private Map<String, Object> errorInfo;

    /** 耗时(毫秒) */
    private long durationMs;

    /** 是否成功 */
    public boolean isSuccess() {
        return "success".equals(status);
    }
}