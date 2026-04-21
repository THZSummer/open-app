package com.xxx.open.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一 API 响应格式
 * 
 * <p>所有接口返回统一格式：{code, messageZh, messageEn, data, page}</p>
 * 
 * @param <T> 数据类型
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private String code;

    /**
     * 中文消息
     */
    private String messageZh;

    /**
     * 英文消息
     */
    private String messageEn;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 分页信息
     */
    private PageResponse page;

    /**
     * 分页响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse implements Serializable {
        
        private static final long serialVersionUID = 1L;

        /**
         * 当前页码（从 1 开始）
         */
        private Integer curPage;

        /**
         * 每页大小
         */
        private Integer pageSize;

        /**
         * 总记录数
         */
        private Long total;

        /**
         * 总页数
         */
        private Integer totalPages;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .build();
    }

    /**
     * 成功响应（有数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .data(data)
                .build();
    }

    /**
     * 成功响应（分页数据）
     */
    public static <T> ApiResponse<T> success(T data, PageResponse page) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .data(data)
                .page(page)
                .build();
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(String code, String messageZh, String messageEn) {
        return ApiResponse.<T>builder()
                .code(code)
                .messageZh(messageZh)
                .messageEn(messageEn)
                .build();
    }
}
