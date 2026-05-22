package com.xxx.it.works.wecode.v2.modules.lookup.mapper;

import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * LookUp项 Mapper
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface LookUpItemMapper {

    /**
     * 插入项
     */
    int insert(LookUpItemEntity item);

    /**
     * 批量插入项
     */
    int batchInsert(@Param("items") List<LookUpItemEntity> items);

    /**
     * 根据ID查询项
     */
    LookUpItemEntity selectById(@Param("itemId") Long itemId);

    /**
     * 根据分类ID和项编码查询项（用于唯一性校验）
     */
    LookUpItemEntity selectByClassifyIdAndCode(@Param("classifyId") Long classifyId,
                                               @Param("itemCode") String itemCode);

    /**
     * 查询项列表（带分页和条件）
     */
    List<LookUpItemEntity> selectList(@Param("classifyId") Long classifyId,
                                      @Param("itemCode") String itemCode,
                                      @Param("itemName") String itemName,
                                      @Param("status") Integer status,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    /**
     * 统计项总数（带条件）
     */
    long countList(@Param("classifyId") Long classifyId,
                   @Param("itemCode") String itemCode,
                   @Param("itemName") String itemName,
                   @Param("status") Integer status);

    /**
     * 根据分类ID查询所有项（用于导出）
     */
    List<LookUpItemEntity> selectByClassifyId(@Param("classifyId") Long classifyId);

    /**
     * 根据条件查询所有项（用于导出）
     */
    List<LookUpItemEntity> selectForExport(@Param("classifyId") Long classifyId,
                                           @Param("status") Integer status,
                                           @Param("itemCode") String itemCode,
                                           @Param("itemName") String itemName);

    /**
     * 更新项
     */
    int update(LookUpItemEntity item);

    /**
     * 删除项
     */
    int deleteById(@Param("itemId") Long itemId);

    /**
     * 根据分类ID删除所有项（级联删除用）
     */
    int deleteByClassifyId(@Param("classifyId") Long classifyId);

    /**
     * 检查项编码是否存在
     */
    int checkItemCodeExists(@Param("classifyId") Long classifyId,
                            @Param("itemCode") String itemCode);
}
