package com.xxx.it.works.wecode.v2.modules.approval.resolver;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务数据解析器工厂
 *
 * <p>自动收集所有 BusinessDataResolver 实现，按 businessType 注册到 Map 中</p>
 */
@Slf4j
@Component
public class BusinessDataResolverFactory {

    @Autowired
    private List<BusinessDataResolver> resolvers;

    private final Map<String, BusinessDataResolver> resolverMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (BusinessDataResolver resolver : resolvers) {
            resolverMap.put(resolver.getBusinessType(), resolver);
            log.info("Registered BusinessDataResolver for businessType: {}", resolver.getBusinessType());
        }
    }

    /**
     * 根据业务类型获取对应的解析器
     *
     * @param businessType 业务类型
     * @return 解析器实例，不存在时返回 null
     */
    public BusinessDataResolver getResolver(String businessType) {
        return resolverMap.get(businessType);
    }
}
