package com.xxx.it.works.wecode.v2.modules.category.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类实体
 * 
 * <p>对应表 openplatform_v2_category_t</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类别名（仅根分类需要）
     * 例如：app_type_a, app_type_b, personal_aksk
     */
    private String categoryAlias;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 路径：/根ID/父ID/当前ID/
     * 用于子树查询优化
     */
    private String path;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

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
