package com.xxx.open.modules.callback.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新回调请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CallbackUpdateRequest implements Serializable {

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
     * 所属分类ID
     */
    private String categoryId;

    /**
     * 权限定义（可部分更新）
     */
    @Valid
    private PermissionDefinitionDto permission;

    /**
     * 扩展属性列表（KV 模式）
     * 更新时会替换所有属性
     */
    private List<CallbackPropertyDto> properties;
}
