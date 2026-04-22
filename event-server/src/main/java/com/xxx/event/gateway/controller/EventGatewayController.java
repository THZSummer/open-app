package com.xxx.event.gateway.controller;

import com.xxx.event.common.model.ApiResponse;
import com.xxx.event.gateway.dto.EventPublishRequest;
import com.xxx.event.gateway.dto.EventPublishResponse;
import com.xxx.event.gateway.service.EventGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 事件网关控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /gateway/events/publish - 事件发布接口（#56）</li>
 * </ul>
 * 
 * <p>服务定位：由内向外（提供方 → 消费方）</p>
 * <p>事件流程：提供方 → 内部消息网关 → event-server → 消费方</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "事件网关", description = "事件发布与分发")
@RestController
@RequestMapping("/gateway/events")
@RequiredArgsConstructor
public class EventGatewayController {

    private final EventGatewayService eventGatewayService;

    /**
     * 事件发布接口
     * 
     * <p>接口编号：#56</p>
     * <p>业务模块通过此接口发布事件，网关分发至订阅的消费方</p>
     * <p>流程：提供方 → 内部消息网关 → event-server → 消费方</p>
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>验证 Topic 对应的事件资源存在且已发布</li>
     *   <li>查询订阅该事件的应用列表</li>
     *   <li>按订阅配置分发事件：
     *     <ul>
     *       <li>WebHook：POST 到 channel_address</li>
     *       <li>企业内部消息队列：推送到对应队列</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    @Operation(summary = "事件发布", description = "发布事件并分发至订阅的消费方")
    @PostMapping("/publish")
    public ApiResponse<EventPublishResponse> publishEvent(@Valid @RequestBody EventPublishRequest request) {
        log.info("接收到事件发布请求: topic={}", request.getTopic());
        
        EventPublishResponse response = eventGatewayService.publishEvent(request);
        
        return ApiResponse.success(response);
    }

    /**
     * 清除订阅列表缓存
     * 
     * <p>管理接口，用于手动清除缓存</p>
     */
    @Operation(summary = "清除缓存", description = "清除指定 Topic 的订阅列表缓存")
    @DeleteMapping("/cache/{topic}")
    public ApiResponse<Void> clearCache(@PathVariable String topic) {
        log.info("清除订阅列表缓存: topic={}", topic);
        
        eventGatewayService.clearCache(topic);
        
        return ApiResponse.success();
    }
}
