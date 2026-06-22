package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器列表响应项（V3 应用隔离版本）
 * <p>
 * API #2: GET /service/open/v2/connectors
 * V3 新增：status、latestPublishedVersionNumber、draftVersionNumber、appId
 * V3 变更：所有 BIGINT ID 统一返回 String 类型
 * </p>
 */
@Data
public class ConnectorListResponse {

    /** 连接器ID（string，避免 JS 精度丢失） */
    private String connectorId;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 连接器类型（TINYINT 数字） */
    private Integer connectorType;

    /** 连接器状态，见 ConnectorStatus 枚举 */
    private Integer status;

    /** 最新已发布版本号，无已发布版本时为 null */
    private Integer latestPublishedVersionNumber;

    /** 当前草稿版本号，无草稿时为 null */
    private Integer draftVersionNumber;

    /** 归属应用 ID（string） */
    private String appId;

    /** 创建时间 */
    private String createTime;
}