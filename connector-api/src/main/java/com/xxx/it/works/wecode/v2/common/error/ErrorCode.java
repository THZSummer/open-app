package com.xxx.it.works.wecode.v2.common.error;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一错误码常量 — connector-api 执行层
 * <p>
 * 码段分配:
 * <ul>
 *   <li>4xx — HTTP 前置校验错误</li>
 *   <li>61xxx — 编排层错误</li>
 *   <li>62xxx — 连接器节点错误</li>
 *   <li>63xxx — 脚本节点错误</li>
 *   <li>64xxx — 超时错误</li>
 *   <li>65xxx — 并行节点错误</li>
 *   <li>66xxx — 出口节点错误</li>
 * </ul>
 * </p>
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ===== 编排通用 (61xxx) =====
    public static final String ORCH_PARSE_FAILED = "61001";
    public static final String ORCH_NO_TRIGGER = "61002";
    public static final String ORCH_NO_EXIT = "61003";
    public static final String ORCH_EDGE_MISSING = "61004";

    // ===== 触发器节点 (610xx) =====
    public static final String TRIGGER_TYPE_MISSING = "61010";
    public static final String TRIGGER_CREDENTIAL_MISSING = "61011";
    public static final String TRIGGER_CREDENTIAL_NOT_WHITELIST = "61012";

    // ===== 连接器节点 — 配置 (6102x) =====
    public static final String CONNECTOR_NOT_SELECTED = "61020";
    public static final String CONNECTOR_VERSION_NOT_SELECTED = "61021";
    public static final String CONNECTOR_TIMEOUT_EXCEEDS = "61022";
    public static final String CONNECTOR_INPUT_FIELD_MISSING = "61023";
    public static final String CONNECTOR_AUTH_MISSING = "61024";
    public static final String CONNECTOR_AUTH_TYPE_NOT_SELECTED = "61025";

    // ===== 连接器节点 — 运行时 (62xxx) =====
    public static final String CONNECTOR_HTTP_FAILED = "62001";
    public static final String CONNECTOR_CONNECT_TIMEOUT = "62002";
    public static final String CONNECTOR_READ_TIMEOUT = "62003";
    public static final String CONNECTOR_DNS_FAILED = "62004";
    public static final String CONNECTOR_SSL_FAILED = "62005";
    public static final String CONNECTOR_SERIALIZE_FAILED = "62006";
    public static final String CONNECTOR_RESPONSE_TOO_LARGE = "62007";

    // ===== 脚本节点 — 配置 (6103x) =====
    public static final String SCRIPT_EMPTY = "61030";
    public static final String SCRIPT_TOO_LONG = "61031";
    public static final String SCRIPT_NO_MAIN = "61032";
    public static final String SCRIPT_SYNTAX_ERROR = "61033";

    // ===== 脚本节点 — 运行时 (63xxx) =====
    public static final String SCRIPT_RUNTIME_ERROR = "63001";
    public static final String SCRIPT_TIMEOUT = "63002";
    public static final String SCRIPT_STATEMENT_LIMIT = "63003";
    public static final String SCRIPT_RETURN_NOT_OBJECT = "63004";
    public static final String SCRIPT_FIELD_NOT_FOUND = "63005";

    // ===== 并行节点 — 配置 (6104x) =====
    public static final String PARALLEL_TOO_FEW_BRANCHES = "61040";
    public static final String PARALLEL_TOO_MANY_BRANCHES = "61041";
    public static final String PARALLEL_BRANCH_EMPTY = "61042";

    // ===== 并行节点 — 运行时 (65xxx) =====
    public static final String PARALLEL_BRANCH_FAILED = "65001";
    public static final String PARALLEL_BRANCH_TIMEOUT = "65002";
    public static final String PARALLEL_ALL_FAILED = "65003";

    // ===== 出口节点 — 配置 (6105x) =====
    public static final String EXIT_FIELD_MISSING = "61050";
    public static final String EXIT_MAPPING_FORMAT_ERROR = "61051";

    // ===== 出口节点 — 运行时 (66xxx) =====
    public static final String EXIT_SERIALIZE_FAILED = "66001";
    public static final String EXIT_HEADER_FAILED = "66002";

    // ===== 前置校验 (HTTP 码) =====
    public static final String PRECHECK_VERSION_NOT_FOUND = "404";
    public static final String PRECHECK_FLOW_NOT_FOUND = "404";
    public static final String PRECHECK_CONNECTOR_NOT_FOUND = "404";
    public static final String PRECHECK_CONNECTOR_VERSION_NOT_FOUND = "404";
    public static final String PRECHECK_FLOW_NOT_RUNNING = "409";
    public static final String PRECHECK_VERSION_INVALIDATED = "422";
    public static final String PRECHECK_VERSION_STATUS_NOT_DEBUGGABLE = "422";
    public static final String PRECHECK_ORCHESTRATION_EMPTY = "422";
    public static final String PRECHECK_DEPLOYED_VERSION_UNAVAILABLE = "422";
    public static final String PRECHECK_CONNECTOR_VERSION_INVALIDATED = "422";
    public static final String PRECHECK_CONNECTOR_INVALIDATED = "422";
    public static final String PRECHECK_AUTH_FAILED = "401";
    public static final String PRECHECK_BAD_REQUEST = "400";
    public static final String PRECHECK_URL_WHITELIST_DENIED = "403";

    /** 完整错误码集合（用于唯一性校验） */
    public static final java.util.Set<String> ALL_CODES = java.util.Set.of(
        ORCH_PARSE_FAILED, ORCH_NO_TRIGGER, ORCH_NO_EXIT, ORCH_EDGE_MISSING,
        TRIGGER_TYPE_MISSING, TRIGGER_CREDENTIAL_MISSING, TRIGGER_CREDENTIAL_NOT_WHITELIST,
        CONNECTOR_NOT_SELECTED, CONNECTOR_VERSION_NOT_SELECTED, CONNECTOR_TIMEOUT_EXCEEDS,
        CONNECTOR_INPUT_FIELD_MISSING, CONNECTOR_AUTH_MISSING, CONNECTOR_AUTH_TYPE_NOT_SELECTED,
        CONNECTOR_HTTP_FAILED, CONNECTOR_CONNECT_TIMEOUT, CONNECTOR_READ_TIMEOUT,
        CONNECTOR_DNS_FAILED, CONNECTOR_SSL_FAILED, CONNECTOR_SERIALIZE_FAILED, CONNECTOR_RESPONSE_TOO_LARGE,
        SCRIPT_EMPTY, SCRIPT_TOO_LONG, SCRIPT_NO_MAIN, SCRIPT_SYNTAX_ERROR,
        SCRIPT_RUNTIME_ERROR, SCRIPT_TIMEOUT, SCRIPT_STATEMENT_LIMIT,
        SCRIPT_RETURN_NOT_OBJECT, SCRIPT_FIELD_NOT_FOUND,
        PARALLEL_TOO_FEW_BRANCHES, PARALLEL_TOO_MANY_BRANCHES, PARALLEL_BRANCH_EMPTY,
        PARALLEL_BRANCH_FAILED, PARALLEL_BRANCH_TIMEOUT, PARALLEL_ALL_FAILED,
        EXIT_FIELD_MISSING, EXIT_MAPPING_FORMAT_ERROR, EXIT_SERIALIZE_FAILED, EXIT_HEADER_FAILED
    );

    /**
     * 构建结构化 errorInfo Map
     */
    public static Map<String, Object> errorInfo(String code, String messageZh, String messageEn) {
        Map<String, Object> info = new HashMap<>();
        info.put("code", code);
        info.put("messageZh", messageZh);
        info.put("messageEn", messageEn != null ? messageEn : messageZh);
        return info;
    }

    public static Map<String, Object> errorInfo(String code, String messageZh) {
        return errorInfo(code, messageZh, null);
    }
}
