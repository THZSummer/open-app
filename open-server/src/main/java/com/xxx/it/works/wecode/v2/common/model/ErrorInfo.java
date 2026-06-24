package com.xxx.it.works.wecode.v2.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 错误信息 POJO (v5.5 升级格式)
 * <p>
 * code 为数字字符串 (如 "6001", "4001")。
 * message 拆分为 messageZh / messageEn 双语。
 * 满足 oneOf 约束：
 * <ul>
 *   <li>内部错误 (6xxxx): 携带 cause 字段</li>
 *   <li>下游错误 (4xx/5xx): 携带 downstreamStatus (Integer) + downstreamBody (String)</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码 (数字字符串，如 "6001", "429")
     */
    private String code;

    /**
     * 中文错误消息
     */
    private String messageZh;

    /**
     * 英文错误消息
     */
    private String messageEn;

    // ====== oneOf 约束 ======

    /**
     * 内部错误原因 (6xxxx 类型错误)
     */
    private String cause;

    /**
     * 下游 HTTP 状态码 (4xx/5xx 类型错误)
     */
    private Integer downstreamStatus;

    /**
     * 下游响应体 (截断 512 字符)
     */
    private String downstreamBody;
}