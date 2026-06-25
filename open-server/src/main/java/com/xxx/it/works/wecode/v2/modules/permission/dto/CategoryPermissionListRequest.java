package com.xxx.it.works.wecode.v2.modules.permission.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分类权限列表请求
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
public class CategoryPermissionListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private String categoryId;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 是否需要审核过滤
     */
    private Integer needApproval;

    /**
     * 是否包含子分类权限（默认 true）
     */
    private Boolean includeChildren;

    /**
     * 当前页码（默认 1）
     */
    private Integer curPage;

    /**
     * 每页数量（默认 20）
     */
    private Integer pageSize;

    public CategoryPermissionListRequest(String categoryId, String keyword, Integer needApproval, Boolean includeChildren, Integer curPage, Integer pageSize) {
        this.categoryId = categoryId;
        this.keyword = keyword;
        this.needApproval = needApproval;
        this.includeChildren = includeChildren;
        this.curPage = curPage;
        this.pageSize = pageSize;
    }
}
