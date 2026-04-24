package com.xxx.open.modules.event.mapper;

import com.xxx.open.modules.event.entity.EventProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 事件属性 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface EventPropertyMapper {

    /**
     * 插入事件属性
     */
    int insert(EventProperty property);

    /**
     * 批量插入事件属性
     */
    int batchInsert(@Param("list") List<EventProperty> properties);

    /**
     * 根据父ID查询属性列表
     */
    List<EventProperty> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 删除指定父ID的所有属性
     */
    int deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 更新属性
     */
    int update(EventProperty property);

    /**
     * 批量查询多个 parentId 的属性列表
     */
    List<EventProperty> selectByParentIds(@Param("list") List<Long> parentIds);
}
