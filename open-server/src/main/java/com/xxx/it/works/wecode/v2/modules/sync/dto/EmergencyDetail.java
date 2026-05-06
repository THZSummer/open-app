package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 应急接口详情
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订阅关系ID
     */
    private Long id;

    /**
     * 操作状态：updated, inserted, failed
     */
    private String status;

    /**
     * 操作消息
     */
    private String message;

    /**
     * 错误信息（失败时）
     */
    private String error;
}
