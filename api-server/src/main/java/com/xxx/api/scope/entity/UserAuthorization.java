package com.xxx.api.scope.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户授权实体
 * 
 * <p>对应表 openplatform_v2_user_authorization_t</p>
 * <p>记录用户对应用的 Scope 授权</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class UserAuthorization implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 授权ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 权限范围数组（JSON 格式）
     */
    private String scopes;

    /**
     * 过期时间
     */
    private Date expiresAt;

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

    /**
     * 撤销时间
     */
    private Date revokedAt;

    /**
     * 解析 scopes JSON 为列表
     */
    public List<String> getScopeList() {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        try {
            // 简单的 JSON 数组解析（假设格式为 ["scope1","scope2"]）
            String json = scopes.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                if (json.isEmpty()) {
                    return List.of();
                }
                String[] items = json.split(",");
                return java.util.Arrays.stream(items)
                        .map(item -> item.trim().replace("\"", ""))
                        .toList();
            }
        } catch (Exception e) {
            // 解析失败返回空列表
        }
        return List.of();
    }
}
