package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运行记录列表 VO
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionRecordVO implements Serializable {

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

    /** 缓存状态：0=未命中, 1=全流命中, 2=部分命中 */
    private Integer cacheStatus;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 触发时间 */
    private Date triggerTime;

    /** 步骤数量 */
    private Integer stepCount;
}
