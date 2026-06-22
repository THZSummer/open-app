package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器版本详情响应（V3 新增）
 * <p>
 * API #10: GET /service/open/v2/connectors/{connectorId}/versions/{versionId}
 * V3 变更：替换 V1 GET /config 接口
 * </p>
 */
@Data
public class ConnectorVersionDetailResponse {

    /** 版本 ID（string） */
    private String versionId;

    /** 所属连接器 ID（string） */
    private String connectorId;

    /** 版本号 */
    private Integer versionNumber;

    /** 版本状态，见 ConnectorVersionStatus 枚举 */
    private Integer status;

    /** 连接配置快照（JSON 字符串，前端自行解析） */
    private String connectionConfig;

    /** 发布时间 */
    private String publishedTime;

    /** 发布人 */
    private String publishedBy;

    /** 创建时间 */
    private String createTime;

    /** 创建人 */
    private String createBy;
}
