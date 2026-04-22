package com.xxx.open.common.config;

import com.xxx.open.common.interceptor.MockInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Mock 拦截器
        registry.addInterceptor(mockInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
    }
}
