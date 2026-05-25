package com.xxx.it.works.wecode.v2.modules.auditlog.mapper;

import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import org.apache.ibatis.annotations.Mapper;

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
}
