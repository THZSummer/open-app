package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接器详情响应（V3 应用隔离版本）
 * <p>
 * API #3: GET /service/open/v2/connectors/{connectorId}
 * V3 新增：status、appId 字段
 * V3 变更：所有 BIGINT ID 统一返回 String 类型，时间字段返回 String
 * </p>
 */
@Data
public class ConnectorDetailResponse {

    /** 连接器ID（string，避免 JS 精度丢失） */
    private String connectorId;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 中文描述 */
    private String descriptionCn;

    /** 英文描述 */
    private String descriptionEn;

    /** 连接器类型（TINYINT 数字） */
    private Integer connectorType;

    /** 连接器状态，见 ConnectorStatus 枚举 */
    private Integer status;

    /** 归属应用 ID（string） */
    private String appId;

    /** 创建时间 */
    private String createTime;

    /** 创建者 */
    private String createBy;

    /** 更新人 */
    private String lastUpdateBy;

    /** 最后更新时间 */
    private String lastUpdateTime;
}