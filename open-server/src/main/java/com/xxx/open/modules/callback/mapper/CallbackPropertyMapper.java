package com.xxx.open.modules.callback.mapper;

import com.xxx.open.modules.callback.entity.CallbackProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 回调属性 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface CallbackPropertyMapper {

    /**
     * 插入回调属性
     */
    int insert(CallbackProperty property);

    /**
     * 批量插入回调属性
     */
    int batchInsert(@Param("list") List<CallbackProperty> properties);

    /**
     * 根据父ID查询属性列表
     */
    List<CallbackProperty> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 删除指定父ID的所有属性
     */
    int deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 根据父ID和属性名查询属性
     */
    CallbackProperty selectByParentIdAndName(
            @Param("parentId") Long parentId,
            @Param("propertyName") String propertyName
    );

    /**
     * 批量查询多个 parentId 的属性列表
     */
    List<CallbackProperty> selectByParentIds(@Param("list") List<Long> parentIds);
}
