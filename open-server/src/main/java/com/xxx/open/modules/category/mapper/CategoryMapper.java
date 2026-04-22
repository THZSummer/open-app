package com.xxx.open.modules.category.mapper;

import com.xxx.open.modules.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface CategoryMapper {

    /**
     * 插入分类
     */
    int insert(Category category);

    /**
     * 根据ID查询分类
     */
    Category selectById(@Param("id") Long id);

    /**
     * 查询所有分类
     */
    List<Category> selectAll();

    /**
     * 根据分类别名查询（用于权限树查询）
     */
    List<Category> selectByCategoryAlias(@Param("categoryAlias") String categoryAlias);

    /**
     * 根据父ID查询子分类
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据路径前缀查询（用于子树查询优化）
     * 例如：path LIKE '/1/2/%' 查询 /1/2/ 下的所有子分类
     */
    List<Category> selectByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 更新分类
     */
    int update(Category category);

    /**
     * 删除分类
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计分类下的 API 数量
     */
    int countApisByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 统计分类下的事件数量
     */
    int countEventsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 统计分类下的回调数量
     */
    int countCallbacksByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 统计分类下的权限数量
     */
    int countPermissionsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 统计子分类数量
     */
    int countChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 根据路径查询分类列表（用于构建 categoryPath）
     * 例如：path = '/1/2/3/'，返回 ID 为 1, 2, 3 的分类
     */
    List<Category> selectByPath(@Param("path") String path);
}
