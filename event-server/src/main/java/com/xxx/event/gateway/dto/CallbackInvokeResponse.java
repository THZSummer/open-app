package com.xxx.event.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 回调触发响应
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
public class CallbackInvokeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回调 Scope
     */
    private String callbackScope;

    /**
     * 订阅者数量
     */
    private Integer subscribers;

    /**
     * 消息
     */
    private String message;
}
