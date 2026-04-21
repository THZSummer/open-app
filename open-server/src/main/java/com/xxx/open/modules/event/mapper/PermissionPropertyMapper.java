package com.xxx.open.modules.event.mapper;

import com.xxx.open.modules.event.entity.PermissionProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限属性 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface PermissionPropertyMapper {

    /**
     * 插入权限属性
     */
    int insert(PermissionProperty property);

    /**
     * 批量插入权限属性
     */
    int batchInsert(@Param("list") List<PermissionProperty> properties);

    /**
     * 根据父ID查询属性列表
     */
    List<PermissionProperty> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 删除指定父ID的所有属性
     */
    int deleteByParentId(@Param("parentId") Long parentId);
}
