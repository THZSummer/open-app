package com.xxx.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 事件/回调网关服务
 * 
 * <p>职责：事件消费网关、回调消费网关</p>
 * <p>调用方向：由内向外（提供方 → 消费方）</p>
 * <p>数据存储：Redis（独立），无数据库，通过 api-server 接口获取数据</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@SpringBootApplication
public class EventServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServerApplication.class, args);
    }

}
