package com.xxx.it.works.wecode.v2.modules.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新审批流程请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class ApprovalFlowUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 流程编码
     */
    private String code;

    /**
     * 归属应用ID（V3 新增）
     *
     * <p>NULL=平台级/全局模板，非NULL=应用级定制模板</p>
     */
    private Long appId;

    /**
     * 审批节点列表
     */
    private List<ApprovalNodeDto> nodes;
}
