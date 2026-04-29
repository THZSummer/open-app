package com.xxx.event.gateway.controller;

import com.xxx.event.common.model.ApiResponse;
import com.xxx.event.gateway.dto.CallbackInvokeRequest;
import com.xxx.event.gateway.dto.CallbackInvokeResponse;
import com.xxx.event.gateway.service.CallbackGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 回调网关控制器
 * 
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /gateway/callbacks/invoke - 回调触发接口（#57）</li>
 * </ul>
 * 
 * <p>服务定位：由内向外（提供方 → 消费方）</p>
 * <p>回调流程：提供方 → event-server → 消费方（不经内部消息网关）</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Tag(name = "回调网关", description = "回调触发与分发")
@RestController
@RequestMapping("/gateway/callbacks")
@RequiredArgsConstructor
public class CallbackGatewayController {

    private final CallbackGatewayService callbackGatewayService;

    /**
     * 回调触发接口
     * 
     * <p>接口编号：#57</p>
     * <p>业务模块通过此接口触发回调，网关调用已订阅的消费方回调地址</p>
     * <p>流程：提供方 → event-server → 消费方（不经内部消息网关）</p>
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>验证 callback_scope 对应的回调资源存在</li>
     *   <li>查询订阅该回调的应用列表</li>
     *   <li>按订阅配置调用消费方：
     *     <ul>
     *       <li>WebHook：POST 到 channel_address</li>
     *       <li>SSE：推送到 SSE 连接</li>
     *       <li>WebSocket：推送到 WebSocket 连接</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    @Operation(summary = "回调触发", description = "触发回调并调用已订阅的消费方")
    @PostMapping("/invoke")
    public ApiResponse<CallbackInvokeResponse> invokeCallback(@Valid @RequestBody CallbackInvokeRequest request) {
        log.info("Received callback trigger request: scope={}", request.getCallbackScope());
        
        CallbackInvokeResponse response = callbackGatewayService.invokeCallback(request);
        
        return ApiResponse.success(response);
    }

    /**
     * 清除订阅列表缓存
     * 
     * <p>管理接口，用于手动清除缓存</p>
     */
    @Operation(summary = "清除缓存", description = "清除指定 Scope 的订阅列表缓存")
    @DeleteMapping("/cache/{scope}")
    public ApiResponse<Void> clearCache(@PathVariable String scope) {
        log.info("Clearing subscription list cache: scope={}", scope);
        
        callbackGatewayService.clearCache(scope);
        
        return ApiResponse.success();
    }
}
