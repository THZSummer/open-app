package com.xxx.it.works.wecode.v2.modules.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类责任人响应
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryOwnerResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（返回 string 类型）
     */
    private String id;

    /**
     * 分类ID（返回 string 类型）
     */
    private String categoryId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 创建时间
     */
    private Date createTime;
}
