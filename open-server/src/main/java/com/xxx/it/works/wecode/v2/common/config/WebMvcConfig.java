package com.xxx.it.works.wecode.v2.common.config;

import com.xxx.it.works.wecode.v2.common.interceptor.UserResolveInterceptor;
import com.xxx.it.works.wecode.v2.modules.security.AppWhitelistInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * <p>配置拦截器、跨域、静态资源等</p>
 *
 * <p>注意：标准环境无需修改此配置</p>
 * <p>标准环境的用户认证拦截器由基础模块注入，优先级为 0</p>
 * <p>本配置的拦截器优先级为 10/20，在标准环境拦截器之后执行</p>
 * <p>拦截器作用范围：/service/open/v2/**</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserResolveInterceptor userResolveInterceptor;
    private final AppWhitelistInterceptor appWhitelistInterceptor;

    @Value("${platform.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${platform.file.url-prefix:/service/open/v2/uploads}")
    private String urlPrefix;

    @Value("${ability.file.local-dir:${java.io.tmpdir}/ability-upload}")
    private String abilityFileLocalDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 注册用户解析拦截器
        // 优先级 10：在标准环境的认证拦截器（优先级 0）之后执行
        registry.addInterceptor(userResolveInterceptor)
                .addPathPatterns("/service/open/v2/**")
                .order(10);

        // 注册应用白名单拦截器
        // 优先级 20：在用户解析之后执行白名单准入校验（#15 app_whitelist）
        registry.addInterceptor(appWhitelistInterceptor)
                .addPathPatterns("/service/open/v2/connectors/**", "/service/open/v2/flows/**", "/service/open/v2/executions/**")
                .order(20);
    }

    /**
     * 静态资源映射：urlPrefix + "/**" 映射到磁盘上传目录
     * urlPrefix 默认 /service/open/v2/uploads，与 vite proxy 前缀一致
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 磁盘（用户上传）+ classpath（预设图标，打包进 JAR）
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadDir + "/", "classpath:/static/uploads/");

        // 能力图标/示意图文件（对齐 market-server /ability-files/** 映射）
        registry.addResourceHandler("/ability-files/**")
                .addResourceLocations("file:" + abilityFileLocalDir + "/");
    }
}
