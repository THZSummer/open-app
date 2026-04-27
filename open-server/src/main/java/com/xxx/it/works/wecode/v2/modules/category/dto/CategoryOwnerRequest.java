package com.xxx.it.works.wecode.v2.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加分类责任人请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CategoryOwnerRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（必填）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户名称（用于展示）
     */
    private String userName;
}
