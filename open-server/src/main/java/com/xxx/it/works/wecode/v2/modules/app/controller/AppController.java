package com.xxx.it.works.wecode.v2.modules.app.controller;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.dto.BindEamapRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.CreateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateVerifyTypeRequest;
import com.xxx.it.works.wecode.v2.common.enums.AppIdSourceEnum;
import com.xxx.it.works.wecode.v2.modules.app.service.AppService;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppBasicInfoVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppIdentityVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppListItemVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppVerifyTypeVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.BindEamapVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.CreateAppVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.CurrentRoleVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.EamapVO;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用管理控制器
 *
 * <p>实现 12 个 1.x 接口</p>
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/service/open/v2/app")
public class AppController {

    @Autowired
    private AppService appService;

    /**
     * 1.1 创建应用
     */
    @AuditLog(value = OperateEnum.CREATE_APP, appIdSource = AppIdSourceEnum.RESPONSE_FIELD)
    @PostMapping("")
    public ApiResponse<CreateAppVO> createApp(@RequestBody @Validated CreateAppRequest request) {
        String appId = appService.saveApp(request);
        return ApiResponse.success(new CreateAppVO(appId));
    }

    /**
     * 1.2 更新应用
     */
    @AuditLog(value = OperateEnum.UPDATE_APP)
    @PutMapping("")
    public ApiResponse<Void> updateApp(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated UpdateAppRequest request) {
        appService.updateApp(appId, request);
        return ApiResponse.success();
    }

    /**
     * 1.3 获取应用基本信息
     */
    @GetMapping("")
    public ApiResponse<AppBasicInfoVO> getAppBasicInfo(@RequestParam @NotBlank String appId) {
        AppBasicInfoVO vo = appService.getAppBasicInfo(appId);
        return ApiResponse.success(vo);
    }

    /**
     * 1.4 获取应用列表
     */
    @GetMapping("/list")
    public ApiResponse<List<AppListItemVO>> getAppList(
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return appService.getAppList(curPage, pageSize);
    }

    /**
     * 1.5 获取 EAMAP 列表（预留）
     */
    @GetMapping("/eamap")
    public ApiResponse<List<EamapVO>> getEamapList(
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return appService.getEamapList(curPage, pageSize);
    }

    /**
     * 1.6 获取默认图标列表（预留）
     */
    @GetMapping("/icons")
    public ApiResponse<List<FileV2VO>> getDefaultIcons() {
        List<FileV2VO> list = appService.getDefaultIcons();
        return ApiResponse.success(list);
    }

    /**
     * 1.7 更新认证方式
     */
    @AuditLog(value = OperateEnum.UPDATE_APP_VERIFY_TYPE)
    @PutMapping("/verify-type")
    public ApiResponse<Void> updateVerifyType(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated UpdateVerifyTypeRequest request) {
        appService.updateVerifyType(appId, request);
        return ApiResponse.success();
    }

    /**
     * 1.8 获取应用凭证
     */
    @GetMapping("/identity")
    public ApiResponse<AppIdentityVO> getAppIdentity(@RequestParam @NotBlank String appId) {
        AppIdentityVO vo = appService.getAppIdentity(appId);
        return ApiResponse.success(vo);
    }

    /**
     * 1.9 获取认证方式
     */
    @GetMapping("/verify-type")
    public ApiResponse<AppVerifyTypeVO> getVerifyType(@RequestParam @NotBlank String appId) {
        AppVerifyTypeVO vo = appService.getVerifyType(appId);
        return ApiResponse.success(vo);
    }

    /**
     * 1.10 绑定 EAMAP
     */
    @AuditLog(value = OperateEnum.BIND_APP_EAMAP)
    @PostMapping("/bind-eamap")
    public ApiResponse<BindEamapVO> bindEamap(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated BindEamapRequest request) {
        appService.saveEamapBinding(appId, request);
        return ApiResponse.success(new BindEamapVO(appId));
    }

    /**
     * 1.11 获取当前用户角色
     */
    @GetMapping("/current-role")
    public ApiResponse<CurrentRoleVO> getCurrentRole(@RequestParam @NotBlank String appId) {
        Integer role = appService.getCurrentRole(appId);
        return ApiResponse.success(new CurrentRoleVO(role));
    }
}
