package com.xxx.it.works.wecode.v2.modules.script;

import com.xxx.it.works.wecode.v2.common.error.ErrorCode;
import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;
import com.xxx.it.works.wecode.v2.modules.runtime.context.ExecutionContext;
import com.xxx.it.works.wecode.v2.modules.runtime.executor.NodeExecutor;
import com.xxx.it.works.wecode.v2.modules.runtime.model.NodeOutput;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GraalJS 沙箱脚本节点执行器
 * <p>
 * 在五层纵深防御的 GraalJS 沙箱中执行用户编写的 JavaScript 脚本.
 * 脚本格式: {@code function main(ctx) { ... return result; }}.
 * </p>
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>从节点配置提取 script 源码、timeoutMs、upstreamNodeIds</li>
 *   <li>通过 CtxAssembler 组装上游节点数据为 ctx Map</li>
 *   <li>在 boundedElastic 线程池中执行 GraalJS 脚本</li>
 *   <li>通过 Mono.timeout() 控制超时</li>
 *   <li>将脚本返回值转换为 NodeOutput</li>
 * </ol>
 *
 * <h3>超时与隔离</h3>
 * <ul>
 *   <li>§3.3.4: ③(data.timeoutMs)存在用③不截断, ③不存在回退①(平台全局)</li>
 *   <li>执行线程: boundedElastic (独立线程池)</li>
 *   <li>超时后 Context.close(true) 强制终止</li>
 *   <li>每流最多 10 个脚本节点 (编排保存时校验, 此处不限制)</li>
 * </ul>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class ScriptNodeExecutor implements NodeExecutor {

    /** 脚本节点默认超时 (毫秒), 仅作为极端兜底 */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** Whether script HTTP client is enabled (script.http.client.enabled, default true) */
    @org.springframework.beans.factory.annotation.Value("${script.http.client.enabled:true}")
    private boolean scriptHttpEnabled;

    private final GraalJsContextFactory contextFactory;
    private final CtxAssembler ctxAssembler;
    private final ConnectorApiPropertyService propertyService;

    @Autowired
    public ScriptNodeExecutor(GraalJsContextFactory contextFactory, CtxAssembler ctxAssembler,
                               ConnectorApiPropertyService propertyService) {
        this.contextFactory = contextFactory;
        this.ctxAssembler = ctxAssembler;
        this.propertyService = propertyService;
    }

    @Override
    public String getNodeType() {
        return "script";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeOutput> execute(ExecutionContext ctx, Object nodeConfig) {
        Map<String, Object> config;
        if (nodeConfig instanceof Map) {
            config = (Map<String, Object>) nodeConfig;
        } else {
            log.warn("Script node: nodeConfig is not a Map, type={}", nodeConfig.getClass().getName());
            return Mono.just(buildFailedOutput("unknown", "Invalid node config type"));
        }

        // React Flow 格式: 节点配置在 data 字段内
        Map<String, Object> data = (Map<String, Object>) config.getOrDefault("data", config);
        String nodeId = (String) config.get("id");

        // 1. 提取脚本源码并校验
        String scriptSource = (String) data.get("script");
        if (scriptSource == null || scriptSource.isBlank()) {
            log.warn("Script node {} has no script source", nodeId);
            return Mono.just(buildFailedOutput(nodeId, "Script source is empty"));
        }

        // 2. 提取超时配置 (§3.3.4: ③存在用③不截断, ③不存在回退①)
        // 异步取①平台全局超时, 避免在 reactor/lettuce 线程上 block 导致自死锁
        Object timeoutObj = data.get("timeoutMs");
        Mono<Integer> timeoutMono;
        if (timeoutObj instanceof Number num && num.intValue() > 0) {
            timeoutMono = Mono.just(num.intValue());
        } else {
            timeoutMono = propertyService.getScriptMaxTimeoutSeconds()
                    .defaultIfEmpty(5)
                    .onErrorReturn(5)
                    .map(s -> s * 1000);
        }

        // 3. 提取上游节点 ID 列表
        Object upstreamObj = data.get("upstreamNodeIds");
        final List<String> upstreamNodeIds = upstreamObj instanceof List<?> list ? (List<String>) list : null;

        // 4. 组装 ctx Map
        final Map<String, Object> ctxMap = ctxAssembler.assembleCtx(ctx, upstreamNodeIds);

        // Inject HTTP client for script HTTP calls (controlled by script.http.client.enabled, default true)
        if (scriptHttpEnabled) {
            ctxMap.put("http", new ScriptHttpClient());
        }

        long startTime = System.currentTimeMillis();

        return timeoutMono.flatMap(finalTimeoutMs -> {
            log.info("Script node executing: nodeId={}, timeoutMs={}, upstreamCount={}",
                    nodeId, finalTimeoutMs,
                    upstreamNodeIds != null ? upstreamNodeIds.size() : "(default)");
            // 5. 在虚拟线程中执行 (轻量级, 不占平台线程), 带超时控制
            return Mono.fromCallable(() -> executeScript(scriptSource, ctxMap))
                    .subscribeOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
                    .timeout(Duration.ofMillis(finalTimeoutMs))
                    .map(result -> {
                        long duration = System.currentTimeMillis() - startTime;
                        ctxMap.remove("http");
                        NodeOutput output = new NodeOutput(nodeId, "script", ctxMap, result);
                        output.setStatus("success");
                        output.setDurationMs(duration);
                        log.info("Script node {} executed successfully, duration={}ms, outputKeys={}",
                                nodeId, duration, result.keySet());
                        return output;
                    })
                    .onErrorResume(e -> {
                        long duration = System.currentTimeMillis() - startTime;
                        ctxMap.remove("http");
                        NodeOutput output = buildFailedOutput(nodeId, ctxMap, duration, e);
                        return Mono.just(output);
                    });
        });
    }

    /**
     * 在 GraalJS 沙箱中执行脚本
     * <p>
     * 脚本应定义一个 {@code function main(ctx)} 函数作为入口,
     * 返回一个可以被转换为 Java Map 的对象.
     * </p>
     *
     * @param scriptSource 脚本源码 (包含 main 函数定义)
     * @param ctxMap       上下文 Map (上游节点 input/output + trigger 数据)
     * @return 脚本返回值 (转换为 Java Map)
     * @throws PolyglotException     GraalJS 执行错误
     * @throws IllegalStateException 脚本未定义 main 函数
     */
    private Map<String, Object> executeScript(String scriptSource, Map<String, Object> ctxMap) {
        Context jsContext = null;
        try {
            jsContext = contextFactory.createContext();

            // 注册脚本 (定义 main 函数到全局作用域)
            jsContext.eval("js", scriptSource);

            // 获取 main 函数
            Value bindings = jsContext.getBindings("js");
            Value mainFunc = bindings.getMember("main");

            if (mainFunc == null || !mainFunc.canExecute()) {
                throw new IllegalStateException("Script must define a 'main(ctx)' function");
            }

            // 执行 main(ctx) — ctxMap 作为 Java Map 传入,
            // GraalJS polyglot 层自动将 Map 映射为可属性访问的 JS 对象
            Value resultValue = mainFunc.execute(ctxMap);

            // 转换结果为 Java Map
            if (resultValue == null || resultValue.isNull()) {
                return new HashMap<>();
            }

            // 尝试通过成员遍历转换为 Map (适用于 JS 普通对象)
            try {
                if (resultValue.hasMembers()) {
                    Map<String, Object> resultMap = new HashMap<>();
                    for (String key : resultValue.getMemberKeys()) {
                        resultMap.put(key, safeConvertValue(resultValue.getMember(key)));
                    }
                    return resultMap;
                }
            } catch (Exception ignored) {
                log.debug("Non-standard object, fallback to generic conversion: {}", ignored.getMessage());
            }

            // 通用类型转换
            Object rawResult = resultValue.as(Object.class);
            if (rawResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) rawResult;
                return resultMap;
            }

            // 非 Map 结果包装
            Map<String, Object> wrapped = new HashMap<>();
            wrapped.put("result", rawResult);
            return wrapped;

        } catch (PolyglotException e) {
            log.error("GraalJS execution error: {}", e.getMessage());
            throw e;
        } finally {
            if (jsContext != null) {
                contextFactory.closeContext(jsContext);
            }
        }
    }

    /**
     * 安全地将 GraalJS Value 转换为 Java 对象
     */
    private Object safeConvertValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.hasArrayElements()) {
            if (value.isHostObject()) {
                Object host = value.as(Object.class);
                if (host instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) host;
                    return list;
                }
            }
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(safeConvertValue(value.getArrayElement(i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            if (value.isHostObject()) {
                Object host = value.as(Object.class);
                if (host instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) host;
                    return map;
                }
            }
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, safeConvertValue(value.getMember(key)));
            }
            return map;
        }
        return value.as(Object.class);
    }

    /**
     * 构建失败 NodeOutput (用于前置校验失败场景)
     */
    private NodeOutput buildFailedOutput(String nodeId, String errorMsg) {
        NodeOutput output = new NodeOutput();
        output.setNodeId(nodeId);
        output.setNodeType("script");
        output.setStatus("failed");
        output.setInput(new HashMap<>());
        output.setOutput(new HashMap<>());

        String code;
        String msgZh;
        if (errorMsg.contains("empty") || errorMsg.contains("Script source is empty")) {
            code = ErrorCode.SCRIPT_EMPTY;
            msgZh = "脚本节点[" + nodeId + "]源码为空，请编写脚本";
        } else {
            code = ErrorCode.SCRIPT_SYNTAX_ERROR;
            msgZh = "脚本节点[" + nodeId + "]错误: " + errorMsg;
        }

        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("messageZh", msgZh);
        errorInfo.put("messageEn", "Script error: " + errorMsg);
        output.setErrorInfo(errorInfo);

        return output;
    }

    /**
     * 构建失败 NodeOutput (用于执行异常场景)
     */
    private NodeOutput buildFailedOutput(String nodeId, Map<String, Object> input, long duration, Throwable e) {
        String errorMsg;
        String code;
        String msgZh;

        if (e instanceof PolyglotException pe) {
            if (pe.isCancelled()) {
                code = ErrorCode.SCRIPT_TIMEOUT;
                errorMsg = sanitize("Script execution cancelled (timeout or resource limit): " + pe.getMessage());
                msgZh = "脚本节点[" + nodeId + "]执行超时或被取消";
                log.warn("Script node {} cancelled: {}", nodeId, pe.getMessage());
            } else {
                code = ErrorCode.SCRIPT_RUNTIME_ERROR;
                errorMsg = sanitize("Script execution error: " + pe.getMessage());
                msgZh = "脚本节点[" + nodeId + "]运行时错误: " + errorMsg;
                log.warn("Script node {} execution error: {}", nodeId, pe.getMessage());
            }
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            code = ErrorCode.SCRIPT_TIMEOUT;
            errorMsg = "Script execution timed out";
            msgZh = "脚本节点[" + nodeId + "]执行超时";
            log.warn("Script node {} timed out", nodeId);
        } else if (e instanceof IllegalStateException && e.getMessage() != null
                && e.getMessage().contains("main(ctx)")) {
            code = ErrorCode.SCRIPT_NO_MAIN;
            errorMsg = sanitize(e.getMessage());
            msgZh = "脚本节点[" + nodeId + "]缺少 main(ctx) 函数定义";
        } else {
            code = ErrorCode.SCRIPT_RUNTIME_ERROR;
            errorMsg = sanitize(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            msgZh = "脚本节点[" + nodeId + "]执行失败: " + errorMsg;
            log.error("Script node {} unexpected error: {}", nodeId, errorMsg, e);
        }

        NodeOutput output = new NodeOutput(nodeId, "script", input, new HashMap<>());
        output.setStatus("failed");
        output.setDurationMs(duration);

        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("code", code);
        errorInfo.put("message", errorMsg);
        errorInfo.put("messageEn", errorMsg);
        errorInfo.put("messageZh", msgZh);
        output.setErrorInfo(errorInfo);

        return output;
    }

    /**
     * 清理错误消息中的控制字符 (换行/回车等),
     * 避免 HTTP 头写入时抛出 {@code IllegalArgumentException}.
     */
    private String sanitize(String msg) {
        if (msg == null) { return ""; }
        return msg.replace('\n', ' ').replace('\r', ' ');
    }
}
