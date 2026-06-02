package com.xxx.api.approval.mapper;

import com.xxx.api.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 审批记录 Mapper 接口（api-server）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApprovalRecordMapper {

    /**
     * 根据业务类型和业务ID查询最新待审批状态的记录（回调专用）
     *
     * <p>按创建时间倒序取第一条 status=0（待审）的记录。
     * WHERE 条件包含 business_type，命中 idx_business(business_type, business_id) 联合索引</p>
     *
     * @param businessType 业务类型（如 api_permission_apply）
     * @param businessId   业务ID
     * @return 审批记录，不存在或非待审返回 null
     */
    ApprovalRecord selectLatestPendingByBusiness(@Param("businessType") String businessType,
                                                  @Param("businessId") Long businessId);

    /**
     * 更新审批记录
     *
     * @param record 审批记录（更新 status / currentNode / combinedNodes / completedAt）
     * @return 影响行数
     */
    int update(ApprovalRecord record);
}
