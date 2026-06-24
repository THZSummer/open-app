package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 连接器版本状态枚举
 *
 * <p>对应表 openplatform_v2_cp_connector_version_t.status 字段</p>
 * <p>生命周期: 草稿 → 已发布 → 已失效 → 物理删除</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §1.7.2 连接器版本生命周期
 */
@Getter
@AllArgsConstructor
public enum ConnectorVersionStatus {

    /** 草稿 */
    DRAFT(1, "草稿"),

    /** 已发布 */
    PUBLISHED(2, "已发布"),

    /** 已失效 */
    INVALIDATED(3, "已失效"),

    /** 物理删除（终态） */
    DELETED(4, "物理删除");

    /** 数据库存储值 (TINYINT) */
    private final Integer code;

    /** 中文描述 */
    private final String description;

    /** 默认状态：草稿 */
    public static final ConnectorVersionStatus DEFAULT = DRAFT;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举实例，未找到返回 null
     */
    public static ConnectorVersionStatus fromValue(Integer code) {
        if (code == null) {
            return null;
        }
        for (ConnectorVersionStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 校验状态转换是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true-合法, false-非法
     */
    public static boolean isValidTransition(ConnectorVersionStatus from, ConnectorVersionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case DRAFT:
                return to == PUBLISHED || to == DELETED;
            case PUBLISHED:
                return to == INVALIDATED;
            case INVALIDATED:
                return to == PUBLISHED || to == DELETED;
            case DELETED:
                return false;
            default:
                return false;
        }
    }
}
