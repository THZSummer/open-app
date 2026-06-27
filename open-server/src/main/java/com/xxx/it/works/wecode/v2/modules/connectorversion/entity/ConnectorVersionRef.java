package com.xxx.it.works.wecode.v2.modules.connectorversion.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接器版本引用中间表实体
 *
 * <p>对应表 openplatform_v2_cp_connector_version_ref_t</p>
 * <p>编排中 connector 节点引用特定 ConnectorVersion，显式管理引用关系</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ConnectorVersionRef implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 连接流ID（冗余，避免 JOIN flow_version_t） */
    private Long flowId;

    /** 连接流版本ID */
    private Long flowVersionId;

    /** 流程编排中的连接器节点ID（React Flow node.id） */
    private String nodeId;

    /** 连接器ID（冗余，避免 JOIN connector_version_t） */
    private Long connectorId;

    /** 连接器版本ID */
    private Long connectorVersionId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date lastUpdateTime;

    /** 创建人账号 */
    private String createBy;

    /** 更新人账号 */
    private String lastUpdateBy;
}
