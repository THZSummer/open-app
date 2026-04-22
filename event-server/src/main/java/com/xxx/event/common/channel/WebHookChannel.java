package com.xxx.event.common.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * WebHook 通道实现
 * 
 * <p>负责通过 WebHook 方式发送事件和回调</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebHookChannel {

    private final RestTemplate restTemplate;

    /**
     * 发送事件到 WebHook 地址
     * 
     * @param channelAddress WebHook 地址
     * @param payload 事件内容
     * @return 是否发送成功
     */
    @Async
    public void sendEvent(String channelAddress, Map<String, Object> payload) {
        sendWebHook(channelAddress, "event", payload);
    }

    /**
     * 发送回调到 WebHook 地址
     * 
     * @param channelAddress WebHook 地址
     * @param payload 回调内容
     * @return 是否发送成功
     */
    @Async
    public void sendCallback(String channelAddress, Map<String, Object> payload) {
        sendWebHook(channelAddress, "callback", payload);
    }

    /**
     * 发送 WebHook 请求
     * 
     * @param url WebHook 地址
     * @param type 类型（event/callback）
     * @param payload 内容
     */
    private void sendWebHook(String url, String type, Map<String, Object> payload) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("发送 WebHook: type={}, url={}", type, url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WebHook 发送成功: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            } else {
                log.warn("WebHook 发送失败: type={}, url={}, status={}, duration={}ms",
                        type, url, response.getStatusCode(), duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("WebHook 发送异常: type={}, url={}, duration={}ms", type, url, duration, e);
        }
    }

    /**
     * 同步发送 WebHook 请求（用于测试）
     * 
     * @param url WebHook 地址
     * @param payload 内容
     * @return 是否发送成功
     */
    public boolean sendSync(String url, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("WebHook 同步发送失败: url={}", url, e);
            return false;
        }
    }
}
