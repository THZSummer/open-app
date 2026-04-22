package com.xxx.open.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * API 列表查询请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "API 列表查询请求")
public class ApiListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID过滤
     */
    @Schema(description = "分类ID过滤")
    private String categoryId;

    /**
     * 状态过滤（0=草稿, 1=待审, 2=已发布, 3=已下线）
     */
    @Schema(description = "状态过滤")
    private Integer status;

    /**
     * 搜索关键词（名称、Scope）
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 当前页码（从 1 开始）
     */
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer curPage = 1;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", defaultValue = "20")
    private Integer pageSize = 20;
}
