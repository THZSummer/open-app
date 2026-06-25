package com.xxx.it.works.wecode.v2.modules.approval.service.impl;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalListRequest;
import com.xxx.it.works.wecode.v2.modules.approval.dto.ApprovalProcessRequest;
import com.xxx.it.works.wecode.v2.modules.approval.engine.ApprovalEngine;
import com.xxx.it.works.wecode.v2.modules.approval.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.approval.entity.AppEntity;
import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import com.xxx.it.works.wecode.v2.modules.approval.entity.AppVersionEntity;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.AbilityMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.ApprovalRecordMapper;
import com.xxx.it.works.wecode.v2.modules.approval.mapper.AppVersionMapper;
import com.xxx.it.works.wecode.v2.modules.approval.service.ApprovalService;
import com.xxx.it.works.wecode.v2.modules.approval.vo.ApprovalListVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApprovalServiceImpl implements ApprovalService {

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private AppVersionMapper appVersionMapper;

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private AbilityMapper abilityMapper;

    @Autowired
    private ApprovalEngine approvalEngine;

    @Override
    public ApiResponse<List<ApprovalListVo>> getPendingList(ApprovalListRequest request) {
        try {
            int curPage = request.getCurPage() != null ? request.getCurPage() : 1;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            int offset = (curPage - 1) * pageSize;

            List<ApprovalRecord> records = recordMapper.selectPendingList(offset, pageSize);
            long total = recordMapper.countPendingList();

            List<ApprovalListVo> voList = new ArrayList<>();
            for (ApprovalRecord record : records) {
                ApprovalListVo vo = new ApprovalListVo();
                vo.setId(String.valueOf(record.getId()));
                vo.setBusinessType(record.getBusinessType());
                vo.setBusinessId(record.getBusinessId());
                vo.setApplicantId(record.getApplicantId());
                vo.setStatus(record.getStatus());
                vo.setCreateTime(record.getCreateTime());

                Long versionId = Long.parseLong(record.getBusinessId());
                AppVersionEntity version = appVersionMapper.selectById(versionId);
                if (version != null) {
                    vo.setVersionNo(version.getVersionCode());

                    AppEntity app = appMapper.selectById(version.getAppId());
                    if (app != null) {
                        vo.setAppNameCn(app.getAppNameCn());
                        vo.setAppNameEn(app.getAppNameEn());
                        vo.setAppId(app.getAppId());
                    }

                    String abilityIdsStr = recordMapper.selectVersionAbilityIds(versionId);
                    if (abilityIdsStr != null && !abilityIdsStr.isEmpty()) {
                        List<Long> abilityIds = parseIds(abilityIdsStr);
                        if (!abilityIds.isEmpty()) {
                            List<AbilityEntity> abilities = abilityMapper.selectByIds(abilityIds);
                            vo.setCapabilityNames(abilities.stream()
                                    .map(AbilityEntity::getAbilityNameCn)
                                    .collect(Collectors.joining(", ")));
                        }
                    }
                }

                voList.add(vo);
            }

            int totalPages = (int) ((total + pageSize - 1) / pageSize);
            ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                    .curPage(curPage)
                    .pageSize(pageSize)
                    .total(total)
                    .totalPages(totalPages)
                    .build();
            return ApiResponse.success(voList, page);
        } catch (Exception e) {
            log.error("Failed to get pending list", e);
            return ApiResponse.error("500", "查询待审批列表失败", "Failed to get pending list");
        }
    }

    @Override
    public ApiResponse<List<ApprovalListVo>> getPublishedList(ApprovalListRequest request) {
        try {
            int curPage = request.getCurPage() != null ? request.getCurPage() : 1;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            int offset = (curPage - 1) * pageSize;

            List<Map<String, Object>> records = recordMapper.selectPublishedList(offset, pageSize);
            long total = recordMapper.countPublishedList();

            List<ApprovalListVo> voList = new ArrayList<>();
            for (Map<String, Object> record : records) {
                ApprovalListVo vo = new ApprovalListVo();

                Long appPkId = toLong(record.get("app_pk_id"));
                Long versionId = toLong(record.get("version_id"));

                vo.setAppId(toString(record.get("app_id")));
                vo.setAppNameCn(toString(record.get("app_name_cn")));
                vo.setAppNameEn(toString(record.get("app_name_en")));
                vo.setVersionNo(toString(record.get("version_code")));
                vo.setCreateTime(toDate(record.get("create_time")));

                if (versionId != null) {
                    String abilityIdsStr = recordMapper.selectVersionAbilityIds(versionId);
                    if (abilityIdsStr != null && !abilityIdsStr.isEmpty()) {
                        List<Long> abilityIds = parseIds(abilityIdsStr);
                        if (!abilityIds.isEmpty()) {
                            List<AbilityEntity> abilities = abilityMapper.selectByIds(abilityIds);
                            vo.setCapabilityNames(abilities.stream()
                                    .map(AbilityEntity::getAbilityNameCn)
                                    .collect(Collectors.joining(", ")));
                        }
                    }

                    String applicantId = recordMapper.selectApplicantByVersionId(versionId);
                    vo.setApplicantId(applicantId);
                }

                voList.add(vo);
            }

            int totalPages = (int) ((total + pageSize - 1) / pageSize);
            ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                    .curPage(curPage)
                    .pageSize(pageSize)
                    .total(total)
                    .totalPages(totalPages)
                    .build();
            return ApiResponse.success(voList, page);
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

    private List<Long> parseIds(String idsStr) {
        if (idsStr == null || idsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }

    private java.util.Date toDate(Object value) {
        if (value instanceof java.util.Date) return (java.util.Date) value;
        return null;
    }
}
