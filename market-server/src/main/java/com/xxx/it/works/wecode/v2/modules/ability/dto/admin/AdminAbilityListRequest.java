package com.xxx.it.works.wecode.v2.modules.ability.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 能力列表查询请求 DTO
 *
 * <p>支持分页、关键字模糊搜索、排序字段和排序方向。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Schema(description = "能力列表查询请求")
public class AdminAbilityListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer curPage = 1;

    @Schema(description = "每页数量，最大 100", defaultValue = "20")
    private Integer pageSize = 20;

    @Schema(description = "按中文名/英文名模糊搜索")
    private String keyword;

    @Schema(description = "排序字段", defaultValue = "order_num")
    private String sortField = "order_num";

    @Schema(description = "排序方向：asc/desc", defaultValue = "asc")
    private String sortOrder = "asc";

    /**
     * 排序字段白名单（防止 SQL 注入）
     */
    public static final java.util.Set<String> SORT_FIELD_WHITELIST = java.util.Set.of(
            "order_num", "ability_type", "ability_name_cn", "ability_name_en",
            "create_time", "last_update_time"
    );

    /**
     * 校验并规范化排序字段
     *
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean validateSortField() {
        if (sortField == null) {
            sortField = "order_num";
            return true;
        }
        // 将驼峰命名转为下划线命名以匹配白名单
        String dbField = camelToUnderscore(sortField);
        if (SORT_FIELD_WHITELIST.contains(dbField)) {
            sortField = dbField;
            return true;
        }
        return false;
    }

    /**
     * 校验并规范化排序方向
     */
    public boolean validateSortOrder() {
        if (sortOrder == null) {
            sortOrder = "asc";
            return true;
        }
        String lower = sortOrder.toLowerCase();
        if ("asc".equals(lower) || "desc".equals(lower)) {
            sortOrder = lower;
            return true;
        }
        return false;
    }

    private static String camelToUnderscore(String camel) {
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
