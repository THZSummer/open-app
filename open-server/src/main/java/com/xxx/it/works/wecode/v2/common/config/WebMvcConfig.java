package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.interceptor.UserResolveInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 
 * <p>配置拦截器、跨域、静态资源等</p>
 * 
 * <p>注意：标准环境无需修改此配置</p>
 * <p>标准环境的用户认证拦截器由基础模块注入，优先级为 0</p>
 * <p>本配置的拦截器优先级为 10，在标准环境拦截器之后执行</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserResolveInterceptor userResolveInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户解析拦截器
        // 优先级 10：在标准环境的认证拦截器（优先级 0）之后执行
        registry.addInterceptor(userResolveInterceptor)
                .addPathPatterns("/api/**")
                .order(10);
    }
}
