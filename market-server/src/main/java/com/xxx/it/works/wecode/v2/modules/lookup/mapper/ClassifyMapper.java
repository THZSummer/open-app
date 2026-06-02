package com.xxx.it.works.wecode.v2.modules.lookup.mapper;

import com.xxx.it.works.wecode.v2.modules.lookup.dto.classify.ClassifyQueryDTO;
import com.xxx.it.works.wecode.v2.modules.lookup.entity.ClassifyEntity;
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
public interface ClassifyMapper {

    /**
     * 插入分类
     */
    int insert(ClassifyEntity classify);

    /**
     * 根据ID查询分类
     */
    ClassifyEntity selectById(@Param("classifyId") Long classifyId);

    /**
     * 根据分类编码和路径查询分类（用于唯一性校验）
     */
    ClassifyEntity selectByCodeAndPath(@Param("classifyCode") String classifyCode,
                                       @Param("path") String path);

    /**
     * 查询分类列表（带分页和条件）
     */
    List<ClassifyEntity> selectList(ClassifyQueryDTO queryDTO);

    /**
     * 统计分类总数（带条件）
     */
    long countList(ClassifyQueryDTO queryDTO);

    /**
     * 更新分类
     */
    int update(ClassifyEntity classify);

    /**
     * 删除分类
     */
    int deleteById(@Param("classifyId") Long classifyId);
}