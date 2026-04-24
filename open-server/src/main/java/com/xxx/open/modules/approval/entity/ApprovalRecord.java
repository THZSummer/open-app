package com.xxx.open.modules.approval.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批记录实体
 * 
 * <p>对应表 openplatform_v2_approval_record_t</p>
 * <p>记录具体的审批单据，关联业务对象</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private Long id;

    // ✅ v2.8.0 变更：移除 flowId 字段
    // 原因：
    // 1. combinedNodes 已包含完整审批节点信息（userId、userName、level等）
    // 2. 审批记录数据独立，不受审批流程模板修改影响
    // 3. 查询效率更高，无需 JOIN approval_flow_t 表
    // 4. 审计追溯更准确，保留审批时的完整配置

    /**
     * 组合后的完整审批节点配置（JSON 格式字符串）
     * 
     * 格式示例（按审批顺序从具体到一般）：
     * [
     *   {"type":"approver","userId":"payment_leader","userName":"支付团队负责人","order":1,"level":"resource"},
     *   {"type":"approver","userId":"finance_admin","userName":"财务管理员","order":2,"level":"resource"},
     *   {"type":"approver","userId":"perm_admin","userName":"权限管理员","order":3,"level":"scene"},
     *   {"type":"approver","userId":"admin001","userName":"系统管理员","order":4,"level":"global"}
     * ]
     * 
     * 三级审批顺序：
     * 1. 资源审批（level='resource') - 资源提供方审核
     * 2. 场景审批（level='scene') - 业务场景审核
     * 3. 全局审批（level='global') - 平台运营审核
     */
    private String combinedNodes;

    /**
     * 业务类型：
     * - api_register = API注册审批
     * - event_register = 事件注册审批
     * - callback_register = 回调注册审批
     * - api_permission_apply = API权限申请审批
     * - event_permission_apply = 事件权限申请审批
     * - callback_permission_apply = 回调权限申请审批
     */
    private String businessType;

    /**
     * 业务对象ID
     */
    private Long businessId;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人名称
     */
    private String applicantName;

    /**
     * 审批状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销
     */
    private Integer status;

    /**
     * 当前审批节点索引（从 0 开始）
     */
    private Integer currentNode;

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

    /**
     * 完成时间
     */
    private Date completedAt;
}
