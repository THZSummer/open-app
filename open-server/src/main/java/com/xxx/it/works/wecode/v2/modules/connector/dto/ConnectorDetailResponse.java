package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器详情响应
 * <p>
 * API #3: GET /service/open/v2/connectors/{connectorId}
 * </p>
 */
@Data
public class ConnectorDetailResponse {

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

    /** 创建时间 (ISO 8601) */
    private String createTime;

    /** 最后更新时间 (ISO 8601) */
    private String lastUpdateTime;
}