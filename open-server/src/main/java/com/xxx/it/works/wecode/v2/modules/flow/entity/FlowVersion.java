package com.xxx.it.works.wecode.v2.modules.flow.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接流版本/配置实体
 * <p>
 * 对应表 openplatform_v2_cp_flow_version_t
 * MVP 单版本模型: 每 flow 仅一条记录, 编辑即生效
 * 编排配置 JSON: {trigger, nodes[], edges[]} 完整 DAG
 * </p>
 */
@Data
public class FlowVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 关联连接流ID (逻辑外键 → flow_t.id) */
    private Long flowId;

    /** 编排配置JSON: {trigger:{...}, nodes:[...], edges:[...]} */
    private String orchestrationConfig;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;
}