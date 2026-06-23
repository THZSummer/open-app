package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 执行记录实体
 *
 * <p>对应表 openplatform_v2_cp_execution_record_t</p>
 * <p>记录每次连接流执行的元数据（触发时间、状态、耗时、触发方式）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 归属应用ID（与连接流 app_id 一致，冗余避免 JOIN） */
    private Long appId;

    /** 关联连接流ID */
    private Long flowId;

    /** 关联连接流版本ID */
    private Long flowVersionId;

    /** 关联连接流版本号（冗余，避免列表 JOIN） */
    private Integer flowVersionNumber;

    /** 执行时版本完整快照JSON（orchestrationConfig + flowConfig） */
    private String flowVersionSnapshot;

    /** 连接流中文名称（触发时快照） */
    private String flowNameCn;

    /** 连接流英文名称（触发时快照） */
    private String flowNameEn;

    /** 触发方式：1=http（HTTP触发）, 2=debug（调试触发） */
    private Integer triggerType;

    /** 触发账号（HTTP=调用方凭证标识） */
    private String triggerAccount;

    /** 执行状态：0=success, 1=failed */
    private Integer status;

    /** 限流状态：0=未触发限流, 1=触发限流（429） */
    private Integer rateLimitStatus;

    /** 缓存状态：0=未命中（正常执行）, 1=全流命中, 2=部分命中 */
    private Integer cacheStatus;

    /** 命中的缓存键（全流命中时有值，调试用） */
    private String cacheKey;

    /** 命中时缓存剩余 TTL（秒） */
    private Integer cacheTtlRemaining;

    /** 错误码（4xx=下游客户端, 5xx=下游服务端, 6xxxx=引擎内部） */
    private String errorCode;

    /** 错误信息（整体摘要，节点级详情在 execution_step_t） */
    private String errorMessage;

    /** 总执行耗时(毫秒) */
    private Integer durationMs;

    /** 触发时间 */
    private Date triggerTime;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人（系统自动生成） */
    private String createBy;

    /** 最后更新人（系统自动生成） */
    private String lastUpdateBy;
}
