package com.xxx.it.works.wecode.v2.modules.runtime.model;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 透明穿透响应 (v5.8)
 * <p>
 * 替代旧的 ExecutionResult 信封模式, 将出口节点的 body/header 直接暴露为 HTTP 响应体/头.
 * 平台元数据 (executionId/flowId/status/durationMs/code/messageZh/messageEn/cacheStatus) 
 * 不再混在 Body 里, 改为 X- 前缀 HTTP 响应头.
 * </p>
 * <ul>
 *   <li>{@code body} — 出口节点的 output.body 裸数据 (null=空Body)</li>
 *   <li>{@code userHeaders} — 出口节点的 output.header 映射 (用户自定义响应头)</li>
 *   <li>{@code platformHeaders} — X- 前缀的平台元数据头</li>
 *   <li>{@code httpStatus} — HTTP 状态码</li>
 * </ul>
 */
public class TransparentFlowResponse {

    /** 响应体 (出口节点 output.body 裸数据, null=空Body) */
    private Object body;

    /** 用户自定义响应头 (出口节点的 header 映射) */
    private Map<String, String> userHeaders;

    /** 平台元数据头 (X-Flow-Id / X-Execution-Id / X-Status / X-Duration-Ms / X-Code / X-Message-Zh / X-Message-En / X-Cache-Status) */
    private Map<String, String> platformHeaders;

    /** HTTP 状态码 */
    private HttpStatus httpStatus;

    public TransparentFlowResponse() {
        this.userHeaders = new LinkedHashMap<>();
        this.platformHeaders = new LinkedHashMap<>();
    }

    /**
     * 执行完成的响应 (成功/失败/超时)
     *
     * @param flowId      连接流ID
     * @param executionId 执行ID
     * @param status      执行状态: 0=success, 1=failed, 2=timeout
     * @param durationMs  总耗时(毫秒)
     * @param userHeaders 出口节点的 header 映射
     * @param body        出口节点的 body 数据 (裸对象)
     */
    public static TransparentFlowResponse success(String flowId, String executionId,
                                                   int status, long durationMs,
                                                   Map<String, String> userHeaders, Object body) {
        TransparentFlowResponse r = new TransparentFlowResponse();
        r.body = body;
        r.userHeaders = userHeaders != null ? userHeaders : new LinkedHashMap<>();
        r.platformHeaders = new LinkedHashMap<>();
        r.platformHeaders.put("X-Flow-Id", flowId);
        r.platformHeaders.put("X-Execution-Id", executionId);
        r.platformHeaders.put("X-Status", String.valueOf(status));
        r.platformHeaders.put("X-Duration-Ms", String.valueOf(durationMs));
        r.platformHeaders.put("X-Cache-Status", "0"); // 当前未实现缓存
        r.httpStatus = HttpStatus.OK;
        return r;
    }

    /**
     * 前置校验失败响应 (认证/限流/校验等 — 空Body, 只有 X- 头)
     *
     * @param flowId     连接流ID
     * @param httpStatus HTTP 状态码
     * @param code       错误码
     * @param messageZh  中文错误消息
     * @param messageEn  英文错误消息
     */
    public static TransparentFlowResponse preExecutionError(String flowId, HttpStatus httpStatus,
                                                             String code, String messageZh, String messageEn) {
        TransparentFlowResponse r = new TransparentFlowResponse();
        r.body = null;
        r.userHeaders = new LinkedHashMap<>();
        r.platformHeaders = new LinkedHashMap<>();
        r.platformHeaders.put("X-Flow-Id", flowId);
        r.platformHeaders.put("X-Code", code);
        r.platformHeaders.put("X-Message-Zh", messageZh);
        r.platformHeaders.put("X-Message-En", messageEn);
        r.httpStatus = httpStatus;
        return r;
    }

    // ===== Getters & Setters =====

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public Map<String, String> getUserHeaders() {
        return userHeaders;
    }

    public void setUserHeaders(Map<String, String> userHeaders) {
        this.userHeaders = userHeaders;
    }

    public Map<String, String> getPlatformHeaders() {
        return platformHeaders;
    }

    public void setPlatformHeaders(Map<String, String> platformHeaders) {
        this.platformHeaders = platformHeaders;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
