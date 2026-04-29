package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 审批操作响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private String id;

    /**
     * 审批状态
     */
    private Integer status;

    /**
     * 提示消息
     */
    private String message;
}
