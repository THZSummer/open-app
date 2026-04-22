package com.xxx.open.modules.approval.mapper;

import com.xxx.open.modules.approval.entity.ApprovalFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批流程模板 Mapper 接口
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface ApprovalFlowMapper {

    /**
     * 插入审批流程
     */
    int insert(ApprovalFlow flow);

    /**
     * 根据ID查询审批流程
     */
    ApprovalFlow selectById(@Param("id") Long id);

    /**
     * 根据编码查询审批流程
     */
    ApprovalFlow selectByCode(@Param("code") String code);

    /**
     * 查询默认审批流程
     */
    ApprovalFlow selectDefaultFlow();

    /**
     * 查询审批流程列表
     * 
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 流程列表
     */
    List<ApprovalFlow> selectList(
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计审批流程数量
     */
    Long countList(@Param("keyword") String keyword);

    /**
     * 更新审批流程
     */
    int update(ApprovalFlow flow);

    /**
     * 删除审批流程
     */
    int deleteById(@Param("id") Long id);

    /**
     * 检查编码是否存在
     */
    int countByCode(@Param("code") String code);

    /**
     * 检查编码是否存在（排除指定ID）
     */
    int countByCodeExcludeId(
            @Param("code") String code,
            @Param("excludeId") Long excludeId);
}
