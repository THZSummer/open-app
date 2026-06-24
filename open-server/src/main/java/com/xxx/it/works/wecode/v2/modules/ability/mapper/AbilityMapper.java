package com.xxx.it.works.wecode.v2.modules.ability.mapper;

import com.xxx.it.works.wecode.v2.modules.ability.entity.Ability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 能力主表 Mapper
 */
@Mapper
public interface AbilityMapper {

    List<Ability> selectAll();

    Ability selectById(@Param("id") Long id);
}
