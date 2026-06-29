package com.xxx.it.works.wecode.v2.modules.lookup.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Lookup 数据 Mapper
 */
@Mapper
public interface LookupWhitelistMapper {

    /**
     * 根据 classify_code 查询所有启用的 item_code 列表
     */
    List<Map<String, String>> selectItemsByClassifyCode(@Param("classifyCode") String classifyCode);

    /**
     * 根据 classify_code 查询所有启用的 item_value 列表
     */
    List<String> selectItemValuesByClassifyCode(@Param("classifyCode") String classifyCode);

    /**
     * 根据 path 和 classify_code 查询所有启用的 item_code → item_value 映射
     *
     * @param path         路径（命名空间）
     * @param classifyCode 分类编码
     * @return List，每行包含 item_code 和 item_value；无结果时返回空 List
     */
    List<Map<String, String>> selectItemMapByPathAndClassifyCode(
            @Param("path") String path,
            @Param("classifyCode") String classifyCode);
}
