package com.xxx.it.works.wecode.v2.modules.auditlog.mapper;

import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作记录 Mapper
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface OperateLogMapper {

    /**
     * 插入操作记录
     *
     * @param operateLog 操作记录
     * @return 影响行数
     */
    int insert(OperateLog operateLog);

    /**
     * 分页查询操作记录
     *
     * @param appId 应用ID（可选）
     * @param operateType 操作类型（可选）
     * @param operateObject 操作对象（可选）
     * @param operateUser 操作人（可选，模糊搜索）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 操作记录列表
     */
    List<OperateLog> selectPage(@Param("appId") String appId,
                                @Param("operateType") String operateType,
                                @Param("operateObject") String operateObject,
                                @Param("operateUser") String operateUser,
                                @Param("startTime") String startTime,
                                @Param("endTime") String endTime,
                                @Param("offset") Integer offset,
                                @Param("pageSize") Integer pageSize);

    /**
     * 查询操作记录总数
     */
    Long selectPageCount(@Param("appId") String appId,
                         @Param("operateType") String operateType,
                         @Param("operateObject") String operateObject,
                         @Param("operateUser") String operateUser,
                         @Param("startTime") String startTime,
                         @Param("endTime") String endTime);
}
