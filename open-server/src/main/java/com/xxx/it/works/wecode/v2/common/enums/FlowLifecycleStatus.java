package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 连接流生命周期状态枚举
 *
 * <p>对应表 openplatform_v2_cp_flow_t.lifecycle_status 字段</p>
 * <p>生命周期: 已停止 ⇄ 运行中 → 已失效 → 物理删除</p>
 * <p>部署不改变状态，仅切换版本绑定</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §1.7.3 连接流生命周期
 */
@Getter
@AllArgsConstructor
public enum FlowLifecycleStatus {

    /** 已停止 */
    STOPPED(1, "已停止"),

    /** 运行中 */
    RUNNING(2, "运行中"),

    /** 已失效 */
    INVALIDATED(3, "已失效"),

    /** 物理删除（终态） */
    DELETED(4, "物理删除");

    /** 数据库存储值 (TINYINT) */
    private final Integer code;

    /** 中文描述 */
    private final String description;

    /** 默认状态：已停止 */
    public static final FlowLifecycleStatus DEFAULT = STOPPED;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举实例，未找到返回 null
     */
    public static FlowLifecycleStatus fromValue(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowLifecycleStatus status : values()) {
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
    public static boolean isValidTransition(FlowLifecycleStatus from, FlowLifecycleStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case STOPPED:
                return to == RUNNING || to == INVALIDATED;
            case RUNNING:
                return to == STOPPED;
            case INVALIDATED:
                return to == STOPPED || to == DELETED;
            case DELETED:
                return false;
            default:
                return false;
        }
    }
}
