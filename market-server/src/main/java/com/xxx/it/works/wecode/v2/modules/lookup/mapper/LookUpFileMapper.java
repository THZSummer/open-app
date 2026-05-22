package com.xxx.it.works.wecode.v2.modules.lookup.mapper;

import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件 Mapper
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface LookUpFileMapper {

    /**
     * 插入文件记录
     *
     * @param file 文件实体
     * @return 影响的行数
     */
    int insert(LookUpFileEntity file);

    /**
     * 根据ID查询文件
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    LookUpFileEntity selectById(@Param("fileId") Long fileId);

    /**
     * 查询文件列表
     *
     * @param bizType 业务类型
     * @param offset  偏移量
     * @param limit   每页条数
     * @return 文件列表
     */
    List<LookUpFileEntity> selectList(@Param("bizType") Integer bizType,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 影响的行数
     */
    int deleteById(@Param("fileId") Long fileId);
}