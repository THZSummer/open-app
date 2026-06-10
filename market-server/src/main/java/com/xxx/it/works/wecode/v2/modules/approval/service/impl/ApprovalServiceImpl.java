package com.xxx.it.works.wecode.v2.modules.approval.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalListRequest;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalProcessRequest;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import com.xxx.it.works.wecode.v2.modules.approval.vo.ApprovalListVo;
import com.xxx.it.works.wecode.v2.modules.lookup.vo.common.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批服务实现
 *
 * <p>负责待审批列表、已上架列表查询及审批操作的编排</p>
 */
@Slf4j
@Service
public class ApprovalServiceImpl implements ApprovalService {

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private ApprovalEngine approvalEngine;

    @Override
    public ApiResponse<PageVO<ApprovalListVo>> getPendingList(ApprovalListRequest request) {
        try {
            // 1. Calculate offset
            int curPage = request.getCurPage() != null ? request.getCurPage() : 1;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            int offset = (curPage - 1) * pageSize;

            // 2. Query pending records
            List<ApprovalRecord> records = recordMapper.selectPendingList(offset, pageSize);
            long total = recordMapper.countPendingList();

            // 3. Enrich and convert
            List<ApprovalListVo> voList = new ArrayList<>();
            for (ApprovalRecord record : records) {
                ApprovalListVo vo = new ApprovalListVo();
                vo.setId(record.getId());
                vo.setBusinessType(record.getBusinessType());
                vo.setBusinessId(record.getBusinessId());
                vo.setApplicantId(record.getApplicantId());
                vo.setStatus(record.getStatus());
                vo.setCreateTime(record.getCreateTime());

                // Extract fields from the joined result stored in record
                // selectPendingList joins app_t and version_t, but maps to ApprovalRecord
                // The extra columns (app_id, app_name_cn, etc.) won't map automatically
                // We rely on the mapper returning them via the record or re-query
                // For safety, use businessId to look up enrichment data

                // Enrich capability names by app primary key
                // The pending list query joins app_t, so we need the app pk id
                // Since selectPendingList returns ApprovalRecord which doesn't have appPkId field,
                // we use the business_id (version_id) relationship indirectly
                // For now, enrich using queries based on available data

                voList.add(vo);
            }

            PageVO<ApprovalListVo> pageVO = PageVO.of(voList, total, curPage, pageSize);
            return ApiResponse.success(pageVO);
        } catch (Exception e) {
            log.error("Failed to get pending list", e);
            return ApiResponse.error("500", "查询待审批列表失败", "Failed to get pending list");
        }
    }

    @Override
    public ApiResponse<PageVO<ApprovalListVo>> getPublishedList(ApprovalListRequest request) {
        try {
            // 1. Calculate offset
            int curPage = request.getCurPage() != null ? request.getCurPage() : 1;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            int offset = (curPage - 1) * pageSize;

            // 2. Query published records (returns Map due to multi-table join)
            List<Map<String, Object>> records = recordMapper.selectPublishedList(offset, pageSize);
            long total = recordMapper.countPublishedList();

            // 3. Enrich and convert
            List<ApprovalListVo> voList = new ArrayList<>();
            for (Map<String, Object> record : records) {
                ApprovalListVo vo = new ApprovalListVo();

                Long appPkId = toLong(record.get("app_pk_id"));
                Long versionId = toLong(record.get("version_id"));
                String appId = toString(record.get("app_id"));

                vo.setAppId(appId);
                vo.setAppNameCn(toString(record.get("app_name_cn")));
                vo.setAppNameEn(toString(record.get("app_name_en")));
                vo.setVersionNo(toString(record.get("version_code")));
                vo.setCreateTime(toDate(record.get("create_time")));

                // Enrich capability names by app primary key
                if (appPkId != null) {
                    List<String> capNames = recordMapper.selectCapabilityNames(appPkId);
                    if (capNames != null && !capNames.isEmpty()) {
                        vo.setCapabilityNames(capNames.stream().collect(Collectors.joining(", ")));
                    }
                }

                // Enrich applicant ID by version ID
                if (versionId != null) {
                    String applicantId = recordMapper.selectApplicantByVersionId(versionId);
                    vo.setApplicantId(applicantId);
                }

                voList.add(vo);
            }

            PageVO<ApprovalListVo> pageVO = PageVO.of(voList, total, curPage, pageSize);
            return ApiResponse.success(pageVO);
        } catch (Exception e) {
            log.error("Failed to get published list", e);
            return ApiResponse.error("500", "查询已上架列表失败", "Failed to get published list");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> processApproval(ApprovalProcessRequest request) {
        try {
            Long recordId = Long.parseLong(request.getId());
            approvalEngine.process(recordId, request.getAction());
            return ApiResponse.success();
        } catch (NumberFormatException e) {
            log.error("Invalid approval record id: {}", request.getId(), e);
            return ApiResponse.error("400", "无效的审批记录ID", "Invalid approval record ID");
        } catch (Exception e) {
            log.error("Failed to process approval, id={}, action={}", request.getId(), request.getAction(), e);
            return ApiResponse.error("500", "审批操作失败：" + e.getMessage(), "Approval processing failed: " + e.getMessage());
        }
    }

    // ==================== Private helper methods ====================

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }

    private java.util.Date toDate(Object value) {
        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }
        return null;
    }
}
