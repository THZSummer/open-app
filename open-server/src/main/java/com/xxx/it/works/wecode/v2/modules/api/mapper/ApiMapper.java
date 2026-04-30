package com.xxx.it.works.wecode.v2.modules.api.mapper;

import com.xxx.it.works.wecode.v2.modules.api.entity.Api;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API Mapper 接口
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApiMapper {

    /**
     * 插入 API
     */
    int insert(Api api);

    /**
     * 根据 ID 查询
     */
    Api selectById(@Param("id") Long id);

    /**
     * 更新 API
     */
    int update(Api api);

    /**
     * 删除 API
     */
    int deleteById(@Param("id") Long id);

    /**
     * 分页查询 API 列表
     */
    List<Api> selectList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计 API 总数
     */
    Long countList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    /**
     * 查询分类下的 API 数量
     */
    Long countByCategoryId(@Param("categoryId") Long categoryId);
}
