package com.xxx.it.works.wecode.v2.modules.connector.dto;

import lombok.Data;

/**
 * 连接配置响应
 * <p>
 * API #6: GET /service/open/v2/connectors/{connectorId}/config
 * 无配置时提示"暂无配置"
 * </p>
 */
@Data
public class ConnectorConfigResponse {

    /** 连接配置JSON (完整 connection_config) */
    private String connectionConfig;

    /** 是否有配置 */
    private boolean hasConfig;

    public static ConnectorConfigResponse empty() {
        ConnectorConfigResponse resp = new ConnectorConfigResponse();
        resp.setHasConfig(false);
        return resp;
    }

    public static ConnectorConfigResponse of(String connectionConfig) {
        ConnectorConfigResponse resp = new ConnectorConfigResponse();
        resp.setConnectionConfig(connectionConfig);
        resp.setHasConfig(true);
        return resp;
    }
}