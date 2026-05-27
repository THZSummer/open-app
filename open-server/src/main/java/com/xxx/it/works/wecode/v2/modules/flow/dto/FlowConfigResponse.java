package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Data;

/**
 * 编排配置响应
 * <p>
 * API #15: GET /service/open/v2/flows/{flowId}/config
 * 无配置时提示"暂无配置"
 * </p>
 */
@Data
public class FlowConfigResponse {

    /** 编排配置JSON (完整 orchestration_config) */
    private String orchestrationConfig;

    /** 是否有配置 */
    private boolean hasConfig;

    public static FlowConfigResponse empty() {
        FlowConfigResponse resp = new FlowConfigResponse();
        resp.setHasConfig(false);
        return resp;
    }

    public static FlowConfigResponse of(String orchestrationConfig) {
        FlowConfigResponse resp = new FlowConfigResponse();
        resp.setOrchestrationConfig(orchestrationConfig);
        resp.setHasConfig(true);
        return resp;
    }
}