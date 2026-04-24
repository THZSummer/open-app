package com.xxx.open.common.model;

import com.xxx.open.common.enums.AuthTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户上下文模型
 * 
 * <p>承载当前请求用户信息的统一 Bean，支持多种认证策略</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（必填，核心字段）
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 认证方式
     */
    private AuthTypeEnum authType;

    /**
     * 创建默认系统用户上下文
     * 
     * @return 系统用户上下文
     */
    public static UserContext empty() {
        return UserContext.builder()
                .userId("system")
                .userName("系统用户")
                .authType(AuthTypeEnum.NONE)
                .build();
    }

    /**
     * 判断用户是否已认证
     * 
     * @return true-已认证, false-未认证（系统用户）
     */
    public boolean isAuthenticated() {
        return userId != null && !"system".equals(userId);
    }
}