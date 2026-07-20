package com.xxx.it.works.wecode.v2.modules.ability.mapper;

import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 能力属性 Mapper（market-server 版）
 *
 * <p>对应表 openplatform_ability_p_t，提供 admin 管理面所需的属性查询。
 * 用于获取能力的图标 URL 和示意图 URL 等扩展属性。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface AbilityPropertyMapper {

    /**
     * 根据父记录 ID 列表批量查询属性
     *
     * @param parentIds 能力 ID 列表
     * @return 属性列表
     */
    List<AbilityProperty> selectByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 插入属性记录（属性表）
     *
     * <p>使用 useGeneratedKeys 自动回填自增主键 ID。</p>
     *
     * @param property 属性实体
     * @return 影响行数
     */
    int insert(AbilityProperty property);
}
