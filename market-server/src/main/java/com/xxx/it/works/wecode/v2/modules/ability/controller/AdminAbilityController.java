package com.xxx.it.works.wecode.v2.modules.ability.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.common.security.AuthRole;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.service.AdminAbilityService;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
