package com.xxx.it.works.wecode.v2.modules.ability.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityCreateRequest;
import com.xxx.it.works.wecode.v2.modules.ability.dto.admin.AdminAbilityListRequest;
import com.xxx.it.works.wecode.v2.modules.ability.vo.admin.AdminAbilityVO;

import java.util.List;

/**
 * 管理面能力服务接口
 *
 * <p>提供能力目录的 CRUD 操作。本接口为管理面（Admin）专用，
 * 与开放面（Open）查询接口分离，避免互相影响。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface AdminAbilityService {

    /**
     * 分页查询能力列表
     *
     * <p>支持关键字模糊搜索（中文名/英文名）、动态排序、字段白名单校验。
     * 图标/示意图 URL 从属性表关联查询后组装。</p>
     *
     * @param request 查询请求（含分页、关键字、排序参数）
     * @return 分页响应，data 为能力列表，page 为分页信息
     */
    ApiResponse<List<AdminAbilityVO>> list(AdminAbilityListRequest request);

    /**
     * 创建能力
     *
     * <p>校验能力类型编码唯一性、loadType 联动校验、entryUrl 格式校验，
     * 写主表 ability_t 和属性表 ability_p_t（图标/示意图）。</p>
     *
     * @param request 创建请求（含所有业务字段和校验注解）
     * @return 标准响应，成功时 code=200
     */
    ApiResponse<Void> create(AdminAbilityCreateRequest request);
}
