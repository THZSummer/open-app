package com.xxx.api.approval.mapper;

import com.xxx.api.approval.entity.ApprovalLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批日志 Mapper 接口（api-server）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApprovalLogMapper {

    /**
     * 插入审批日志
     *
     * @param log 审批日志
     * @return 影响行数
     */
    int insert(ApprovalLog log);
}
