package com.xxx.api.modules.appmember.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户角色查询响应
 *
 * <p>返回解析后的内部应用ID及用户在应用中的角色编码列表。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 解析后的内部应用ID
     */
    private String appId;

    /**
     * 用户在应用中的角色编码列表
     * <ul>
     *   <li>0 - 开发者 (Developer)</li>
     *   <li>1 - 拥有者 (Owner)</li>
     *   <li>2 - 管理员 (Admin)</li>
     * </ul>
     */
    private Integer[] roles;
}
