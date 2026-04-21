package com.xxx.api.common.mapper;

import com.xxx.api.common.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 权限 Mapper 接口（api-server）
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface PermissionMapper {

    /**
     * 根据 Scope 查询权限
     */
    Permission selectByScope(@Param("scope") String scope);

    /**
     * 根据ID查询权限
     */
    Permission selectById(@Param("id") Long id);
}
