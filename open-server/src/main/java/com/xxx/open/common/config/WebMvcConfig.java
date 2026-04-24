package com.xxx.open.common.config;

import com.xxx.open.common.interceptor.MockInterceptor;
import com.xxx.open.common.interceptor.UserAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 
 * <p>配置拦截器、跨域、静态资源等</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MockInterceptor mockInterceptor;
    private final UserAuthInterceptor userAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户认证拦截器（优先级最高）
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/**")
                .order(-10);
        
        // 注册 Mock 拦截器
        registry.addInterceptor(mockInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
    }
}
