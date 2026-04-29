package com.xxx.it.works.wecode.v2.modules.category.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类责任人实体
 *
 * <p>对应表 openplatform_v2_category_owner_t</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class CategoryOwner implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称（用于展示，从请求中获取）
     */
    private String userName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 最后更新人
     */
    private String lastUpdateBy;
}
