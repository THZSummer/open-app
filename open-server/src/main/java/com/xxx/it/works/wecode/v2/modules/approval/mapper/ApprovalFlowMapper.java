package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalFlow;
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
            @Param("appId") Long appId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计审批流程数量
     */
    Long countList(@Param("keyword") String keyword, @Param("appId") Long appId);

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

    /**
     * 检查 code+appId 组合是否存在（V3 修复：唯一性校验带 appId）
     */
    int countByCodeAndAppId(
            @Param("code") String code,
            @Param("appId") Long appId);

    /**
     * 检查 code+appId 组合是否存在（排除指定ID，用于更新校验）
     */
    int countByCodeAndAppIdExcludeId(
            @Param("code") String code,
            @Param("appId") Long appId,
            @Param("excludeId") Long excludeId);

    /**
     * 根据编码和应用ID查询审批流程（V3 新增）
     *
     * <p>用于连接器流版本发布审批的三级审批人查找：
     * 1. 应用级：code=? AND app_id=?
     * 2. 平台级：code=? AND app_id IS NULL
     * 3. 全局级：code='global' AND app_id IS NULL</p>
     *
     * @param code  流程编码
     * @param appId 应用ID（可为null）
     * @return 审批流程模板
     */
    ApprovalFlow selectByCodeAndAppId(
            @Param("code") String code,
            @Param("appId") Long appId);
}
