package com.xxx.it.works.wecode.v2.modules.flow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 创建连接流响应
 * <p>
 * API #8: POST /service/open/v2/flows
 * 返回创建的连接流ID (string)
 * </p>
 */
@Data
@Builder
public class FlowCreateResponse {

    /** 连接流ID (string格式) */
    private String id;
}