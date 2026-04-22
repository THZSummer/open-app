package com.xxx.api.scope.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户授权列表查询请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class UserAuthorizationListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID过滤
     */
    private String userId;

    /**
     * 应用ID过滤
     */
    private String appId;

    /**
     * 搜索关键词（用户名、应用名）
     */
    private String keyword;

    /**
     * 当前页码，默认 1
     */
    private Integer curPage = 1;

    /**
     * 每页数量，默认 20
     */
    private Integer pageSize = 20;
}
