package com.xxx.it.works.wecode.v2.modules.execution.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 执行步骤详情实体
 *
 * <p>对应表 openplatform_v2_cp_execution_step_t</p>
 * <p>connector-api 运行时写入侧使用，与 open-server 查询侧共享表结构</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ExecutionStep implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long id;

    /** 执行记录ID（关联 execution_record_t） */
    private Long executionId;

    /** 节点ID（编排配置中的节点标识） */
    private String nodeId;

    /** 节点类型：1=触发器, 2=连接器, 3=脚本, 4=并行处理, 5=出口 */
    private Integer nodeType;

    /** 节点中文标签 */
    private String nodeLabelCn;

    /** 节点英文标签 */
    private String nodeLabelEn;

    /** 迭代次数（并行分支内顺序） */
    private Integer iteration;

    /** 执行状态：0=成功, 1=失败 */
    private Integer status;

    /** 缓存状态：0=未命中, 1=命中 */
    private Integer cacheStatus;

    /** 缓存键 */
    private String cacheKey;

    /** 缓存剩余 TTL（秒） */
    private Integer cacheTtlRemaining;

    /** 输入数据快照（JSON，脱敏后） */
    private String inputData;

    /** 输出数据快照（JSON，脱敏后） */
    private String outputData;

    /** 错误信息 */
    private String errorMessage;

    /** 错误码 */
    private String errorCode;

    /** 耗时（毫秒） */
    private Integer durationMs;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建者 */
    private String createBy;

    /** 最后更新者 */
    private String lastUpdateBy;
}
