package com.xxx.it.works.wecode.v2.modules.app.service;

import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 应用公共服务
 *
 * <p>职责：通知卡片服务应用变更事件、认证方式解析等跨组件公共逻辑</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class AppCommonService {

    /**
     * 通知卡片服务应用变更事件
     *
     * @param appId     应用ID
     * @param eventType 事件类型，如 CREATE / UPDATE / BIND_EAMAP / TRANSFER_OWNER
     */
    public void notifyCardService(String appId, String eventType) {
        // TODO: 对接卡片服务，发送应用变更事件
        log.info("Notify card service: appId={}, eventType={}", appId, eventType);
    }

    /**
     * 从属性列表中解析认证方式，优先返回新字段 verify_type_v2，没值返回旧字段 verify_type。
     *
     * @param props 应用属性列表
     * @return 认证方式 code 列表，都没值返回空列表
     */
    public List<Integer> resolveVerifyTypeList(List<AppProperty> props) {
        if (props == null) {
            return Collections.emptyList();
        }
        String v2 = null;
        String old = null;
        for (AppProperty p : props) {
            if (AppPropertyConstants.PROP_VERIFY_TYPE_V2.equals(p.getPropertyName())) {
                v2 = p.getPropertyValue();
            } else if (AppPropertyConstants.PROP_VERIFY_TYPE.equals(p.getPropertyName())) {
                old = p.getPropertyValue();
            }
        }
        String raw = StringUtils.hasText(v2) ? v2 : old;
        if (raw == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
