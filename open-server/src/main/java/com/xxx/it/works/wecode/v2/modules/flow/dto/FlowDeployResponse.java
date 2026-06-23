package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 连接流部署响应
 * <p>
 * API #22: POST /service/open/v2/flows/{flowId}/deploy
 * 部署仅绑定版本
 * </p>
 */
@Data
@Builder
public class FlowDeployResponse {

    /** 连接流ID（String 格式） */
    private String flowId;

    /** 已部署版本ID（String 格式） */
    private String deployedVersionId;

    /** 已部署版本号 */
    private Integer deployedVersionNumber;

    /** 提示信息 */
    private String message;
}
