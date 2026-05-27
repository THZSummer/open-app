package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 更新连接器基本信息请求
 * <p>
 * API #4: PUT /service/open/v2/connectors/{connectorId}
 * </p>
 */
@Data
public class ConnectorUpdateRequest {

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

    /** 连接器类型: 1=HTTP */
    private Integer connectorType;
}