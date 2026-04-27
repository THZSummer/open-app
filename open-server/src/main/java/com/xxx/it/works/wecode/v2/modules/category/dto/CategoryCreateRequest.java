package com.xxx.it.works.wecode.v2.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建分类请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CategoryCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类别名（一级分类必填，子分类为空）
     * 例如：app_type_a, app_type_b, personal_aksk
     */
    private String categoryAlias;

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
     * 父分类ID（创建一级分类时为 null）
     */
    private String parentId;

    /**
     * 排序序号（默认 0）
     */
    private Integer sortOrder;
}
