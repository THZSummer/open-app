package com.xxx.it.works.wecode.v2.modules.callback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建回调请求
 *
 * <p>注册回调时附带权限定义</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CallbackCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中文名称（必填）
     */
    @NotBlank(message = "中文名称不能为空")
    private String nameCn;

    /**
     * 英文名称（必填）
     */
    @NotBlank(message = "英文名称不能为空")
    private String nameEn;

    /**
     * 所属分类ID（必填）
     */
    @NotBlank(message = "分类ID不能为空")
    private String categoryId;

    /**
     * 权限定义（必填）
     */
    @NotNull(message = "权限定义不能为空")
    @Valid
    private PermissionDefinitionDto permission;

    /**
     * 扩展属性列表（可选）
     * KV 模式：propertyName -> propertyValue
     */
    private List<CallbackPropertyDto> properties;
}
