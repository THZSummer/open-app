package com.xxx.it.works.wecode.v2.modules.callback.mapper;

import com.xxx.it.works.wecode.v2.modules.callback.entity.Callback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 回调 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface CallbackMapper {

    /**
     * 插入回调
     */
    int insert(Callback callback);

    /**
     * 根据ID查询回调
     */
    Callback selectById(@Param("id") Long id);

    /**
     * 分页查询回调列表
     * 
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）
     * @param keyword 搜索关键词（可选）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 回调列表
     */
    List<Callback> selectList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计回调总数
     * 
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）
     * @param keyword 搜索关键词（可选）
     * @return 总数
     */
    long countList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    /**
     * 更新回调
     */
    int update(Callback callback);

    /**
     * 删除回调
     */
    int deleteById(@Param("id") Long id);
}
