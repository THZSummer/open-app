package com.xxx.it.works.wecode.v2.modules.connector.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接器版本/配置实体
 * <p>
 * 对应表 openplatform_v2_cp_connector_version_t
 * MVP 单版本模型: 每 connector 仅一条记录, 编辑即生效
 * 仅声明认证类型 Schema (含 sensitive:true 标记), 不存储凭证值
 * </p>
 */
@Data
public class ConnectorVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 关联连接器ID (逻辑外键 → connector_t.id) */
    private Long connectorId;

    /** 连接配置JSON: {protocol,protocolConfig,authTypeSchema,inputSchema,outputSchema,timeoutMs,rateLimit} */
    private String connectionConfig;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;
}