package com.xxx.it.works.wecode.v2.modules.ability.mapper;

import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 能力属性 Mapper
 *
 * <p>对应表 openplatform_ability_p_t</p>
 */
@Mapper
public interface AbilityPropertyMapper {

    List<AbilityProperty> selectByParentId(Long parentId);

    List<AbilityProperty> selectByParentIds(List<Long> parentIds);
}
