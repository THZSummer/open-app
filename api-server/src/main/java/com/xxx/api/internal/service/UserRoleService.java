package com.xxx.api.internal.service;

import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;

/**
 * 用户角色查询业务接口
 *
 * <p>根据应用标识 + 用户账号查询用户在应用中的角色列表。
 * 支持 Mock/Real 两种实现策略，通过 {@code user-role.service.impl} 配置切换。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface UserRoleService {

    /**
     * 查询用户在指定应用中的角色
     *
     * @param request 查询请求（包含应用标识和用户账号）
     * @param resolvedAppId 解析后的内部应用ID
     * @return 用户角色查询响应
     */
    UserRoleQueryResponse queryUserRoles(UserRoleQueryRequest request, String resolvedAppId);
}
