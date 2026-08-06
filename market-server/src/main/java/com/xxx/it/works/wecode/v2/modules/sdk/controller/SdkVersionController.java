package com.xxx.it.works.wecode.v2.modules.sdk.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionCreateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionStatusUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.dto.SdkVersionUpdateDTO;
import com.xxx.it.works.wecode.v2.modules.sdk.service.SdkVersionService;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkPermissionVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionDetailVO;
import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkVersionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SDK版本管理控制器
 *
 * <p>基础路径：/service/open/v2/sdk/version</p>
 */
@Tag(name = "SDK版本管理", description = "SDK版本的创建、编辑、废弃、恢复、查询接口")
@RestController
@RequestMapping("/service/open/v2/sdk/version")
public class SdkVersionController {

    private final SdkVersionService sdkVersionService;

    public SdkVersionController(SdkVersionService sdkVersionService) {
        this.sdkVersionService = sdkVersionService;
    }

    @AuthRole
    @Operation(summary = "上架新版本", description = "创建新版本，versionName必填，直接以已发布状态入库")
    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody SdkVersionCreateDTO request) {
        return sdkVersionService.create(request);
    }

    @AuthRole
    @Operation(summary = "编辑版本", description = "仅status=1的版本可编辑，versionName不可修改")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody SdkVersionUpdateDTO request) {
        return sdkVersionService.update(id, request);
    }

    @AuthRole
    @Operation(summary = "废弃/恢复", description = "status=2废弃（必填deprecateReason 5-2000字符），status=1恢复（清空原因）")
    @PostMapping("/updateStatus")
    public ApiResponse<Void> updateStatus(@Valid @RequestBody SdkVersionStatusUpdateDTO request) {
        return sdkVersionService.updateStatus(request);
    }

    @AuthRole
    @Operation(summary = "版本详情", description = "按ID查询版本详情，含关联权限名和废弃原因")
    @GetMapping("/{id}")
    public ApiResponse<SdkVersionDetailVO> getById(@PathVariable Long id) {
        return sdkVersionService.getById(id);
    }

    @AuthRole
    @Operation(summary = "分页列表", description = "支持status/versionName筛选，按创建时间倒序")
    @GetMapping("/list")
    public ApiResponse<List<SdkVersionListVO>> list(@RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) String versionName,
                                                     @RequestParam(defaultValue = "1") Integer curPage,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return sdkVersionService.list(status, versionName, curPage, pageSize);
    }

    @AuthRole
    @Operation(summary = "查询SDK权限列表", description = "查询SDK权限分类下的启用权限，用于上架/编辑表单选择")
    @GetMapping("/permissions")
    public ApiResponse<List<SdkPermissionVO>> listPermissions() {
        return sdkVersionService.listPermissions();
    }
}
