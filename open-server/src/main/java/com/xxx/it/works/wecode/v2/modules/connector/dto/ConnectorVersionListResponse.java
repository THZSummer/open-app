package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器版本列表响应项（V3 新增）
 * <p>
 * API #9: GET /service/open/v2/connectors/{connectorId}/versions
 * </p>
 */
@Data
public class ConnectorVersionListResponse {

    /** 版本 ID（string） */
    private String versionId;

    /** 版本号 */
    private Integer versionNumber;

    /** 版本状态，见 ConnectorVersionStatus 枚举 */
    private Integer status;

    /** 发布时间（已发布/已失效时有值） */
    private String publishedTime;

    /** 发布人 */
    private String publishedBy;

    /** 创建时间 */
    private String createTime;

    /** 创建人（草稿时有值） */
    private String createBy;
}
