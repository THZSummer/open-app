package com.xxx.it.works.wecode.v2.modules.app.mapper;

import com.xxx.it.works.wecode.v2.modules.app.entity.AppIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用凭证 Mapper
 *
 * <p>对应表 openplatform_app_identity_t</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Mapper
public interface AppIdentityMapper {

    /**
     * 根据 appId（应用主表 id）查询凭证
     */
    AppIdentity selectByAppId(@Param("appId") Long appId);

    /**
     * 插入凭证记录
     */
    int insert(AppIdentity identity);

    /**
     * 根据 appId 删除凭证（物理删除，仅用于数据迁移清理）
     */
    int deleteByAppId(@Param("appId") Long appId);
}
