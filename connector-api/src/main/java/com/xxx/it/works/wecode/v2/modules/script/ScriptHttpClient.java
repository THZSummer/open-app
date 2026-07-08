package com.xxx.it.works.wecode.v2.modules.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Script-side HTTP client exposed to GraalJS sandbox via {@code ctx.http}.
 *
 * <p>Single unified entry: {@code ctx.http.request(method, url, opts)}.
 * Acts as a transparent proxy from JS to JVM HTTP — no restrictions on
 * method, headers, or body format. All future parameters extend {@code opts}.</p>
 *
 * <p>Controlled by Spring property {@code script.http.client.enabled} (default true).
 * When enabled, {@code ctx.http} is injected into the script context.</p>
 *
 * <p>Uses a dedicated thread pool initialized at class-load time (before any
 * polyglot context exists) to perform HTTP calls outside the GraalVM sandbox,
 * bypassing {@code allowIO(false)} restrictions.</p>
 *
 * @author SDDU
 */
public class ScriptHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ScriptHttpClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int FUTURE_TIMEOUT_SECONDS = 3;

    private static final ExecutorService SHARED_EXECUTOR;
    private static final HttpClient SHARED_HTTP_CLIENT;

    static {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "script-http-worker");
            t.setDaemon(true);
            return t;
        });
        tpe.prestartAllCoreThreads();
        SHARED_EXECUTOR = tpe;
        SHARED_HTTP_CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private final HttpClient httpClient;
    private final ExecutorService executor;

    public ScriptHttpClient() {
        this.httpClient = SHARED_HTTP_CLIENT;
        this.executor = SHARED_EXECUTOR;
    }

    /**
     * Unified HTTP request method — the only entry point exposed to GraalJS.
     *
     * @param method HTTP method string, no whitelist (any value passthrough)
     * @param url    full URL including query parameters
     * @param opts   optional config: { headers: {...}, body: ... }
     *               body as JS object → JSON serialized;
     *               body as string → direct passthrough;
     *               body omitted → no body
     * @return response map: { status, body, headers }
     */
    @HostAccess.Export
    public Map<String, Object> request(String method, String url) {
        return request(method, url, null);
    }

    @HostAccess.Export
    public Map<String, Object> request(String method, String url, Map<String, Object> opts) {
        Map<String, String> headers = extractHeadersDirect(opts);
        Object body = (opts != null) ? resolveBody(opts.get("body")) : null;

        Callable<Map<String, Object>> task = () -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

                if (headers != null) {
                    headers.forEach((k, v) -> {
                        if (k != null && v != null) {
                            builder.header(k, v);
                        }
                    });
                }

                if (body != null) {
                    String bodyStr;
                    if (body instanceof String) {
                        bodyStr = (String) body;
                    } else {
                        bodyStr = OBJECT_MAPPER.writeValueAsString(body);
                        if (!hasContentType(headers)) {
                            builder.header("Content-Type", "application/json");
                        }
                    }
                    builder.method(method, HttpRequest.BodyPublishers.ofString(bodyStr));
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.noBody());
                }

                HttpResponse<String> response = httpClient.send(
                        builder.build(), HttpResponse.BodyHandlers.ofString());

                Map<String, Object> result = new HashMap<>();
                result.put("status", response.statusCode());
                result.put("body", tryParseJson(response.body()));
                result.put("headers", flattenHeaders(response.headers().map()));

                log.debug("Script HTTP {} {} -> {}", method, url, response.statusCode());
                return result;

            } catch (Exception e) {
                log.warn("Script HTTP call failed: {} {}, error={}", method, url, e.getMessage());
                return errorResult("HTTP call failed: " + e.getClass().getSimpleName(), e.getMessage());
            }
        };

        Future<Map<String, Object>> future = executor.submit(task);
        try {
            return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Script HTTP timeout: {} {} (>{}s)", method, url, FUTURE_TIMEOUT_SECONDS);
            return errorResult("HTTP call timed out",
                    "Request exceeded " + FUTURE_TIMEOUT_SECONDS + "s timeout");
        } catch (Exception e) {
            log.warn("Script HTTP executor error: {} {}, error={}", method, url, e.getMessage());
            return errorResult("HTTP call failed: " + e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private Object resolveBody(Object body) {
        if (body == null) return null;
        if (body instanceof String) return body;
        if (body instanceof Map) return resolveValue(body);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(Object val) {
        if (val instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) val).entrySet()) {
                result.put(String.valueOf(entry.getKey()), resolveValue(entry.getValue()));
            }
            return result;
        }
        if (val instanceof java.util.List) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : (java.util.List<Object>) val) {
                list.add(resolveValue(item));
            }
            return list;
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractHeadersDirect(Map<String, Object> opts) {
        if (opts == null) return null;
        Object raw = opts.get("headers");
        if (!(raw instanceof Map)) return null;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private boolean hasContentType(Map<String, String> headers) {
        if (headers == null) {
            return false;
        }
        return headers.keySet().stream()
                .anyMatch(k -> k != null && k.equalsIgnoreCase("Content-Type"));
    }

    @SuppressWarnings("unchecked")
    private Object tryParseJson(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            return OBJECT_MAPPER.readValue(text, Map.class);
        } catch (Exception e) {
            return text;
        }
    }

    private Map<String, String> flattenHeaders(Map<String, java.util.List<String>> map) {
        Map<String, String> flat = new HashMap<>();
        if (map != null) {
            map.forEach((k, v) -> flat.put(k, v != null && !v.isEmpty() ? v.get(0) : ""));
        }
        return flat;
    }

    private Map<String, Object> errorResult(String message, String detail) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 0);
        Map<String, Object> error = new HashMap<>();
        error.put("message", message);
        error.put("detail", detail != null ? detail : "");
        result.put("body", error);
        result.put("headers", new HashMap<>());
        return result;
    }
}
