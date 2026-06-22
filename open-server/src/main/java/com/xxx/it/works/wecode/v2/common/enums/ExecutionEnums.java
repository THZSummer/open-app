package com.xxx.it.works.wecode.v2.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行相关枚举聚合
 *
 * <p>包含 ExecutionStatus / TriggerType / NodeType / CacheStatus 四个子枚举</p>
 * <p>对应 execution_record_t 和 execution_step_t 的枚举字段</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class ExecutionEnums {

    private ExecutionEnums() {
        // 工具类，禁止实例化
    }

    /**
     * 执行状态枚举
     *
     * <p>对应 execution_record_t.status / execution_step_t.status</p>
     */
    @Getter
    @AllArgsConstructor
    public enum ExecutionStatus {

        /** 成功 */
        SUCCESS(0, "成功"),

        /** 失败 */
        FAILED(1, "失败"),

        /** 执行中（运行时状态） */
        PENDING(2, "执行中");

        private final Integer code;
        private final String description;

        public static ExecutionStatus fromValue(Integer code) {
            if (code == null) {
                return null;
            }
            for (ExecutionStatus status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 触发方式枚举
     *
     * <p>对应 execution_record_t.trigger_type</p>
     */
    @Getter
    @AllArgsConstructor
    public enum TriggerType {

        /** HTTP 触发 */
        HTTP(1, "HTTP触发"),

        /** 调试触发 */
        DEBUG(2, "调试触发");

        private final Integer code;
        private final String description;

        public static TriggerType fromValue(Integer code) {
            if (code == null) {
                return null;
            }
            for (TriggerType type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 节点类型枚举
     *
     * <p>对应 execution_step_t.node_type</p>
     */
    @Getter
    @AllArgsConstructor
    public enum NodeType {

        /** 触发器 */
        TRIGGER(1, "触发器"),

        /** 连接器 */
        CONNECTOR(2, "连接器"),

        /** 脚本 */
        SCRIPT(3, "脚本"),

        /** 并行处理 */
        PARALLEL(4, "并行处理"),

        /** 数据输出/出口 */
        EXIT(5, "出口");

        private final Integer code;
        private final String description;

        public static NodeType fromValue(Integer code) {
            if (code == null) {
                return null;
            }
            for (NodeType type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 缓存状态枚举
     *
     * <p>对应 execution_record_t.cache_status / execution_step_t.cache_status</p>
     */
    @Getter
    @AllArgsConstructor
    public enum CacheStatus {

        /** 未命中（正常执行） */
        MISS(0, "未命中"),

        /** 全流命中 */
        FULL_HIT(1, "全流命中"),

        /** 部分命中 */
        PARTIAL_HIT(2, "部分命中");

        private final Integer code;
        private final String description;

        public static CacheStatus fromValue(Integer code) {
            if (code == null) {
                return null;
            }
            for (CacheStatus status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }
}
