package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.interceptor.UserResolveInterceptor;
import com.xxx.it.works.wecode.v2.modules.security.AppWhitelistInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * <p>配置拦截器、跨域、静态资源等</p>
 *
 * <p>拦截器执行顺序：
 * <ol>
 *   <li>标准环境用户认证拦截器（基础模块注入，优先级 0）</li>
 *   <li>应用白名单准入拦截器（优先级 5）</li>
 *   <li>用户解析拦截器（优先级 10）</li>
 * </ol>
 *
 * <p>注意：标准环境无需修改此配置</p>
 * <p>标准环境的用户认证拦截器由基础模块注入，优先级为 0</p>
 * <p>本配置的拦截器优先级为 5/10，在标准环境拦截器之后执行</p>
 *
 * @author SDDU Build Agent
 * @version 1.1.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppWhitelistInterceptor appWhitelistInterceptor;
    private final UserResolveInterceptor userResolveInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 注册应用白名单准入拦截器
        // 优先级 5：在标准环境认证拦截器（优先级 0）之后、用户解析拦截器（优先级 10）之前执行
        // 仅对连接器平台的两个核心路径生效
        registry.addInterceptor(appWhitelistInterceptor)
                .addPathPatterns("/service/open/v2/connectors/**",
                                 "/service/open/v2/flows/**")
                .order(5);

        // 注册用户解析拦截器
        // 优先级 10：在标准环境的认证拦截器（优先级 0）和
        // 应用白名单拦截器（优先级 5）之后执行
        registry.addInterceptor(userResolveInterceptor)
                .addPathPatterns("/service/open/v2/**")
                .order(10);
    }
}
