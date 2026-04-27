package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批记录 Mapper 接口
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApprovalRecordMapper {

    /**
     * 插入审批记录
     */
    int insert(ApprovalRecord record);

    /**
     * 根据ID查询审批记录
     */
    ApprovalRecord selectById(@Param("id") Long id);

    /**
     * 根据业务类型和业务ID查询审批记录
     */
    ApprovalRecord selectByBusiness(
            @Param("businessType") String businessType,
            @Param("businessId") Long businessId);

    /**
     * 查询待审批列表
     * 
     * @param type 审批类型
     * @param keyword 搜索关键词
     * @param status 审批状态
     * @param applicantId 申请人ID
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 审批记录列表
     */
    List<ApprovalRecord> selectPendingList(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("applicantId") String applicantId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计待审批数量
     */
    Long countPendingList(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("applicantId") String applicantId);

    /**
     * 更新审批记录
     */
    int update(ApprovalRecord record);

    /**
     * 更新审批状态
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("currentNode") Integer currentNode,
            @Param("completedAt") java.util.Date completedAt,
            @Param("lastUpdateTime") java.util.Date lastUpdateTime,
            @Param("lastUpdateBy") String lastUpdateBy);

    /**
     * 删除审批记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID列表查询审批记录
     */
    List<ApprovalRecord> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据业务类型和业务ID查询最新审批记录
     */
    ApprovalRecord selectLatestByBusiness(
            @Param("businessType") String businessType,
            @Param("businessId") Long businessId);
}
