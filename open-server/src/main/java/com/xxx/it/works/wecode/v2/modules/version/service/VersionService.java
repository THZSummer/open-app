package com.xxx.it.works.wecode.v2.modules.version.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.dto.UpdateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.vo.AppVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.version.vo.VersionVO;

import java.util.List;

/**
 * 版本服务接口
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
public interface VersionService {

    ApiResponse<List<VersionVO>> getVersionList(String appId, Integer curPage, Integer pageSize);

    String createVersion(String appId, CreateVersionRequest request);

    AppVersionDetailVO getVersionDetail(String appId, Long versionId);

    void publishVersion(String appId, Long versionId);

    void withdrawVersion(String appId, Long versionId);

    void deleteVersion(String appId, Long versionId);

    void updateVersion(String appId, Long versionId, UpdateVersionRequest request);
}
