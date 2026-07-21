package com.xxx.api.internal.mapper;

import com.xxx.api.internal.entity.AppPropertyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppPropertyEntityMapper {
    AppPropertyEntity selectByEamapAppCode(@Param("eamapAppCode") String eamapAppCode);
}
