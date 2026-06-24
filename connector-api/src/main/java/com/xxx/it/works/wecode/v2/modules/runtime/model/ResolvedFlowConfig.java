package com.xxx.it.works.wecode.v2.modules.runtime.model;

import com.xxx.it.works.wecode.v2.modules.connector.entity.ConnectorVersionEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 已解析的连接流配置快照
 * <p>
 * 包含连接流基本信息、已部署版本快照、各连接器节点的版本配置,
 * 以及解析后的 flowConfig (超时/限流/缓存).
 * 由 {@code VersionConfigResolver} 解析后供 DAG 调度器使用.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedFlowConfig {

    /** 连接流基本信息 */
    private FlowEntity flow;

    /** 已部署的版本快照 */
    private FlowVersionEntity flowVersion;

    /** 各连接器节点的版本配置 (nodeId → ConnectorVersion) */
    private Map<String, ConnectorVersionEntity> connectorConfigs;

    /** 解析后的运行时配置 */
    private FlowConfig flowConfig;
}
