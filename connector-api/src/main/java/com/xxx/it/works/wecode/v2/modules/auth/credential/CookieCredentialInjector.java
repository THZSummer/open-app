package com.xxx.it.works.wecode.v2.modules.auth.credential;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Cookie 认证凭据注入器
 *
 * <p>将 Cookie 凭据注入到请求头 Cookie 字段中</p>
 * <p>从认证配置中读取 cookieName 和 cookieValue，拼接为标准 Cookie 格式</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §3.3 连接器配置认证类型
 */
@Slf4j
@Component
public class CookieCredentialInjector implements CredentialInjector {

    @Override
    public String getAuthType() {
        return "COOKIE";
    }

    @Override
    public void inject(Map<String, Object> authConfig, Map<String, String> headers) {
        // 从认证配置中读取 Cookie 名称和值（值由 flow mapping 解析后传入）
        String cookieName = (String) authConfig.get("cookieName");
        String cookieValue = (String) authConfig.get("cookieValue");

        // 校验必要参数：cookieName 和 cookieValue 均不能为空
        if (cookieName == null || cookieName.isEmpty() || cookieValue == null || cookieValue.isEmpty()) {
            log.warn("Cookie credential injection skipped: cookieName or cookieValue is empty");
            return;
        }

        // 设置 Cookie 请求头，格式为 "name=value"
        headers.put("Cookie", cookieName + "=" + cookieValue);
        log.info("Cookie credential injected, cookieName={}", cookieName);
    }
}
