package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApprovalFlowMapper {

    ApprovalFlow selectByCode(@Param("code") String code);
}
