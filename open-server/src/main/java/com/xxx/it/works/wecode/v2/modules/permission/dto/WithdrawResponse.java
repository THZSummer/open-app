package com.xxx.it.works.wecode.v2.modules.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 撤回响应
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    private String id;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 消息
     */
    private String message;

    /**
     * 通道类型：0=内部消息队列, 1=WebHook
     */
    private Integer channelType;

    /**
     * 通道地址
     */
    private String channelAddress;

    /**
     * 认证类型
     */
    private Integer authType;
}
