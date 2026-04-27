package com.xxx.it.works.wecode.v2.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新分类请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CategoryUpdateRequest implements Serializable {

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
     * 排序序号
     */
    private Integer sortOrder;
}
