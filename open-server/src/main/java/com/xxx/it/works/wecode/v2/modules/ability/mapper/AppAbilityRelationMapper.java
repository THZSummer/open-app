package com.xxx.it.works.wecode.v2.modules.ability.mapper;

import com.xxx.it.works.wecode.v2.modules.ability.entity.AppAbilityRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用能力关联 Mapper
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Mapper
public interface AppAbilityRelationMapper {

    List<AppAbilityRelation> selectByAppId(@Param("appId") Long appId);

    AppAbilityRelation selectByAppIdAndAbilityType(
            @Param("appId") Long appId,
            @Param("abilityType") Integer abilityType);

    AppAbilityRelation selectById(@Param("id") Long id);

    int insert(AppAbilityRelation relation);
}
