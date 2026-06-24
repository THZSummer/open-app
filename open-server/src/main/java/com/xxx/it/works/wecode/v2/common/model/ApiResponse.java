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


    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessageZh() { return messageZh; }
    public void setMessageZh(String messageZh) { this.messageZh = messageZh; }
    public String getMessageEn() { return messageEn; }
    public void setMessageEn(String messageEn) { this.messageEn = messageEn; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public PageResponse getPage() { return page; }
    public void setPage(PageResponse page) { this.page = page; }

    public static <T> ApiResponseBuilder<T> builder() { return new ApiResponseBuilder<>(); }

    public static class ApiResponseBuilder<T> {
        private String code;
        private String messageZh;
        private String messageEn;
        private T data;
        private PageResponse page;

        public ApiResponseBuilder<T> code(String code) { this.code = code; return this; }
        public ApiResponseBuilder<T> messageZh(String messageZh) { this.messageZh = messageZh; return this; }
        public ApiResponseBuilder<T> messageEn(String messageEn) { this.messageEn = messageEn; return this; }
        public ApiResponseBuilder<T> data(T data) { this.data = data; return this; }
        public ApiResponseBuilder<T> page(PageResponse page) { this.page = page; return this; }
        public ApiResponse<T> build() { return new ApiResponse<>(code, messageZh, messageEn, data, page); }
    }

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


        public Integer getCurPage() { return curPage; }
        public void setCurPage(Integer curPage) { this.curPage = curPage; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public static PageResponseBuilder builder() { return new PageResponseBuilder(); }

        public static class PageResponseBuilder {
            private Integer curPage;
            private Integer pageSize;
            private Long total;
            private Integer totalPages;

            public PageResponseBuilder curPage(Integer curPage) { this.curPage = curPage; return this; }
            public PageResponseBuilder pageSize(Integer pageSize) { this.pageSize = pageSize; return this; }
            public PageResponseBuilder total(Long total) { this.total = total; return this; }
            public PageResponseBuilder totalPages(Integer totalPages) { this.totalPages = totalPages; return this; }
            public PageResponse build() { return new PageResponse(curPage, pageSize, total, totalPages); }
        }
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("æä½æå")
                .messageEn("Success")
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("æä½æå")
                .messageEn("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, PageResponse page) {
        return ApiResponse.<T>builder()
                .code("200")
                .messageZh("æä½æå")
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
