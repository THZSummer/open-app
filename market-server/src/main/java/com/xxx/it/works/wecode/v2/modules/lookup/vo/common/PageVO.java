package com.xxx.it.works.wecode.v2.modules.lookup.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页返回VO
 *
 * @param <T> 列表数据类型
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页返回结果")
public class PageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    @Builder.Default
    private List<T> list = Collections.emptyList();

    /**
     * 总记录数
     */
    @Schema(description = "总记录数",
            example = "100")
    private Long total;

    /**
     * 总页数
     */
    @Schema(description = "总页数",
            example = "10")
    private Integer pages;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码",
            example = "1")
    private Integer pageNum;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数",
            example = "10")
    private Integer pageSize;

    /**
     * 创建分页VO（使用现有列表）
     *
     * @param list     数据列表
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param <T>      数据类型
     * @return 分页VO
     */
    public static <T> PageVO<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        int pages = (int) ((total + pageSize - 1) / pageSize);
        return PageVO.<T>builder()
                .list(list)
                .total(total)
                .pages(pages)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    /**
     * 创建空分页VO
     *
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param <T>      数据类型
     * @return 空分页VO
     */
    public static <T> PageVO<T> empty(Integer pageNum, Integer pageSize) {
        return PageVO.<T>builder()
                .list(Collections.emptyList())
                .total(0L)
                .pages(0)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }
}
