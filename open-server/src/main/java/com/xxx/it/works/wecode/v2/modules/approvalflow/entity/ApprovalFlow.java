package com.xxx.it.works.wecode.v2.modules.approvalflow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批流程模板实体
 *
 * <p>对应表 openplatform_v2_approval_flow_t</p>
 * <p>审批流程模板定义，支持动态节点配置</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程ID
     */
    private Long id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 流程编码，全局唯一：
     * - global=全局审批流程
     * - api_register=API注册审批流程
     * - event_register=事件注册审批流程
     * - callback_register=回调注册审批流程
     * - api_permission_apply=API权限申请审批流程
     * - event_permission_apply=事件权限申请审批流程
     * - callback_permission_apply=回调权限申请审批流程
     *
     * v2.8.0变更：移除 isDefault 字段，改用 code='global' 标识全局审批
     */
    private String code;

    /**
     * 中文描述
     */
    private String descriptionCn;

    /**
     * 英文描述
     */
    private String descriptionEn;

    // ✅ v2.8.0 变更：移除 isDefault 字段
    // 原因：消除冗余，用 code='global' 标识全局审批，更语义化且统一规范
    // 查询方式：SELECT * FROM approval_flow_t WHERE code='global'

    /**
     * 归属应用ID（V3 新增）
     *
     * <p>NULL=平台级/全局模板，非NULL=应用级定制模板。
     * 查询时按app_id就近匹配：应用级优先 → 平台级回退 → 全局级兜底。</p>
     * <p>存储的是 App.id（内部主键），非 App.appId（业务标识）。</p>
     */
    private Long appId;

    /**
     * 外部业务应用ID（V3 新增，transient，来自 JOIN openplatform_app_t）
     *
     * <p>仅在列表/详情查询 SQL 通过 LEFT JOIN 填充，insert/update 不持久化。</p>
     */
    private String externalAppId;

    /**
     * 审批节点配置（JSON 格式）
     *
     * 格式示例：
     * [
     *   {"type": "approver", "userId": "user001", "userName": "张三", "order": 1},
     *   {"type": "approver", "userId": "user002", "userName": "李四", "order": 2}
     * ]
     */
    private String nodes;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

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
