package com.xxx.open.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis 连接测试接口（临时，测试完可删除）
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/redis")
    public Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试基本连接
            RedisClusterConnection connection = null;
            try {
                connection = redisConnectionFactory.getClusterConnection();
                
                // 获取集群信息
                Properties info = connection.info();
                result.put("connected", true);
                result.put("clusterNodes", connection.clusterGetNodes().size());
                
                // 测试读写
                String testKey = "test:connection:" + System.currentTimeMillis();
                connection.set(testKey.getBytes(), "ok".getBytes());
                byte[] value = connection.get(testKey.getBytes());
                connection.del(testKey.getBytes());
                
                result.put("readWrite", "ok".equals(new String(value)));
                result.put("message", "Redis 集群连接正常");
                
                log.info("Redis 集群连接测试成功，节点数: {}", connection.clusterGetNodes().size());
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("message", "Redis 集群连接失败");
            log.error("Redis 集群连接测试失败", e);
        }
        
        return result;
    }
}