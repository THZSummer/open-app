package com.xxx.it.works.wecode.v2.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String messageZh;
    private String messageEn;
    private T data;
    private PageResponse page;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer curPage;
        private Integer pageSize;
        private Long total;
        private Integer totalPages;
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, PageResponse page) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("操作成功")
                .messageEn("Success")
                .data(data)
                .page(page)
                .build();
    }

    public static PageResponse buildPage(int curPage, int pageSize, long total) {
        return PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String messageZh, String messageEn) {
        return ApiResponse.<T>builder()
                .code(code)
                .messageZh(messageZh)
                .messageEn(messageEn)
                .build();
    }
}
