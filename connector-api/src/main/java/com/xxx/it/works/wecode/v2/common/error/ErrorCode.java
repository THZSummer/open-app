package com.xxx.it.works.wecode.v2.common.error;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一错误码枚举 — connector-api 执行层
 * <p>
 * 每个枚举值内聚四维：X-Code, messageZh, messageEn, HTTP Status。
 * 码段统一 5 位，按首位分类：
 * <ul>
 *   <li>2xxxx — 成功</li>
 *   <li>41xxx~43xxx — 校验错误（前置拦截，流未执行）</li>
 *   <li>60000~66xxx — 执行错误（流已执行，节点失败/超时）</li>
 *   <li>50000 — 系统错误</li>
 * </ul>
 * </p>
 */
public enum ErrorCode {

    // ────── 成功 / 系统 ──────
    SUCCESS         ("20000", "成功",                      "Success",                    200),
    INTERNAL_ERROR  ("50000", "平台内部异常，请联系管理员",     "Internal server error",       500),

    // ────── 校验 — 请求不合法 (41xxx) ──────
    BAD_REQUEST                 ("41001", "请求参数缺失或格式不合法",           "Bad request",                    400),
    INPUT_CONTRACT_FAILED       ("41002", "触发器输入参数校验失败",              "Bad request",                    400),
    TRIGGER_TYPE_UNKNOWN        ("41003", "触发方式缺失或未知",                "Bad request",                    400),
    TRIGGER_INPUT_MISSING       ("41004", "HTTP 触发器缺少输入契约配置",        "Bad request",                    400),

    // ────── 校验 — 资源不存在 (411xx) ──────
    FLOW_NOT_FOUND              ("41101", "连接流不存在或已被删除",             "Flow not found",                 400),
    CONNECTOR_NOT_FOUND         ("41102", "连接器不存在或已被删除",             "Flow not found",                 400),
    CONNECTOR_VERSION_NOT_FOUND ("41103", "连接器版本不存在",                  "Flow not found",                 400),

    // ────── 校验 — 状态冲突 / 前置条件 (412xx~413xx) ──────
    FLOW_NOT_RUNNING            ("41201", "连接流未启动，请先启动后再调用",       "Flow not running",               400),
    DEPLOYED_VERSION_UNAVAILABLE("41301", "已部署版本不可用，请重新部署",         "Trigger execution failed",       400),
    CONNECTOR_VERSION_INVALID   ("41302", "连接器版本已失效",                  "Trigger execution failed",       400),
    CONNECTOR_INVALID           ("41303", "连接器已失效",                     "Trigger execution failed",       400),

    // ────── 校验 — 认证 / 鉴权 (42xxx~43xxx) ──────
    AUTH_NOT_WHITELIST          ("42001", "调用凭证不在白名单中",              "Authentication failed",          401),
    AUTH_MISSING_OR_EXPIRED     ("42002", "调用凭证缺失或已过期",              "Authentication failed",          401),
    URL_WHITELIST_DENIED        ("43001", "目标 URL 未通过白名单校验",          "URL whitelist denied",           403),

    // ────── 执行 — DAG 通用 (60xxx) ──────
    ORCH_EXECUTION_FAILED       ("60000", "编排执行失败",                     "Trigger execution failed",       400),
    NODE_EXECUTION_FAILED       ("60001", "节点执行失败",                     "Trigger execution failed",       400),
    NODE_TIMEOUT_OR_ERROR       ("60002", "节点超时或执行错误",                 "Trigger execution failed",       400),

    // ────── 执行 — 编排 / 配置 (61xxx) ──────
    ORCH_PARSE_FAILED           ("61001", "编排配置 JSON 解析失败",            "Trigger execution failed",       400),
    ORCH_NO_TRIGGER             ("61002", "编排配置中缺少触发器节点",            "Trigger execution failed",       400),
    ORCH_NO_EXIT                ("61003", "编排配置中缺少出口节点",             "Trigger execution failed",       400),
    ORCH_EDGE_MISSING           ("61004", "节点间连接关系缺失",                "Trigger execution failed",       400),
    TRIGGER_TYPE_MISSING        ("61010", "触发器节点未配置触发方式",            "Trigger execution failed",       400),
    TRIGGER_CREDENTIAL_MISSING  ("61011", "触发器 SYSTOKEN 凭证不存在或已过期",   "Trigger execution failed",       400),
    TRIGGER_CREDENTIAL_NOT_WHITELIST ("61012", "触发器调用凭证不在白名单中",     "Trigger execution failed",       400),
    CONNECTOR_NOT_SELECTED      ("61020", "连接器节点未选择连接器",             "Trigger execution failed",       400),
    CONNECTOR_VERSION_NOT_SELECTED ("61021", "连接器节点未选择版本",            "Trigger execution failed",       400),
    CONNECTOR_TIMEOUT_EXCEEDS   ("61022", "连接器节点超时值超过上限",            "Trigger execution failed",       400),
    CONNECTOR_INPUT_FIELD_MISSING ("61023", "连接器入参映射引用了不存在的字段",    "Trigger execution failed",       400),
    CONNECTOR_AUTH_MISSING      ("61024", "连接器缺少认证配置",                "Trigger execution failed",       400),
    CONNECTOR_AUTH_TYPE_NOT_SELECTED ("61025", "连接器未选择认证类型",           "Trigger execution failed",       400),
    SCRIPT_EMPTY                ("61030", "脚本节点源码为空",                  "Trigger execution failed",       400),
    SCRIPT_TOO_LONG             ("61031", "脚本节点源码超过字符上限",            "Trigger execution failed",       400),
    SCRIPT_NO_MAIN              ("61032", "脚本节点缺少 main(ctx) 函数",        "Trigger execution failed",       400),
    SCRIPT_SYNTAX_ERROR         ("61033", "脚本节点存在语法错误",               "Trigger execution failed",       400),
    PARALLEL_TOO_FEW_BRANCHES   ("61040", "并行节点分支数不足（最少 2 个）",      "Trigger execution failed",       400),
    PARALLEL_TOO_MANY_BRANCHES  ("61041", "并行节点分支数超过上限（最多 8 个）",    "Trigger execution failed",       400),
    PARALLEL_BRANCH_EMPTY       ("61042", "并行节点分支内无节点",               "Trigger execution failed",       400),
    EXIT_FIELD_MISSING          ("61050", "出口节点输出映射引用了不存在的字段",     "Trigger execution failed",       400),
    EXIT_MAPPING_FORMAT_ERROR   ("61051", "出口节点输出映射格式错误",            "Trigger execution failed",       400),

