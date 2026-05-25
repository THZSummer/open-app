package com.xxx.it.works.wecode.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 连接器平台运行时服务
 * <p>
 * 基于 Spring WebFlux + R2DBC 的全 Reactive 栈,
 * 承载同步调度执行引擎、HTTP 触发入口、测试执行接口。
 * </p>
 */
@SpringBootApplication
public class ConnectorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectorApiApplication.class, args);
    }
}