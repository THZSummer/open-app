package com.xxx.it.works.wecode.v2.modules.flow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新编排配置请求
 * <p>
 * API #16: PUT /service/open/v2/flows/{flowId}/config
 * 编辑即生效, orchestration_config 全文替换
 * 编排校验: 无节点时拒绝保存
 * </p>
 */
@Data
public class FlowConfigUpdateRequest {

    /** 编排配置JSON (完整替换) */
    @NotBlank(message = "编排配置不能为空")
    private String orchestrationConfig;
}