package com.xxx.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

/**
 * API 认证鉴权服务
 * 
 * <p>职责：API认证鉴权、Scope授权、数据查询接口</p>
 * <p>调用方向：由外向内（消费方 → 提供方）</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class
})
@MapperScan("com.xxx.api.*.mapper")
public class ApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiServerApplication.class, args);
    }

}
