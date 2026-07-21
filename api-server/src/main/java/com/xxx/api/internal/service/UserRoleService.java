package com.xxx.api.internal.service;

import com.xxx.api.internal.dto.UserRoleQueryRequest;
import com.xxx.api.internal.dto.UserRoleQueryResponse;

/**
 * 用户角色查询服务接口
 *
 * <p>承载完整业务流程：凭证校验 → 参数校验 → 应用标识解析 → 角色查询</p>
 *
 * @author SDDU Build Agent
 */
public interface UserRoleService {

    /**
     * 查询用户在应用中的角色
     *
     * @param request 请求参数（appId/hisAppId + userAccount）
     * @param token   请求头 X-Internal-Token（可为 null）
     * @return 角色查询结果
     * @throws com.xxx.api.common.exception.BusinessException 凭证校验失败、参数错误、应用不存在
     */
    UserRoleQueryResponse queryUserRoles(UserRoleQueryRequest request, String token);
}
