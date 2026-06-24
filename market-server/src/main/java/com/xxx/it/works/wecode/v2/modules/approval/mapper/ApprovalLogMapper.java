package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApprovalLogMapper {

    int insert(ApprovalLog log);
}
