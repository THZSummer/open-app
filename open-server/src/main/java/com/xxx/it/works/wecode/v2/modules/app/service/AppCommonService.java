package com.xxx.it.works.wecode.v2.modules.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用公共服务
 *
 * <p>职责：通知卡片服务应用变更事件等跨模块公共逻辑</p>
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
}
