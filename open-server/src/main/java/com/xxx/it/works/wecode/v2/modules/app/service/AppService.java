package com.xxx.it.works.wecode.v2.modules.app.service;

import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.dto.BindEamapRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.CreateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateVerifyTypeRequest;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppBasicInfoVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppIdentityVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppListItemVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppVerifyTypeVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.EamapVO;

import java.util.List;

/**
 * 应用服务接口
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
public interface AppService {

    /**
     * 1.1 创建应用
     */
    String saveApp(CreateAppRequest request);

    /**
     * 1.2 更新应用
     */
    void updateApp(String appId, UpdateAppRequest request);

    /**
     * 1.3 获取应用基本信息
     */
    AppBasicInfoVO getAppBasicInfo(String appId);

    /**
     * 1.4 获取应用列表
     */
    ApiResponse<List<AppListItemVO>> getAppList(Integer curPage, Integer pageSize);

    /**
     * 1.5 获取 EAMAP 列表
     */
    ApiResponse<List<EamapVO>> getEamapList(Integer curPage, Integer pageSize);

    /**
     * 1.6 获取默认图标列表
     */
    List<FileV2VO> getDefaultIcons();

    /**
     * 1.7 更新认证方式
     */
    void updateVerifyType(String appId, UpdateVerifyTypeRequest request);

    /**
     * 1.8 获取应用凭证
     */
    AppIdentityVO getAppIdentity(String appId);

    /**
     * 1.9 获取认证方式
     */
    AppVerifyTypeVO getVerifyType(String appId);

    /**
     * 1.10 绑定 EAMAP
     */
    void saveEamapBinding(String appId, BindEamapRequest request);

    /**
     * 1.11 获取当前用户角色
     */
    Integer getCurrentRole(String appId);
}
