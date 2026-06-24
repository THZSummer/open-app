package com.xxx.it.works.wecode.v2.modules.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.springframework.stereotype.Component;

/**
 * GraalJS 沙箱 Context 工厂
 * <p>
 * 创建并配置五层纵深防御的 GraalJS 执行环境:
 * <ol>
 *   <li>{@code allowIO(false)} — 禁止文件/网络 IO</li>
 *   <li>{@code allowCreateThread(false)} — 禁止创建线程</li>
 *   <li>{@code allowNativeAccess(false)} — 禁止原生代码访问</li>
         *   <li>{@code allowHostAccess(EXPLICIT + Map/List)} — 禁止 {@code Java.type()} 等反射, 但允许 Map/List 键访问</li>
 *   <li>{@code allowAllAccess(false)} — 最大限制, 全部关闭</li>
 * </ol>
 * </p>
 *
 * <p>
 * 额外安全措施:
 * <ul>
 *   <li>语句限制: 10000 条 (防止死循环耗尽 CPU)</li>
 *   <li>ES2022 严格模式 ({@code "use strict"} 隐式启用)</li>
 * </ul>
 * </p>
 *
 * <p>
 * 每次脚本执行创建新的 Context, 执行完毕后立即关闭, 保证最大隔离性 (MVP 阶段不做池化).
 * Engine 单例复用, 避免重复初始化开销.
 * </p>
 *
 * @author SDDU Build Agent
 */
@Slf4j
@Component
public class GraalJsContextFactory {

    /** 语句执行上限 (防止死循环) */
    private static final long STATEMENT_LIMIT = 10000L;

    /** GraalVM 引擎 (单例复用) */
    private final Engine engine;

    public GraalJsContextFactory() {
        this.engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        log.info("GraalJS engine initialized");
    }

    /**
     * 创建带五层纵深防御的沙箱 Context
     *
     * @return 新创建的 GraalJS Context
     */
    public Context createContext() {
        Context ctx = Context.newBuilder("js")
                .engine(engine)
                // === 五层纵深防御 ===
                .allowIO(false)                       // 1. 禁止文件/网络 IO
                .allowCreateThread(false)             // 2. 禁止创建线程
                .allowNativeAccess(false)             // 3. 禁止原生访问
                 .allowHostAccess(HostAccess.newBuilder(HostAccess.EXPLICIT)
                         .allowMapAccess(true)
                         .allowListAccess(true)
                         .allowBufferAccess(true)
                         .build())
                .allowAllAccess(false)                // 5. 最大限制
                // === 语句限制 ===
                .resourceLimits(ResourceLimits.newBuilder()
                        .statementLimit(STATEMENT_LIMIT, null)
                        .build())
                // === 语言选项 ===
                .option("js.ecmascript-version", "2022")
                .option("js.strict", "true")
                .build();

        log.debug("Created new GraalJS sandbox context");
        return ctx;
    }

    /**
     * 安全关闭 Context
     * <p>
     * 使用 {@code close(true)} 强制取消所有正在执行的脚本,
     * 防止资源泄漏和僵尸线程.
     * </p>
     *
     * @param ctx 要关闭的 Context, 可以为 null
     */
    public void closeContext(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close(true);
                log.debug("GraalJS context closed");
            } catch (Exception e) {
                log.warn("Error closing GraalJS context: {}", e.getMessage());
            }
        }
    }
}
