package com.xxx.it.works.wecode.v2.modules.dictionary.mapper;

import com.xxx.it.works.wecode.v2.modules.dictionary.dto.DictionaryQueryDTO;
import com.xxx.it.works.wecode.v2.modules.dictionary.entity.DictionaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据字典 Mapper
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface DictionaryMapper {

    /**
     * 插入数据字典
     */
    int insert(DictionaryEntity entity);

    /**
     * 根据ID查询数据字典
     */
    DictionaryEntity selectById(@Param("id") Long id);

    /**
     * 根据路径和编码查询数据字典（用于唯一性校验）
     */
    DictionaryEntity selectByPathAndCode(@Param("path") String path,
                                          @Param("code") String code);

    /**
     * 查询数据字典列表（带分页和条件）
     */
    List<DictionaryEntity> selectList(DictionaryQueryDTO queryDTO);

    /**
     * 统计数据字典总数（带条件）
     */
    long countList(DictionaryQueryDTO queryDTO);

    /**
     * 更新数据字典
     */
    int update(DictionaryEntity entity);

    /**
     * 删除数据字典
     */
    int deleteById(@Param("id") Long id);

}