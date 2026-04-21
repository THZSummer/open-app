package com.xxx.open.modules.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 权限 DTO
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "权限信息")
public class PermissionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    @Schema(description = "权限ID（string 类型）")
    private String id;

    /**
     * 权限中文名称
     */
    @Schema(description = "权限中文名称")
    private String nameCn;

    /**
     * 权限英文名称
     */
    @Schema(description = "权限英文名称")
    private String nameEn;

    /**
     * Scope 标识
     */
    @Schema(description = "Scope 标识")
    private String scope;

    /**
     * 状态：0=禁用, 1=启用
     */
    @Schema(description = "权限状态")
    private Integer status;
}
