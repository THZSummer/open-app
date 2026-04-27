package com.xxx.it.works.wecode.v2.modules.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树形响应
 * 
 * <p>包含 children 字段用于树形结构</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryTreeResponse extends CategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 子分类列表
     */
    private List<CategoryTreeResponse> children = new ArrayList<>();
}
