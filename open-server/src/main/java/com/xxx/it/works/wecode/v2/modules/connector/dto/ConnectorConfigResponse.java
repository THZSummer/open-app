package com.xxx.it.works.wecode.v2.modules.connector.dto;

import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersion;
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

    /** 连接器ID (connector_t.id, 雪花ID转string) */
    private String connectorId;

    /** 连接器版本ID (connector_version_t.id, 雪花ID转string) */
    private String connectorVersionId;

    /** 连接配置JSON (完整 connection_config) */
    private String connectionConfig;

    /** 是否有配置 */
    private boolean hasConfig;

    /** 配置创建时间 (ISO 8601) */
    private String createTime;

    /** 配置最后更新时间 (ISO 8601) */
    private String lastUpdateTime;

    public static ConnectorConfigResponse empty() {
        ConnectorConfigResponse resp = new ConnectorConfigResponse();
        resp.setHasConfig(false);
        return resp;
    }

    /**
     * 从 ConnectorVersion 构建响应
     *
     * @param version       连接器版本实体
     * @param createTime    格式化后的创建时间
     * @param lastUpdateTime 格式化后的更新时间
     */
    public static ConnectorConfigResponse of(ConnectorVersion version, String createTime, String lastUpdateTime) {
        ConnectorConfigResponse resp = new ConnectorConfigResponse();
        resp.setConnectorId(String.valueOf(version.getConnectorId()));
        resp.setConnectorVersionId(String.valueOf(version.getId()));
        resp.setConnectionConfig(version.getConnectionConfig());
        resp.setHasConfig(true);
        resp.setCreateTime(createTime);
        resp.setLastUpdateTime(lastUpdateTime);
        return resp;
    }
}
