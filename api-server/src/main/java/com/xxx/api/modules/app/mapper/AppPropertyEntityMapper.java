package com.xxx.api.modules.app.mapper;

import com.xxx.api.modules.app.entity.AppPropertyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppPropertyEntityMapper {
    AppPropertyEntity selectByEamapAppCode(@Param("eamapAppCode") String eamapAppCode);
}
