package com.xxx.it.works.wecode.v2.modules.trigger.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 触发数据
 * <p>
 * 描述 HTTP 触发请求或手动触发的输入数据。
 * type 为触发方式枚举，仅支持 "http" / "manual" (不含 "test")。
 * @JsonAlias 用于向后兼容旧字段名。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriggerData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发送方标识 */
    private String sender;

    /** 消息内容 */
    private String content;

    /** 触发类型: "http" / "manual" (仅此两种，不含 "test") */
    @JsonProperty("type")
    @JsonAlias({"type", "triggerType"})
    private String type;
}