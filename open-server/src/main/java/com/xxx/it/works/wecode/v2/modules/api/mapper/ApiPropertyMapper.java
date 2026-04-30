package com.xxx.it.works.wecode.v2.modules.api.mapper;

import com.xxx.it.works.wecode.v2.modules.api.entity.ApiProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API 属性 Mapper 接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApiPropertyMapper {

    /**
     * 批量插入属性
     */
    int batchInsert(@Param("list") List<ApiProperty> list);

    /**
     * 根据 parentId 查询属性列表
     */
    List<ApiProperty> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据 parentId 删除属性
     */
    int deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 批量查询多个 parentId 的属性列表
     */
    List<ApiProperty> selectByParentIds(@Param("list") List<Long> parentIds);
}
