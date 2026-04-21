package com.xxx.event.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.event.EventServerApplication;
import com.xxx.event.gateway.dto.EventPublishRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 事件网关控制器测试
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@SpringBootTest(classes = EventServerApplication.class)
@AutoConfigureMockMvc
class EventGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testPublishEvent() throws Exception {
        // 构造请求
        EventPublishRequest request = EventPublishRequest.builder()
                .topic("im.message.received")
                .payload(Map.of(
                        "messageId", "msg001",
                        "content", "Hello World",
                        "sender", "user001"
                ))
                .build();

        // 发送请求
        mockMvc.perform(post("/gateway/events/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.topic").value("im.message.received"));
    }

    @Test
    void testPublishEventWithoutTopic() throws Exception {
        // 构造请求（缺少 topic）
        EventPublishRequest request = EventPublishRequest.builder()
                .payload(Map.of("messageId", "msg001"))
                .build();

        // 发送请求，期望返回 400 错误
        mockMvc.perform(post("/gateway/events/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
