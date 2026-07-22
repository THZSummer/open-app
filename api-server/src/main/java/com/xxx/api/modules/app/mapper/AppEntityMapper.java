package com.xxx.api.modules.app.mapper;

import com.xxx.api.modules.app.entity.AppEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppEntityMapper {
    AppEntity selectByAppId(@Param("appId") String appId);
    AppEntity selectById(@Param("id") Long id);
}