    // ────── 执行 — 连接器运行时 (62xxx) ──────
    CONNECTOR_HTTP_FAILED       ("62001", "连接器调用下游失败",                "Trigger execution failed",       400),
    CONNECTOR_CONNECT_TIMEOUT   ("62002", "连接器连接目标超时",                "Trigger execution failed",       400),
    CONNECTOR_READ_TIMEOUT      ("62003", "连接器读取响应超时",                "Trigger execution failed",       400),
    CONNECTOR_DNS_FAILED        ("62004", "连接器目标地址解析失败",             "Trigger execution failed",       400),
    CONNECTOR_SSL_FAILED        ("62005", "连接器 SSL 证书校验失败",            "Trigger execution failed",       400),
    CONNECTOR_SERIALIZE_FAILED  ("62006", "连接器请求参数序列化失败",            "Trigger execution failed",       400),
    CONNECTOR_RESPONSE_TOO_LARGE("62007", "连接器下游响应体超过限制",            "Trigger execution failed",       400),

    // ────── 执行 — 脚本运行时 (63xxx) ──────
    SCRIPT_RUNTIME_ERROR        ("63001", "脚本节点运行时异常",                "Trigger execution failed",       400),
    SCRIPT_TIMEOUT              ("63002", "脚本节点执行超时",                  "Trigger execution failed",       400),
    SCRIPT_STATEMENT_LIMIT      ("63003", "脚本节点执行超过语句上限",            "Trigger execution failed",       400),
    SCRIPT_RETURN_NOT_OBJECT    ("63004", "脚本节点返回值不是对象类型",           "Trigger execution failed",       400),
    SCRIPT_FIELD_NOT_FOUND      ("63005", "脚本节点访问了不存在的上游字段",        "Trigger execution failed",       400),

    // ────── 执行 — 超时 / 并行 / 出口 (64xxx~66xxx) ──────
    ORCH_NODE_TIMEOUT           ("64000", "节点执行超时",                     "Trigger execution failed",       400),
    PARALLEL_BRANCH_FAILED      ("65001", "并行分支执行失败",                  "Trigger execution failed",       400),
    PARALLEL_BRANCH_TIMEOUT     ("65002", "并行分支执行超时",                  "Trigger execution failed",       400),
    PARALLEL_ALL_FAILED         ("65003", "所有并行分支均执行失败",              "Trigger execution failed",       400),
    EXIT_SERIALIZE_FAILED       ("66001", "出口节点响应体序列化失败",            "Trigger execution failed",       400),
    EXIT_HEADER_FAILED          ("66002", "出口节点响应头设置失败",             "Trigger execution failed",       400),

    // ────── 校验 — 调试 (41104 / 414xx) ──────
    VERSION_NOT_FOUND              ("41104", "版本不存在，请检查版本 ID",       "Version not found",              400),
    ORCHESTRATION_EMPTY            ("41401", "编排配置为空，请先完成编排后再调试", "Orchestration config is empty",   400),
    VERSION_STATUS_NOT_DEBUGGABLE  ("41402", "版本状态不支持调试",              "Version status not debuggable",   400),
    ;

    // ── 枚举字段 ──────────────────────────────────────

    private final String code;
    private final String messageZh;
    private final String messageEn;
    private final int httpStatus;

    ErrorCode(String code, String messageZh, String messageEn, int httpStatus) {
        this.code = code;
        this.messageZh = messageZh;
        this.messageEn = messageEn;
        this.httpStatus = httpStatus;
    }

    public String code()       { return code; }
    public String messageZh()  { return messageZh; }
    public String messageEn()  { return messageEn; }
    public int httpStatus()    { return httpStatus; }
    public HttpStatus status() { return HttpStatus.valueOf(httpStatus); }

    // ── 工具方法 ──────────────────────────────────────

    /** 构建结构化 errorInfo Map (供 Executor 层使用) */
    public Map<String, Object> toErrorInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("code", code);
        info.put("messageZh", messageZh);
        info.put("messageEn", messageEn);
        return info;
    }

    /**
     * 构建 errorInfo Map 并覆盖 messageZh / messageEn（供 Executor 层动态消息使用）。
     * @deprecated 迁移到 {@link #toErrorInfo()} + put 覆盖。
     */
    @Deprecated
    public static Map<String, Object> errorInfo(ErrorCode code, String messageZh, String messageEn) {
        Map<String, Object> info = code.toErrorInfo();
        info.put("messageZh", messageZh);
        info.put("messageEn", messageEn);
        return info;
    }

    /** 按 code 字符串精确查找 */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) return ec;
        }
        return INTERNAL_ERROR;
    }
}
