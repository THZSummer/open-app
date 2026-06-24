package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 连接流版本状态枚举
 *
 * <p>对应表 openplatform_v2_cp_flow_version_t.status 字段</p>
 * <p>生命周期: 草稿 → 待审批 → 已撤回/已驳回 → 已发布 → 已失效 → 物理删除</p>
 * <p>7 状态含审批中间态（待审批/已撤回/已驳回）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §1.7.4 连接流版本生命周期
 */
@Getter
@AllArgsConstructor
public enum FlowVersionStatus {

    /** 草稿 */
    DRAFT(1, "草稿"),

    /** 待审批 */
    PENDING_APPROVAL(2, "待审批"),

    /** 已撤回 */
    WITHDRAWN(3, "已撤回"),

    /** 已驳回 */
    REJECTED(4, "已驳回"),

    /** 已发布 */
    PUBLISHED(5, "已发布"),

    /** 已失效 */
    INVALIDATED(6, "已失效"),

    /** 物理删除（终态） */
    DELETED(7, "物理删除");

    /** 数据库存储值 (TINYINT) */
    private final Integer code;

    /** 中文描述 */
    private final String description;


    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    /** 默认状态：草稿 */
    public static final FlowVersionStatus DEFAULT = DRAFT;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举实例，未找到返回 null
     */
    public static FlowVersionStatus fromValue(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowVersionStatus status : values()) {
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
    public static boolean isValidTransition(FlowVersionStatus from, FlowVersionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case DRAFT:
                return to == PENDING_APPROVAL || to == DELETED;
            case PENDING_APPROVAL:
                return to == WITHDRAWN || to == REJECTED || to == PUBLISHED;
            case WITHDRAWN:
                return to == DRAFT || to == DELETED;
            case REJECTED:
                return to == DRAFT || to == DELETED;
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
