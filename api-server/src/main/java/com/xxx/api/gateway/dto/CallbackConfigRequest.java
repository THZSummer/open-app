package com.xxx.api.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 回调配置查询请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "回调配置查询请求")
public class CallbackConfigRequest {

    @NotBlank(message = "AK不能为空")
    @Schema(description = "应用 Access Key", example = "AK123456789")
    private String ak;

    @NotBlank(message = "Scope不能为空")
    @Schema(description = "回调权限标识（Scope）", example = "callback:approval:completed")
    private String scope;
}