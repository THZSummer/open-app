package com.xxx.open.modules.callback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限定义 DTO（用于创建请求）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDefinitionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限中文名称（必填）
     */
    private String nameCn;

    /**
     * 权限英文名称（必填）
     */
    private String nameEn;

    /**
     * 权限标识（Scope，必填）
     * 格式：callback:{module}:{identifier}
     */
    private String scope;

    /**
     * 审批流程ID（可选，不填使用默认流程）
     */
    private String approvalFlowId;
}
