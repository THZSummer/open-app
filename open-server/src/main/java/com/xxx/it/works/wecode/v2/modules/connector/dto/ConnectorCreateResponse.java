package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建连接器响应（V3 扩展）
 * <p>
 * API #1: POST /service/open/v2/connectors
 * V3 新增：nameCn、nameEn、connectorType、status、appId、createTime、note
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
public class ConnectorCreateResponse {

    /** 连接器ID（string） */
    private String connectorId;

    /** 中文名称 */
    private String nameCn;

    /** 英文名称 */
    private String nameEn;

    /** 协议类型 */
    private Integer connectorType;

    /** 连接器状态：固定返回 1（有效不可用） */
    private Integer status;

    /** 归属应用 ID（string） */
    private String appId;

    /** 创建时间 */
    private String createTime;

    /** 提示信息 */
    private String note;

    public ConnectorCreateResponse(String connectorId, String nameCn, String nameEn, Integer connectorType, Integer status, String appId, String createTime, String note) {
        this.connectorId = connectorId;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.connectorType = connectorType;
        this.status = status;
        this.appId = appId;
        this.createTime = createTime;
        this.note = note;
    }
}