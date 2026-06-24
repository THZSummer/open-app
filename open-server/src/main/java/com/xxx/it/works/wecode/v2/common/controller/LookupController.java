package com.xxx.it.works.wecode.v2.common.controller;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用查询控制器
 *
 * <p>提供灰度发布白名单等 lookup 配置查询接口</p>
 * <p>数据来源：openplatform_lookup_item_t 表，classify_code = APP_UI_WHITELIST</p>
 * <p>白名单规则：
 *   <ul>
 *     <li>[{itemCode:"all"}] → 全员走新页面</li>
 *     <li>[{itemCode:"userId1"},{itemCode:"userId2"}] → 仅白名单用户走新页面</li>
 *     <li>[] 空数组 → 全员走旧页面</li>
 *   </ul>
 * </p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Tag(name = "通用查询", description = "灰度发布白名单等配置查询接口")
@RestController
@RequestMapping("/service/open/v2/lookup")
public class LookupController {

    private static final String CLASSIFY_CODE = "APP_UI_WHITELIST";

    @Autowired
    private LookupWhitelistMapper lookupWhitelistMapper;

    @Operation(summary = "灰度发布白名单", description = "从 lookup_item_t 表查询 APP_UI_WHITELIST 分类的启用项")
    @GetMapping("/whitelist")
    public ApiResponse<List<Map<String, String>>> whitelist() {
        List<Map<String, String>> items = lookupWhitelistMapper.selectItemsByClassifyCode(CLASSIFY_CODE);
        if (items == null) {
            items = Collections.emptyList();
        }
        return ApiResponse.success(items);
    }
}
