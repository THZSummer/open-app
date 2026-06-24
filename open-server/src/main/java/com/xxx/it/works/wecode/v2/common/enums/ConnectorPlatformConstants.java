package com.xxx.it.works.wecode.v2.common.enums;

/**
 * 连接器平台业务常量
 *
 * <p>定义 V3 所有关键上限常量、默认值和配置项</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 * @see spec.md §1.6 关键设计决策
 */
public final class ConnectorPlatformConstants {

    private ConnectorPlatformConstants() {
        // 工具类，禁止实例化
    }

    // ===== 版本管理 =====

    /** 版本数量上限 */
    public static final int MAX_VERSION_COUNT = 1000;

    /** 草稿默认版本号 */
    public static final int DRAFT_DEFAULT_VERSION_NUMBER = 1;

    // ===== 脚本节点 =====

    /** 每连接流脚本节点数量上限 */
    public static final int MAX_SCRIPT_NODES_PER_FLOW = 10;

    /** 脚本源码最大字符数 */
    public static final int MAX_SCRIPT_SOURCE_LENGTH = 10000;

    // ===== 并行分支 =====

    /** 并行处理节点分支数上限 */
    public static final int MAX_PARALLEL_BRANCHES = 8;

    /** 并行处理节点分支数下限 */
    public static final int MIN_PARALLEL_BRANCHES = 2;

    // ===== 缓存 =====

    /** 缓存 TTL 上限（秒）：15 天 */
    public static final int MAX_CACHE_TTL_SECONDS = 1296000;

    /** 缓存 TTL 下限（秒） */
    public static final int MIN_CACHE_TTL_SECONDS = 1;

    // ===== 超时 =====

    /** 平台默认超时（秒） */
    public static final int DEFAULT_TIMEOUT_SECONDS = 5;

    /** 脚本节点默认超时（秒） */
    public static final int DEFAULT_SCRIPT_TIMEOUT_SECONDS = 5;

    /** 脚本节点最大超时（秒） */
    public static final int MAX_SCRIPT_TIMEOUT_SECONDS = 30;

    // ===== 限流 =====

    /** 平台默认 QPS */
    public static final int DEFAULT_QPS_LIMIT = 1000;

    /** 平台默认并发数 */
    public static final int DEFAULT_CONCURRENCY_LIMIT = 1000;

    // ===== 运行记录 =====

    /** 每连接流运行记录条数上限（平台默认） */
    public static final int DEFAULT_EXECUTION_RECORD_LIMIT = 1000;

    /** 运行记录保留天数 */
    public static final int EXECUTION_RECORD_RETENTION_DAYS = 30;

    /** 定时清理批量删除大小 */
    public static final int CLEANUP_BATCH_SIZE = 1000;

    // ===== 调试 =====

    /** 调试执行超时（秒） */
    public static final int DEBUG_EXECUTION_TIMEOUT_SECONDS = 30;

    /** 调试线程池最大线程数 */
    public static final int DEBUG_THREAD_POOL_MAX_SIZE = 5;

    // ===== 应用白名单 =====

    /** 应用白名单 classify_code */
    public static final String APP_WHITELIST_CLASSIFY_CODE = "cp_app_whitelist";

    // ===== 审批 =====

    /** 连接流版本发布审批 businessType */
    public static final String APPROVAL_BUSINESS_TYPE_FLOW_VERSION_PUBLISH = "connector_flow_version_publish";

    // ===== 实体缓存 =====

    /** 实体缓存默认 TTL（秒）：7 天 */
    public static final int ENTITY_CACHE_TTL_SECONDS = 604800;

    /** 实体缓存 TTL 抖动范围（秒）：2 小时 */
    public static final int ENTITY_CACHE_TTL_JITTER_SECONDS = 7200;

    // ===== URL 白名单 =====

    /** URL 白名单正则编译缓存 TTL（秒）：5 分钟 */
    public static final int URL_WHITELIST_PATTERN_CACHE_TTL_SECONDS = 300;
}
