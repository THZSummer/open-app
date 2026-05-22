package com.xxx.it.works.wecode.v2.modules.lookup.mapper;

import com.xxx.it.works.wecode.v2.modules.lookup.entity.LookUpTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务 Mapper
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface LookUpTaskMapper {

    /**
     * 插入任务
     *
     * @param task 任务实体
     * @return 影响的行数
     */
    int insert(LookUpTaskEntity task);

    /**
     * 根据ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务实体
     */
    LookUpTaskEntity selectById(@Param("taskId") Long taskId);

    /**
     * 更新任务
     *
     * @param task 任务实体
     * @return 影响的行数
     */
    int update(LookUpTaskEntity task);

    /**
     * 查询任务列表（带分页和条件）
     *
     * @param taskType 任务类型
     * @param bizType  业务类型
     * @param status   任务状态
     * @param offset   偏移量
     * @param limit    每页条数
     * @return 任务列表
     */
    List<LookUpTaskEntity> selectList(@Param("taskType") Integer taskType,
                                       @Param("bizType") Integer bizType,
                                       @Param("status") Integer status,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    /**
     * 统计任务总数（带条件）
     *
     * @param taskType 任务类型
     * @param bizType  业务类型
     * @param status   任务状态
     * @return 任务总数
     */
    long countList(@Param("taskType") Integer taskType,
                   @Param("bizType") Integer bizType,
                   @Param("status") Integer status);

    /**
     * 更新任务状态和结果
     *
     * @param taskId  任务ID
     * @param status  任务状态
     * @param result  结果描述
     * @param fileId  文件ID
     * @return 影响的行数
     */
    int updateStatus(@Param("taskId") Long taskId,
                     @Param("status") Integer status,
                     @Param("result") String result,
                     @Param("fileId") Long fileId);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 影响的行数
     */
    int deleteById(@Param("taskId") Long taskId);
}
