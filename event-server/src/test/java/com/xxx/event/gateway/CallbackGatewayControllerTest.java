package com.xxx.event.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.event.EventServerApplication;
import com.xxx.event.gateway.dto.CallbackInvokeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 回调网关控制器测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@SpringBootTest(classes = EventServerApplication.class, 
        properties = {
            "management.health.redis.enabled=false",
            "spring.data.redis.repositories.enabled=false"
        })
@AutoConfigureMockMvc
class CallbackGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testInvokeCallback() throws Exception {
        // 构造请求
        CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                .callbackScope("callback:approval:completed")
                .payload(Map.of(
                        "approvalId", "app001",
                        "status", "approved",
                        "approver", "user001"
                ))
                .build();

        // 发送请求
        mockMvc.perform(post("/gateway/callbacks/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.callbackScope").value("callback:approval:completed"));
    }

    @Test
    void testInvokeCallbackWithoutScope() throws Exception {
        // 构造请求（缺少 callbackScope）
        CallbackInvokeRequest request = CallbackInvokeRequest.builder()
                .payload(Map.of("approvalId", "app001"))
                .build();

        // 发送请求，期望返回 400 错误
        mockMvc.perform(post("/gateway/callbacks/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
