package com.xxx.it.works.wecode.v2.modules.ability.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityCreateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityUpdateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理面能力控制器
 *
 * <p>提供能力目录的 CRUD 接口，仅平台管理员可访问。
 * 基础路径：/service/open/v2/ability/admin</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Tag(name = "能力管理", description = "管理面能力目录 CRUD 接口")
@RestController
@RequestMapping("/service/open/v2/ability/admin")
public class AdminAbilityController {

    private final AdminAbilityService adminAbilityService;

    public AdminAbilityController(AdminAbilityService adminAbilityService) {
        this.adminAbilityService = adminAbilityService;
    }

    @AuthRole
    @Operation(summary = "查询能力列表", description = "分页查询能力目录列表，支持关键字搜索和动态排序")
    @GetMapping("/list")
    public ApiResponse<List<AdminAbilityVO>> list(AdminAbilityListRequest request) {
        return adminAbilityService.list(request);
    }

    @AuthRole
    @Operation(summary = "创建能力", description = "新增能力目录记录，包含编码唯一性校验、" +
            "参数字段校验、loadType 联动校验，图标/示意图 batchId 写属性表")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody AdminAbilityCreateRequest request) {
        return adminAbilityService.create(request);
    }

    @AuthRole
    @Operation(summary = "编辑能力", description = "部分更新能力记录，仅更新传入字段，abilityType 不可修改。" +
            "loadType=2 时校验三要素必填，乐观锁基于 lastUpdateTime")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminAbilityUpdateRequest request) {
        return adminAbilityService.update(id, request);
    }

    @AuthRole
    @Operation(summary = "删除能力", description = "根据 id 删除能力记录及其关联属性记录。" +
            "能力不存在时返回 404。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return adminAbilityService.delete(id);
    }
}
