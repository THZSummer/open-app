package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.AppEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppMapper {

    /**
     * 根据主键 ID 查询应用
     */
    AppEntity selectById(@Param("id") Long id);
}
