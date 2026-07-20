package com.xxx.it.works.wecode.v2.modules.ability.mapper;

import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AbilityMapper {

    /**
     * 根据主键 ID 列表批量查询能力
     */
    List<AbilityEntity> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 分页查询能力列表（支持关键字模糊搜索和动态排序）
     *
     * @param keyword  搜索关键字（按中文名/英文名模糊匹配）
     * @param sortField 排序字段（已通过白名单校验）
     * @param sortOrder 排序方向（asc/desc）
     * @param offset   偏移量
     * @param pageSize 每页条数
     * @return 能力实体列表
     */
    List<AbilityEntity> selectPage(@Param("keyword") String keyword,
                                   @Param("sortField") String sortField,
                                   @Param("sortOrder") String sortOrder,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /**
     * 统计能力总数（支持关键字模糊搜索）
     *
     * @param keyword 搜索关键字
     * @return 总记录数
     */
    long countByKeyword(@Param("keyword") String keyword);

    /**
     * 插入能力记录（主表）
     *
     * <p>使用 useGeneratedKeys 自动回填自增主键 ID。</p>
     *
     * @param entity 能力实体
     * @return 影响行数
     */
    int insert(AbilityEntity entity);

    /**
     * 根据能力类型编码查询
     *
     * @param abilityType 能力类型编码
     * @return 能力实体，不存在返回 null
     */
    AbilityEntity selectByAbilityType(@Param("abilityType") Integer abilityType);

    /**
     * 查询当前最大排序号
     *
     * @return 最大 orderNum，无记录时返回 null
     */
    Integer selectMaxOrderNum();
}
