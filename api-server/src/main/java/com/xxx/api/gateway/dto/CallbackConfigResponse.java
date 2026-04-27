package com.xxx.api.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调配置查询响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "回调配置查询响应")
public class CallbackConfigResponse {

    @Schema(description = "应用 Access Key", example = "AK123456789")
    private String ak;

    @Schema(description = "回调权限标识", example = "callback:approval:completed")
    private String scope;

    @Schema(description = "通道类型（1=WebHook, 2=SSE, 3=WebSocket）", example = "1")
    private Integer channelType;

    @Schema(description = "通道地址", example = "https://webhook.example.com/callbacks")
    private String channelAddress;

    @Schema(description = "认证类型（1=SOA, 2=APIG）", example = "1")
    private Integer authType;
}