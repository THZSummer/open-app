package com.xxx.open.common.user.strategy;

import com.xxx.open.common.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户解析策略接口
 * 
 * <p>支持不同环境下的用户信息解析策略，实现策略模式</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface UserResolveStrategy {

    /**
     * 解析用户上下文
     * 
     * @param request HTTP 请求
     * @return 用户上下文，解析失败返回 null
     */
    UserContext resolve(HttpServletRequest request);

    /**
     * 判断当前策略是否支持指定的环境
     * 
     * @param activeProfile 当前激活的环境配置
     * @return true-支持, false-不支持
     */
    boolean supports(String activeProfile);
}