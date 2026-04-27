package com.xxx.event.common.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证头字段配置
 * 
 * <p>支持自定义头字段名称，未配置时使用默认值</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 * @since 2026-04-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.headers")
public class AuthHeaderProperties {
    
    /**
     * SOA 认证头字段名称
     * 默认值：X-SOA-TOKEN
     */
    private String soaToken = "X-SOA-TOKEN";
    
    /**
     * APIG AppId 头字段名称
     * 默认值：X-APIG-APPID
     */
    private String apigAppId = "X-APIG-APPID";
    
    /**
     * APIG AppKey 头字段名称
     * 默认值：X-APIG-APPKEY
     */
    private String apigAppKey = "X-APIG-APPKEY";
    
    /**
     * AKSK Token 头字段名称
     * 默认值：X-AKSK-TOKEN
     */
    private String akskToken = "X-AKSK-TOKEN";
}
