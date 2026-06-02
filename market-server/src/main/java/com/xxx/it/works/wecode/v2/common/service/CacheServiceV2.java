package com.xxx.it.works.wecode.v2.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 缓存服务 V2
 * <p>使用 RedisTemplate 实现缓存清除</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Slf4j
@Service
public class CacheServiceV2 {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 清除LookUp项列表缓存
     * 缓存key: OPENPLATFORM:LOOK:UP:ITEM:{path}:{classifyCode}
     */
    public void clearLookUpItemCache(String path, String classifyCode) {
        String key = "OPENPLATFORM:LOOK:UP:ITEM:" + path + ":" + classifyCode;
        redisTemplate.delete(key);
        log.info("清除LookUp项列表缓存: {}", key);
    }

    /**
     * 清除数据字典缓存
     * 缓存key: OPENPLATFORM:DICTIONARY:INFO:{path}:{code}
     */
    public void clearDictionaryCache(String path, String code) {
        String key = "OPENPLATFORM:DICTIONARY:INFO:" + path + ":" + code;
        redisTemplate.delete(key);
        log.info("清除数据字典缓存: {}", key);
    }

}
