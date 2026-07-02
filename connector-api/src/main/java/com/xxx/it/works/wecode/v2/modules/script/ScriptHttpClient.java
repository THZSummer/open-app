package com.xxx.it.works.wecode.v2.modules.script;

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

/**
 * Script-side HTTP client exposed to GraalJS sandbox via {@code ctx.http}.
 *
 * <p>Scripts call {@code ctx.http.get(url)} or {@code ctx.http.post(url, body)}
 * to make HTTP calls through this Java wrapper.
 * Methods are exposed via {@link HostAccess.Export} for polyglot access.</p>
 *
 * <p>Controlled by Spring property {@code script.http.client.enabled} (default true).
 * When enabled, {@code ctx.http} is injected into the script context.</p>
 *
 * @author SDDU
 */
public class ScriptHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ScriptHttpClient.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient;

    public ScriptHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @HostAccess.Export
    public Map<String, Object> get(String url) {
        return get(url, null);
    }

    @HostAccess.Export
    public Map<String, Object> get(String url, Map<String, String> headers) {
        return execute("GET", url, headers, null);
    }

    @HostAccess.Export
    public Map<String, Object> post(String url, Object body) {
        return post(url, body, null);
    }

    @HostAccess.Export
    public Map<String, Object> post(String url, Object body, Map<String, String> headers) {
        return execute("POST", url, headers, body);
    }

    @HostAccess.Export
    public Map<String, Object> put(String url, Object body) {
        return put(url, body, null);
    }

    @HostAccess.Export
    public Map<String, Object> put(String url, Object body, Map<String, String> headers) {
        return execute("PUT", url, headers, body);
    }

    private Map<String, Object> execute(String method, String url, Map<String, String> headers, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            if (headers != null) {
                headers.forEach(builder::header);
            }

            if (body != null && !"GET".equalsIgnoreCase(method)) {
                String jsonBody = body instanceof String ? (String) body : toJson(body);
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new HashMap<>();
            result.put("status", response.statusCode());
            result.put("body", tryParseJson(response.body()));
            result.put("headers", flattenHeaders(response.headers().map()));

            log.debug("Script HTTP {} {} → {}", method, url, response.statusCode());
            return result;

        } catch (Exception e) {
            log.warn("Script HTTP call failed: {} {}, error={}", method, url, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("status", 0);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "HTTP call failed");
            error.put("detail", e.getMessage());
            result.put("body", error);
            result.put("headers", new HashMap<>());
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Object tryParseJson(String text) {
        if (text == null || text.isBlank()) return "";
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(text, Map.class);
        } catch (Exception e) {
            return text;
        }
    }

    private String toJson(Object obj) {
        try {
            if (obj instanceof String) return (String) obj;
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private Map<String, String> flattenHeaders(Map<String, java.util.List<String>> map) {
        Map<String, String> flat = new HashMap<>();
        if (map != null) {
            map.forEach((k, v) -> flat.put(k, v != null && !v.isEmpty() ? v.get(0) : ""));
        }
        return flat;
    }
}
