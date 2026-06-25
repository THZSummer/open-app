package com.xxx.it.works.wecode.v2.modules.app.mapper;

import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用属性 Mapper 接口
 *
 * <p>对应表 {@code openplatform_app_p_t}。</p>
 *
 * <p>当前唯一使用场景：卡片设置板块通过 {@code selectByAppIdAndPropertyName}
 * 查询 {@code eamap_app_code} 属性，作为调用卡片服务时的 {@code clientId}。</p>
 *
 * @author Spec Agent
 * @version 1.0.0
 */
@Mapper
public interface AppPropertyMapper {

    /**
     * 按应用 ID + 属性名查询单个属性
     *
     * @param appId        应用主键 ID（openplatform_app_t.id）
     * @param propertyName 属性名（如 eamap_app_code）
     * @return 第一条匹配的属性记录，未找到返回 null
     */
    AppProperty selectByAppIdAndPropertyName(@Param("appId") Long appId,
                                              @Param("propertyName") String propertyName);
}
