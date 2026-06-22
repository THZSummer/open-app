package com.xxx.it.works.wecode.v2.modules.execution.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 执行记录实体
 *
 * <p>对应表 openplatform_v2_cp_execution_record_t</p>
 * <p>connector-api 运行时写入侧使用，与 open-server 查询侧共享表结构</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long id;

    /** 应用ID */
    private Long appId;

    /** 连接流ID */
    private Long flowId;

    /** 连接流版本ID */
    private Long flowVersionId;

    /** 连接流版本号 */
    private Integer flowVersionNumber;

    /** 连接流版本快照（MEDIUMTEXT JSON） */
    private String flowVersionSnapshot;

    /** 连接流中文名称（冗余） */
    private String flowNameCn;

    /** 连接流英文名称（冗余） */
    private String flowNameEn;

    /** 触发方式：1=HTTP触发, 2=调试触发 */
    private Integer triggerType;

    /** 触发账号 */
    private String triggerAccount;

    /** 执行状态：0=成功, 1=失败 */
    private Integer status;

    /** 限流状态：0=未限流, 1=被限流 */
    private Integer rateLimitStatus;

    /** 缓存状态：0=未命中, 1=全流命中, 2=部分命中 */
    private Integer cacheStatus;

    /** 缓存键 */
    private String cacheKey;

    /** 缓存剩余 TTL（秒） */
    private Integer cacheTtlRemaining;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 总耗时（毫秒） */
    private Integer durationMs;

    /** 触发时间 */
    private Date triggerTime;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建者 */
    private String createBy;

    /** 最后更新者 */
    private String lastUpdateBy;
}
