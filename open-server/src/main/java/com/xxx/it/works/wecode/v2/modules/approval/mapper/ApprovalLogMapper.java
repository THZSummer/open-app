package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批操作日志 Mapper 接口
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApprovalLogMapper {

    /**
     * 插入审批日志
     */
    int insert(ApprovalLog log);

    /**
     * 根据审批记录ID查询日志列表
     */
    List<ApprovalLog> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据ID查询日志
     */
    ApprovalLog selectById(@Param("id") Long id);

    /**
     * 删除审批日志
     */
    int deleteByRecordId(@Param("recordId") Long recordId);

    /**
     * 批量插入日志
     */
    int batchInsert(@Param("list") List<ApprovalLog> list);
}
