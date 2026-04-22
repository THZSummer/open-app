package com.xxx.open.modules.callback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限信息 DTO
 * 
 * <p>用于回调响应中展示权限信息</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private String id;

    /**
     * 权限中文名称
     */
    private String nameCn;

    /**
     * 权限英文名称
     */
    private String nameEn;

    /**
     * 权限标识（Scope）
     * 格式：callback:{module}:{identifier}
     */
    private String scope;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;
}
