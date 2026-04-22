package com.xxx.event.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事件发布响应
 * 
 * <p>接口编号：#56</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPublishResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件 Topic
     */
    private String topic;

    /**
     * 订阅者数量
     */
    private Integer subscribers;

    /**
     * 消息
     */
    private String message;
}
