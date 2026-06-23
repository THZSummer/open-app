package com.xxx.it.works.wecode.v2.modules.flow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 连接流部署请求
 * <p>
 * API #22: POST /service/open/v2/flows/{flowId}/deploy
 * 部署仅绑定版本，不改变生命周期状态
 * </p>
 */
@Data
public class FlowDeployRequest {

    /** 要部署的版本ID */
    @NotNull(message = "版本ID不能为空")
    private Long versionId;
}
