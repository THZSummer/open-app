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
 * 事件发布请求
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
public class EventPublishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件 Topic
     */
    @NotBlank(message = "事件 Topic 不能为空")
    private String topic;

    /**
     * 事件内容
     */
    @NotNull(message = "事件内容不能为空")
    private Map<String, Object> payload;
}
