package com.xxx.it.works.wecode.v2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 能力开放平台 - 管理服务
 *
 * <p>职责：分类管理、API/事件/回调管理、权限管理、审批管理</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class
})
@MapperScan("com.xxx.it.works.wecode.v2.modules.*.mapper")
@EnableAsync
public class OpenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenServerApplication.class, args);
    }

}
