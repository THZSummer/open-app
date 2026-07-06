package com.xxx.it.works.wecode.v2.modules.approvalflow.service;

import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalNodeDto;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approvalflow.dto.*;
import com.xxx.it.works.wecode.v2.modules.approvalflow.entity.ApprovalFlow;
import com.xxx.it.works.wecode.v2.modules.approvalflow.mapper.ApprovalFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批流程模板管理 Service
 *
 * <p>负责审批流程模板的 CRUD 操作。</p>
 *
 * <p>核心设计（V3）：</p>
 * <ul>
 *   <li>appId 使用外部业务标识（App.appId），内部存储为 App.id</li>
 *   <li>列表/详情查询通过 LEFT JOIN 返回 externalAppId</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 2.8.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowService {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalEngine approvalEngine;
    private final IdGeneratorStrategy idGenerator;
    private final AppMapper appMapper;

    /** 将外部业务 appId（String）解析为内部主键 App.id（Long），用于入库 */
    private Long resolveInternalAppId(String externalAppId) {
        if (externalAppId == null || externalAppId.trim().isEmpty()) {
            return null;
        }
        App app = appMapper.selectByAppId(externalAppId.trim());
        if (app == null) {
            throw new BusinessException("404", "应用不存在", "App not found: " + externalAppId);
        }
        return app.getId();
    }

    /**
     * 查询审批流程列表
     */
    public List<ApprovalFlowListResponse> getFlowList(ApprovalFlowListRequest request) {
        int offset = (request.getCurPage() - 1) * request.getPageSize();
        List<ApprovalFlow> flows = flowMapper.selectList(request.getKeyword(), request.getAppId(), offset, request.getPageSize());

        return flows.stream().map(flow -> {
            ApprovalFlowListResponse response = new ApprovalFlowListResponse();
            response.setId(String.valueOf(flow.getId()));
            response.setNameCn(flow.getNameCn());
            response.setNameEn(flow.getNameEn());
            response.setCode(flow.getCode());
            response.setAppId(flow.getExternalAppId());

            response.setStatus(flow.getStatus());
            response.setNodes(approvalEngine.parseNodes(flow.getNodes()));
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 统计审批流程数量
     */
    public Long countFlowList(String keyword, String appId) {
        return flowMapper.countList(keyword, appId);
    }

    /**
     * 获取审批流程详情
     */
    public ApprovalFlowDetailResponse getFlowDetail(Long id) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        ApprovalFlowDetailResponse response = new ApprovalFlowDetailResponse();
        response.setId(String.valueOf(flow.getId()));
        response.setNameCn(flow.getNameCn());
        response.setNameEn(flow.getNameEn());
        response.setCode(flow.getCode());
        response.setAppId(flow.getExternalAppId());

        response.setStatus(flow.getStatus());
        response.setNodes(approvalEngine.parseNodes(flow.getNodes()));

        return response;
    }

    /**
     * 创建审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse createFlow(ApprovalFlowCreateRequest request, String operator) {

        Long internalAppId = resolveInternalAppId(request.getAppId());

        if (flowMapper.countByCodeAndAppId(request.getCode(), internalAppId) > 0) {
            throw new BusinessException("409", "流程编码已存在", "Flow code already exists for this app");
        }

        ApprovalFlow flow = new ApprovalFlow();
        flow.setId(idGenerator.nextId());
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setCode(request.getCode());
        flow.setAppId(internalAppId);
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setStatus(1);
        flow.setCreateTime(new Date());
        flow.setLastUpdateTime(new Date());
        flow.setCreateBy(operator);
        flow.setLastUpdateBy(operator);

        flowMapper.insert(flow);

        log.info("Create approval flow: id={}, code={}, appId={}, operator={}", flow.getId(), flow.getCode(), flow.getAppId(), operator);

        return getFlowDetail(flow.getId());
    }

    /**
     * 更新审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowDetailResponse updateFlow(Long id, ApprovalFlowUpdateRequest request, String operator) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        Long internalAppId = resolveInternalAppId(request.getAppId());

        String effectiveCode = request.getCode() != null ? request.getCode() : flow.getCode();
        Long effectiveAppId = internalAppId != null ? internalAppId : flow.getAppId();
        if (flowMapper.countByCodeAndAppIdExcludeId(effectiveCode, effectiveAppId, id) > 0) {
            throw new BusinessException("409", "流程编码已存在", "Flow code already exists for this app");
        }

        if (request.getCode() != null) {
            flow.setCode(request.getCode());
        }
        flow.setNameCn(request.getNameCn());
        flow.setNameEn(request.getNameEn());
        flow.setAppId(internalAppId);
        flow.setNodes(approvalEngine.serializeNodes(request.getNodes()));
        flow.setLastUpdateTime(new Date());
        flow.setLastUpdateBy(operator);

        flowMapper.update(flow);

        log.info("Update approval flow: id={}, operator={}", id, operator);

        return getFlowDetail(id);
    }

    /**
     * 删除审批流程
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFlow(Long id, String operator) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BusinessException("404", "审批流程不存在", "Approval flow not found");
        }

        flowMapper.deleteById(id);

        log.info("Delete approval flow: id={}, code={}, operator={}", id, flow.getCode(), operator);
    }
}
