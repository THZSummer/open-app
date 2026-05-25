package com.xxx.it.works.wecode.v2.modules.connector.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新连接配置请求
 * <p>
 * API #7: PUT /api/v1/connectors/{connectorId}/config
 * 编辑即生效, connection_config 全文替换
 * </p>
 */
@Data
public class ConnectorConfigUpdateRequest {

    /** 连接配置JSON (完整替换) */
    @NotBlank(message = "连接配置不能为空")
    private String connectionConfig;
}