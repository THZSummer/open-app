package com.xxx.open.modules.event.mapper;

import com.xxx.open.modules.event.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface PermissionMapper {

    /**
     * 插入权限
     */
    int insert(Permission permission);

    /**
     * 根据ID查询权限
     */
    Permission selectById(@Param("id") Long id);

    /**
     * 根据资源类型和资源ID查询权限
     */
    Permission selectByResourceTypeAndResourceId(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    /**
     * 根据 Scope 查询权限
     */
    Permission selectByScope(@Param("scope") String scope);

    /**
     * 根据 Scope 统计数量
     */
    int countByScope(@Param("scope") String scope);

    /**
     * 更新权限
     */
    int update(Permission permission);

    /**
     * 删除权限
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据资源类型和资源ID删除权限
     */
    int deleteByResourceTypeAndResourceId(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    /**
     * 统计权限被订阅的数量
     */
    int countSubscriptionsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 根据资源类型和资源ID查询权限（别名方法）
     */
    default Permission selectByResource(String resourceType, Long resourceId) {
        return selectByResourceTypeAndResourceId(resourceType, resourceId);
    }

    // ==================== 分类权限查询（TASK-008 新增）====================

    /**
     * 查询分类下的权限列表（API）
     * 
     * @param categoryId 分类ID
     * @param keyword 关键词
     * @param needApproval 是否需要审核
     * @param categoryPath 分类路径（用于递归查询）
     * @param includeChildren 是否包含子分类
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 权限列表
     */
    List<Permission> selectApiPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计分类下的权限数量（API）
     */
    Long countApiPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren);

    /**
     * 查询分类下的权限列表（事件）
     */
    List<Permission> selectEventPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计分类下的权限数量（事件）
     */
    Long countEventPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren);

    /**
     * 查询分类下的权限列表（回调）
     */
    List<Permission> selectCallbackPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计分类下的权限数量（回调）
     */
    Long countCallbackPermissionsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("needApproval") Integer needApproval,
            @Param("categoryPath") String categoryPath,
            @Param("includeChildren") Boolean includeChildren);

    /**
     * 根据ID列表查询权限
     */
    List<Permission> selectByIds(@Param("ids") List<Long> ids);
}
