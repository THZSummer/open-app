package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 连接流生命周期操作响应 (start/stop/deploy/invalidate/recover)
 * <p>
 * 返回操作后的生命周期状态
 * </p>
 */
@Data
@Builder
public class FlowLifecycleResponse {

    /** 连接流ID (string格式) */
    private String flowId;

    /** 操作后的生命周期状态 */
    private Integer lifecycleStatus;

    /** 操作时间 */
    private String lastUpdateTime;
}
