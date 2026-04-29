package com.xxx.it.works.wecode.v2.modules.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 分类响应
 *
 * <p>包含完整路径信息：path 和 categoryPath</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID（返回 string 类型）
     */
    private String id;

    /**
     * 分类别名
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
     * 父分类ID（返回 string 类型）
     */
    private String parentId;

    /**
     * 分类路径，如 /1/2/
     */
    private String path;

    /**
     * 完整分类路径名称数组，如 ["A类应用权限", "IM业务"]
     */
    private List<String> categoryPath;

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
     * 创建人
     */
    private String createBy;
}
