package com.xxx.it.works.wecode.v2.modules.connector.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新连接配置请求
 * <p>
 * API #7: PUT /service/open/v2/connectors/{connectorId}/config
 * 编辑即生效, connection_config 全文替换
 * </p>
 */
@Data
public class ConnectorConfigUpdateRequest {

    /** 连接配置 JSON 对象 (完整替换) */
    @NotNull(message = "连接配置不能为空")
    private JsonNode connectionConfig;
}