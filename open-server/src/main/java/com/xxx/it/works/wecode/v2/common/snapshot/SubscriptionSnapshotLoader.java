package com.xxx.it.works.wecode.v2.common.snapshot;

import com.xxx.it.works.wecode.v2.modules.permission.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订阅表快照加载器
 *
 * <p>支持 API_PERMISSION / EVENT_PERMISSION / CALLBACK_PERMISSION 三种操作对象，
 * 统一从 openplatform_subscription_t 表加载实体快照</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SubscriptionSnapshotLoader implements EntitySnapshotLoader {

    private final SubscriptionMapper subscriptionMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("API_PERMISSION", "EVENT_PERMISSION", "CALLBACK_PERMISSION");
    }

    @Override
    public Object loadById(Long id) {
        return subscriptionMapper.selectById(id);
    }
}
