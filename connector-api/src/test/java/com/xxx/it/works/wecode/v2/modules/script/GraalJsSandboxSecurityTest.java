package com.xxx.it.works.wecode.v2.modules.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GraalJsSandboxSecurity 测试 — 5层安全覆盖")
class GraalJsSandboxSecurityTest {

    private GraalJsContextFactory factory;

    @BeforeEach
    void setUp() {
        factory = new GraalJsContextFactory();
    }

    // ===== Layer 1: IO 访问被拒绝 =====

    @Test
    @DisplayName("Layer 1 — IO 访问被拒绝 (allowIO=false)")
    void testLayer1_IOAccessDenied() {
        Context ctx = factory.createContext();
        try {
            assertThrows(PolyglotException.class, () ->
                    ctx.eval("js", "java.nio.file.Files.readAllLines(java.nio.file.Paths.get('/etc/passwd'))")
            );
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== Layer 2: 线程创建被拒绝 =====

    @Test
    @DisplayName("Layer 2 — 线程创建被拒绝 (allowCreateThread=false)")
    void testLayer2_ThreadCreationDenied() {
        Context ctx = factory.createContext();
        try {
            assertThrows(PolyglotException.class, () ->
                    ctx.eval("js", "new java.lang.Thread(function() {}).start()")
            );
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== Layer 3: 原生访问被拒绝 =====

    @Test
    @DisplayName("Layer 3 — 原生访问被拒绝 (allowNativeAccess=false)")
    void testLayer3_NativeAccessDenied() {
        Context ctx = factory.createContext();
        try {
            // 在 EXPLICIT host access 下，Java.type 不可用
            assertThrows(PolyglotException.class, () ->
                    ctx.eval("js", "java.lang.System.exit(0)")
            );
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== Layer 4: Java.type() 反射被拒绝 =====

    @Test
    @DisplayName("Layer 4 — Java.type() 被拒绝 (HostAccess.EXPLICIT)")
    void testLayer4_JavaTypeReflectionDenied() {
        Context ctx = factory.createContext();
        try {
            assertThrows(PolyglotException.class, () ->
                    ctx.eval("js", "var System = Java.type('java.lang.System'); System.exit(0);")
            );
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== Layer 5: statementLimit 超限终止 =====

    @Test
    @DisplayName("Layer 5 — 语句限制超限终止 (statementLimit=10000)")
    void testLayer5_StatementLimitExceeded() {
        Context ctx = factory.createContext();
        try {
            assertThrows(PolyglotException.class, () ->
                    ctx.eval("js", "while(true) { var x = 1 + 1; }")
            );
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== 正常脚本执行 =====

    @Test
    @DisplayName("正常脚本在沙箱中可执行")
    void testNormalScriptRunsInSandbox() {
        Context ctx = factory.createContext();
        try {
            Object result = ctx.eval("js", "1 + 1").asInt();
            assertEquals(2, result);
        } finally {
            factory.closeContext(ctx);
        }
    }

    // ===== closeContext 安全处理 null =====

    @Test
    @DisplayName("closeContext(null) 不抛异常")
    void testCloseContext_NullSafe() {
        assertDoesNotThrow(() -> factory.closeContext(null));
    }
}
