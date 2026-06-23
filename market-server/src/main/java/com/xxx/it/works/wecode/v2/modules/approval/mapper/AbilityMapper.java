package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.AbilityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AbilityMapper {

    /**
     * 根据主键 ID 列表批量查询能力
     */
    List<AbilityEntity> selectByIds(@Param("ids") List<Long> ids);
}
