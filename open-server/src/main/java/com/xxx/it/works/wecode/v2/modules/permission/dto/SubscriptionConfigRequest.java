package com.xxx.it.works.wecode.v2.modules.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 订阅配置请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class SubscriptionConfigRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通道类型
     * 事件：0=内部消息队列, 1=WebHook
     * 回调：0=WebHook, 1=SSE, 2=WebSocket
     */
    @NotNull(message = "通道类型不能为空")
    private Integer channelType;

    /**
     * 通道地址
     */
    private String channelAddress;

    /**
     * 认证类型：0=应用类凭证A, 1=应用类凭证B
     */
    @NotNull(message = "认证类型不能为空")
    private Integer authType;
}
