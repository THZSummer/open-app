package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

import java.util.Date;

/**
 * 连接器列表响应项
 * <p>
 * API #2: GET /service/open/v2/connectors
 * </p>
 */
@Data
public class ConnectorListResponse {

    /** 连接器ID (string格式返回) */
    private String id;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 图标文件ID */
    private String iconFileId;

    /** 连接器类型 (TINYINT数字) */
    private Integer connectorType;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;
}