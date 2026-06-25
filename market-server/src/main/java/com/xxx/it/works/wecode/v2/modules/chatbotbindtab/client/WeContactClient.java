package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.client;

import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto.WeContactRequest;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto.WeContactResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * 通讯录 API 客户端
 *
 * <p>用于校验机器人账号有效性（userType ∈ {4,5,10}）</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Slf4j
@Component
public class WeContactClient {

    private static final String USER_TYPE_ROBOT = "4";
    private static final String USER_TYPE_BUSINESS_ASSISTANT = "5";
    private static final String USER_TYPE_PERSONAL_ASSISTANT = "10";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wecontact.api-url:}")
    private String apiUrl;

    @Value("${wecontact.tenant-id:}")
    private String tenantId;

    /**
     * 校验账号有效性
     *
     * <p>调用通讯录 API，校验 userType ∈ {4, 5, 10}</p>
     *
     * @param accountId 机器人账号 ID
     * @param token     Authorization token（由人工通过工具类获取）
     * @return true=有效（userType 合法），false=无效
     * @throws RestClientException 网络异常或 API 调用失败
     */
    public boolean validateAccount(String accountId, String token) throws RestClientException {
        String url = apiUrl + "/wecontact-relation/v1/relation/personPublicInfo?source=welink_sysi_open";

        // 构造请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-welink-tenantid", tenantId);
        headers.set("Authorization", token);

        // 构造请求体
        WeContactRequest request = new WeContactRequest(Collections.singletonList(accountId));
        HttpEntity<WeContactRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling WeContact API to validate account: {}", accountId);

        ResponseEntity<WeContactResponse> response = restTemplate.postForEntity(
                url, entity, WeContactResponse.class);

        WeContactResponse body = response.getBody();
        if (body == null || !"0".equals(body.getCode())) {
            log.warn("WeContact API returned error: code={}, message={}",
                    body != null ? body.getCode() : "null",
                    body != null ? body.getMessage() : "null");
            return false;
        }

        if (body.getUsers() == null || body.getUsers().isEmpty()) {
            log.info("WeContact API returned empty users for account: {}", accountId);
            return false;
        }

        Integer userType = body.getUsers().get(0).getUserType();
        boolean valid = userType != null &&
                (USER_TYPE_ROBOT.equals(String.valueOf(userType)) ||
                 USER_TYPE_BUSINESS_ASSISTANT.equals(String.valueOf(userType)) ||
                 USER_TYPE_PERSONAL_ASSISTANT.equals(String.valueOf(userType)));

        log.info("WeContact validation result: account={}, userType={}, valid={}",
                accountId, userType, valid);

        return valid;
    }
}
