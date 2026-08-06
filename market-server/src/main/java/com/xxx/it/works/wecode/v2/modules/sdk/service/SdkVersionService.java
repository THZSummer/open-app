package com.xxx.it.works.wecode.v2.modules.sdk.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionCreateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionStatusUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkPermissionVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionListVO;

import java.util.List;

/**
 * SDK版本管理服务接口
 */
public interface SdkVersionService {

    /**
     * 上架新版本
     */
    ApiResponse<String> create(SdkVersionCreateDTO request);

    /**
     * 编辑版本（仅 status=1 可编辑）
     */
    ApiResponse<Void> update(Long id, SdkVersionUpdateDTO request);

    /**
     * 废弃/恢复
     */
    ApiResponse<Void> updateStatus(SdkVersionStatusUpdateDTO request);

    /**
     * 版本详情
     */
    ApiResponse<SdkVersionDetailVO> getById(Long id);

    /**
     * 分页列表
     */
    ApiResponse<List<SdkVersionListVO>> list(Integer status, String versionName, Integer curPage, Integer pageSize);

    /**
     * 查询 SDK 权限分类下的权限列表
     */
    ApiResponse<List<SdkPermissionVO>> listPermissions();
}
