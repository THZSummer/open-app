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

    /**
     * 根据能力类型查询启用状态的能力（不限制 hidden 字段）
     *
     * <p>用于订阅校验，替代 AbilityTypeEnum.isValidCode() 硬编码枚举校验。
     * 订阅不关心 hidden 状态（EC-001：已订阅不受隐藏影响），仅校验 status=1 即可。</p>
     *
     * @param abilityType 能力类型
     * @return 匹配的能力实体，未找到或已禁用则返回 null
     */
    Ability selectByAbilityType(@Param("abilityType") Integer abilityType);
}
