package com.xxx.it.works.wecode.v2.modules.approval.resolver;

import java.util.Map;

/**
 * 业务数据解析器接口
 *
 * <p>根据不同业务类型解析并返回业务详情数据</p>
 */
public interface BusinessDataResolver {

    /**
     * 获取业务类型标识
     *
     * @return 业务类型字符串
     */
    String getBusinessType();

    /**
     * 根据业务ID解析业务数据
     *
     * @param businessId 业务ID
     * @return 业务数据键值对
     */
    Map<String, Object> resolveBusinessData(String businessId);

    /**
     * 获取业务类型中文名称
     *
     * @return 业务类型名称
     */
    String getBusinessTypeName();
}
