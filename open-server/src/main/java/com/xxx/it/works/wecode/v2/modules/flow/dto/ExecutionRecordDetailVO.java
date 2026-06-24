package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 运行记录详情 VO（含步骤日志列表）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionRecordDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 执行记录ID */
    private String id;

    /** 连接流ID */
    private String flowId;

    /** 连接流版本ID */
    private String flowVersionId;

    /** 连接流版本号 */
    private Integer flowVersionNumber;

    /** 连接流名称 */
    private String flowNameCn;

    /** 连接流英文名称 */
    private String flowNameEn;

    /** 触发方式：1=HTTP触发, 2=调试触发 */
    private Integer triggerType;

    /** 触发方式描述 */
    private String triggerTypeDesc;

    /** 触发账号 */
    private String triggerAccount;

    /** 执行状态：0=成功, 1=失败 */
    private Integer status;

    /** 执行状态描述 */
    private String statusDesc;

    /** 总耗时（毫秒） */
    private Integer durationMs;

    /** 限流状态 */
    private Integer rateLimitStatus;

    /** 缓存状态 */
    private Integer cacheStatus;

    /** 缓存键 */
    private String cacheKey;

    /** 缓存剩余TTL */
    private Integer cacheTtlRemaining;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 触发时间 */
    private Date triggerTime;

    /** 创建时间 */
    private Date createTime;

    /** 步骤日志列表 */
    private List<ExecutionStepVO> steps;

    /**
     * 步骤日志 VO
     */
    @Data
    public static class ExecutionStepVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 步骤ID */
        private String id;

        /** 节点ID */
        private String nodeId;

        /** 节点类型 */
        private Integer nodeType;

        /** 节点类型描述 */
        private String nodeTypeDesc;

        /** 节点中文标签 */
        private String nodeLabelCn;

        /** 节点英文标签 */
        private String nodeLabelEn;

        /** 迭代次数 */
        private Integer iteration;

        /** 执行状态：0=成功, 1=失败 */
        private Integer status;

        /** 执行状态描述 */
        private String statusDesc;

        /** 缓存状态 */
        private Integer cacheStatus;

        /** 输入数据（JSON字符串） */
        private String inputData;

        /** 输出数据（JSON字符串） */
        private String outputData;

        /** 错误信息 */
        private String errorMessage;

        /** 错误码 */
        private String errorCode;

        /** 耗时（毫秒） */
        private Integer durationMs;

        /** 创建时间 */
        private Date createTime;
    }
}
