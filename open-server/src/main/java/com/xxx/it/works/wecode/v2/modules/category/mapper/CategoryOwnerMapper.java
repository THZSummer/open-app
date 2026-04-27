package com.xxx.it.works.wecode.v2.modules.category.mapper;

import com.xxx.it.works.wecode.v2.modules.category.entity.CategoryOwner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类责任人 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface CategoryOwnerMapper {

    /**
     * 插入责任人
     */
    int insert(CategoryOwner categoryOwner);

    /**
     * 根据分类ID查询责任人列表
     */
    List<CategoryOwner> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据ID查询责任人
     */
    CategoryOwner selectById(@Param("id") Long id);

    /**
     * 根据分类ID和用户ID查询责任人（检查是否已存在）
     */
    CategoryOwner selectByCategoryIdAndUserId(
            @Param("categoryId") Long categoryId,
            @Param("userId") String userId
    );

    /**
     * 删除责任人
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据分类ID和用户ID删除责任人
     */
    int deleteByCategoryIdAndUserId(
            @Param("categoryId") Long categoryId,
            @Param("userId") String userId
    );

    /**
     * 统计分类的责任人数量
     */
    int countByCategoryId(@Param("categoryId") Long categoryId);
}
