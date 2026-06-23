package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 执行步骤详情实体
 *
 * <p>对应表 openplatform_v2_cp_execution_step_t</p>
 * <p>记录连接流单次执行中每个节点的输入/输出/耗时/状态</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionStep implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 关联执行记录ID */
    private Long executionId;

    /** 节点ID（对应 flow_version_snapshot.nodes[].id） */
    private String nodeId;

    /** 节点类型：1=trigger, 2=connector, 3=script, 4=parallel, 5=exit */
    private Integer nodeType;

    /** 节点中文名称 (执行时快照) */
    private String nodeLabelCn;

    /** 节点英文名称 (执行时快照) */
    private String nodeLabelEn;

    /** 循环轮次（0=首次或非循环，>0=第N轮循环） */
    private Integer iteration;

    /** 步骤状态：0=success, 1=failed */
    private Integer status;

    /** 节点级缓存状态：0=未命中（正常执行）, 1=节点级命中 */
    private Integer cacheStatus;

    /** 命中的节点级缓存键（调试用） */
    private String cacheKey;

    /** 命中时缓存剩余 TTL（秒） */
    private Integer cacheTtlRemaining;

    /** 步骤输入数据JSON */
    private String inputData;

    /** 步骤输出数据JSON */
    private String outputData;

    /** 步骤错误信息 */
    private String errorMessage;

    /** 错误码（4xx=下游客户端, 5xx=下游服务端, 6xxxx=引擎内部） */
    private String errorCode;

    /** 步骤耗时(毫秒) */
    private Integer durationMs;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人（系统自动生成） */
    private String createBy;

    /** 最后更新人（系统自动生成） */
    private String lastUpdateBy;
}
