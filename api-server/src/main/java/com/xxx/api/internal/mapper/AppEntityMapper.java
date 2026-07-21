package com.xxx.api.internal.mapper;

import com.xxx.api.internal.entity.AppEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppEntityMapper {
    AppEntity selectByAppId(@Param("appId") String appId);
    AppEntity selectById(@Param("id") Long id);
}
