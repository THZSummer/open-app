package com.xxx.event.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 回调触发请求
 * 
 * <p>接口编号：#57</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackInvokeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回调 Scope
     */
    @NotBlank(message = "回调 Scope 不能为空")
    private String callbackScope;

    /**
     * 回调内容
     */
    @NotNull(message = "回调内容不能为空")
    private Map<String, Object> payload;
}
