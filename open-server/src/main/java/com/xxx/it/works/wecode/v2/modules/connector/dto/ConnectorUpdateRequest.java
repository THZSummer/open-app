package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 更新连接器基本信息请求（V3 应用隔离版本）
 * <p>
 * API #4: PUT /service/open/v2/connectors/{connectorId}
 * V3 变更：移除 iconFileId 和 connectorType，仅允许更新名称和描述
 * </p>
 */
@Data
public class ConnectorUpdateRequest {

    /** 中文名称，最长 64 字符 */
    private String nameCn;

    /** 英文名称，最长 128 字符 */
    private String nameEn;

    /** 中文描述，最长 512 字符 */
    private String descriptionCn;

    /** 英文描述，最长 512 字符 */
    private String descriptionEn;
}