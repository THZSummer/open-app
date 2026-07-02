package com.xxx.it.works.wecode.v2.modules.app.constants;

import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用属性名常量（openplatform_app_p_t.property_name）
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class AppPropertyConstants {

    private AppPropertyConstants() {
    }

    /**
     * EAMAP 编码
     */
    public static final String PROP_EAMAP_CODE = "eamap_app_code";

    /**
     * 认证方式（旧字段，单选值，保留不动）
     */
    public static final String PROP_VERIFY_TYPE = "verify_type";

    /**
     * 认证方式（新字段，多选值，逗号分隔）
     */
    public static final String PROP_VERIFY_TYPE_V2 = "verify_type_v2";

    /**
     * API 密钥
     */
    public static final String PROP_API_SECRET = "api_secret";

    /**
     * 架构图ID列表
     */
    public static final String PROP_DIAGRAM_ID_LIST = "diagram_id_list";

    /**
     * EAMAP 应用名称
     */
    public static final String PROP_EAMAP_APP_NAME = "eamap_app_name";

    /**
     * 从属性列表中解析认证方式，优先返回新字段 verify_type_v2，没值返回旧字段 verify_type。
     *
     * @param props 应用属性列表
     * @return 认证方式 code 列表，都没值返回空列表
     */
    public static List<Integer> resolveVerifyTypeList(List<AppProperty> props) {
        if (props == null) {
            return Collections.emptyList();
        }
        String v2 = null;
        String old = null;
        for (AppProperty p : props) {
            if (PROP_VERIFY_TYPE_V2.equals(p.getPropertyName())) {
                v2 = p.getPropertyValue();
            } else if (PROP_VERIFY_TYPE.equals(p.getPropertyName())) {
                old = p.getPropertyValue();
            }
        }
        String raw = StringUtils.hasText(v2) ? v2 : old;
        if (raw == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
