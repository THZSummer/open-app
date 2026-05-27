package com.xxx.api.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批操作日志实体
 *
 * <p>对应表 openplatform_v2_approval_log_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（TODO: ID 生成策略人工实现）
     */
    private Long id;

    /**
     * 审批记录 ID
     */
    private Long recordId;

    /**
     * 节点索引
     */
    private Integer nodeIndex;

    /**
     * 审批级别：global=全局, scene=场景, resource=资源
     */
    private String level;

    /**
     * 操作人 ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作类型：0=同意, 1=拒绝, 2=撤销, 3=转交
     */
    private Integer action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;
}
