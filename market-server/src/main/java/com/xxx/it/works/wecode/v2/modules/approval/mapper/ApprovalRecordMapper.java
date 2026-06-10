package com.xxx.it.works.wecode.v2.modules.approval.mapper;

import com.xxx.it.works.wecode.v2.modules.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalRecordMapper {

    ApprovalRecord selectById(@Param("id") Long id);

    List<ApprovalRecord> selectPendingList(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPendingList();

    List<Map<String, Object>> selectPublishedList(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPublishedList();

    int update(ApprovalRecord record);

    String selectApplicantByVersionId(@Param("versionId") Long versionId);

    List<String> selectCapabilityNames(@Param("appPkId") Long appPkId);

    String selectThirdPartyAppId(@Param("appPkId") Long appPkId);
}
