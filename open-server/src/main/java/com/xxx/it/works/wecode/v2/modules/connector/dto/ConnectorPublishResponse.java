package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接器版本发布响应（V3 新增）
 * <p>
 * API #12: PUT /service/open/v2/connectors/{connectorId}/versions/{versionId}/publish
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
public class ConnectorPublishResponse {

    /** 版本 ID（string） */
    private String versionId;

    /** 版本号 */
    private Integer versionNumber;

    /** 变更后状态：2（已发布） */
    private Integer status;

    /** 连接器状态变更后 */
    private Integer connectorStatus;

    /** 发布时间 */
    private String publishedTime;

    public ConnectorPublishResponse(String versionId, Integer versionNumber, Integer status, Integer connectorStatus, String publishedTime) {
        this.versionId = versionId;
        this.versionNumber = versionNumber;
        this.status = status;
        this.connectorStatus = connectorStatus;
        this.publishedTime = publishedTime;
    }
}
