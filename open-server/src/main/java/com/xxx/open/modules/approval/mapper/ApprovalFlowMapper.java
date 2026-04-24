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
     * 
     * v2.8.0变更：此方法替代原有的 selectDefaultFlow()
     * - 查询全局审批流程：selectByCode("global")
     * - 查询场景审批流程：selectByCode("api_permission_apply") 等
     */
    ApprovalFlow selectByCode(@Param("code") String code);

    // ✅ v2.8.0 变更：移除 selectDefaultFlow() 方法
    // 原因：移除 isDefault 字段，改用 code='global' 标识全局审批
    // 查询全局审批流程：ApprovalFlow globalFlow = flowMapper.selectByCode("global");

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
