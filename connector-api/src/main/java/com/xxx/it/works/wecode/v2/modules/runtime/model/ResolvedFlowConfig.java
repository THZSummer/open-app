package com.xxx.it.works.wecode.v2.modules.runtime.model;

import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowEntity;
import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已解析的连接流配置快照
 * <p>
 * 包含连接流基本信息、已部署版本快照、解析后的 flowConfig (超时/限流/缓存).
 * v6.0: connector 节点配置直接从 node.data.connectorVersionConfig 快照获取,
 * 不再需要 connectorConfigs Map.
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

    /** 解析后的运行时配置 */
    private FlowConfig flowConfig;
}
