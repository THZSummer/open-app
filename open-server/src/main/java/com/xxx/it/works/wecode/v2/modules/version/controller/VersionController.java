package com.xxx.it.works.wecode.v2.modules.version.controller;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.version.dto.CreateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.dto.UpdateVersionRequest;
import com.xxx.it.works.wecode.v2.modules.version.service.VersionService;
import com.xxx.it.works.wecode.v2.modules.version.vo.AppVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.version.vo.VersionVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 版本管理控制器
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/service/open/v2/version")
public class VersionController {

    @Autowired
    private VersionService versionService;

    /**
     * 4.1 获取版本列表
     */
    @GetMapping("/list")
    public ApiResponse<List<VersionVO>> getVersionList(
            @RequestParam @NotBlank String appId,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return versionService.getVersionList(appId, curPage, pageSize);
    }

    /**
     * 4.2 创建版本
     */
    @AuditLog(value = OperateEnum.CREATE_APP_VERSION)
    @PostMapping("")
    public ApiResponse<String> createVersion(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated CreateVersionRequest request) {
        String versionId = versionService.createVersion(appId, request);
        return ApiResponse.success(versionId);
    }

    /**
     * 4.3 获取版本详情
     */
    @GetMapping("")
    public ApiResponse<AppVersionDetailVO> getVersionDetail(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotNull Long versionId) {
        AppVersionDetailVO vo = versionService.getVersionDetail(appId, versionId);
        return ApiResponse.success(vo);
    }

    /**
     * 4.4 发布版本
     */
    @AuditLog(value = OperateEnum.PUBLISH_APP_VERSION, resourceIdParam = "versionId")
    @PostMapping("/publish")
    public ApiResponse<Void> publishVersion(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotNull Long versionId) {
        versionService.publishVersion(appId, versionId);
        return ApiResponse.success();
    }

    /**
     * 4.5 撤回版本
     */
    @AuditLog(value = OperateEnum.WITHDRAW_APP_VERSION, resourceIdParam = "versionId")
    @PostMapping("/withdraw")
    public ApiResponse<Void> withdrawVersion(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotNull Long versionId) {
        versionService.withdrawVersion(appId, versionId);
        return ApiResponse.success();
    }

    /**
     * 4.6 删除版本
     */
    @AuditLog(value = OperateEnum.DELETE_APP_VERSION, resourceIdParam = "versionId")
    @DeleteMapping("")
    public ApiResponse<Void> deleteVersion(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotNull Long versionId) {
        versionService.deleteVersion(appId, versionId);
        return ApiResponse.success();
    }

    /**
     * 4.7 更新版本
     */
    @AuditLog(value = OperateEnum.UPDATE_APP_VERSION, resourceIdParam = "versionId")
    @PutMapping("")
    public ApiResponse<Void> updateVersion(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotNull Long versionId,
            @RequestBody @Validated UpdateVersionRequest request) {
        versionService.updateVersion(appId, versionId, request);
        return ApiResponse.success();
    }
}
