package com.xxx.it.works.wecode.v2.modules.version.mapper;

import com.xxx.it.works.wecode.v2.modules.version.entity.AppVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用版本 Mapper
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Mapper
public interface AppVersionMapper {

    List<AppVersion> selectByAppId(
            @Param("appId") Long appId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countByAppId(@Param("appId") Long appId);

    AppVersion selectById(@Param("id") Long id);

    AppVersion selectByAppIdAndVersionCode(
            @Param("appId") Long appId,
            @Param("versionCode") String versionCode);

    List<AppVersion> selectByAppIdAndStatus(
            @Param("appId") Long appId,
            @Param("status") Integer status);

    int insert(AppVersion version);

    int update(AppVersion version);

    int deleteById(@Param("id") Long id);

    /**
     * 查询应用下最近创建的版本号（排除指定 ID）
     * 按 create_time DESC 取最新创建的版本
     */
    String selectLatestVersionCodeExcludeId(
            @Param("appId") Long appId,
            @Param("excludeId") Long excludeId);

    /**
     * 写入版本属性（K-V）
     */
    int insertProperty(
            @Param("id") Long id,
            @Param("parentId") Long parentId,
            @Param("propertyName") String propertyName,
            @Param("propertyValue") String propertyValue);

    /**
     * 按版本 ID + 属性名查属性值
     */
    String selectPropertyValue(
            @Param("parentId") Long parentId,
            @Param("propertyName") String propertyName);
}
