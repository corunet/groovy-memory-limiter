package com.corunet.groovy.limiter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.junit.jupiter.api.Test;

public class CheckMemoryQuotaTest {

    private static final long MEGABYTES_64 = 1024 * 1024 * 64L;
    private static final long MEGABYTES_65 = 1024 * 1024 * 65L;


    private static class QuotaInfringementHandler {

        @SuppressWarnings("unused")
        public static void handle(long memory) {
            throw new OutOfMemoryError("Memory quota exceeded, current memory use " + memory + " bytes");
        }
    }

    @Test
    void testRunScriptWithQuotaCheckInForLoopExceed() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_64);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        assertThrows(OutOfMemoryError.class, () -> groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n" +
                "for(int i=0; i<1; i+=1) { garbage[i] = (byte)i }\n"
                + "return 5"
        ), "Handler didn't return expected result");
    }

    @Test
    void testRunScriptWithQuotaCheckInForLoopNormal() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_65);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        assertEquals(5, (int) groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n" +
                "for(int i=0; i<1; i+=1) { garbage[i] = (byte)i }\n"
                + "return 5"
        ), "Unexpected result on non failing script");
    }

    @Test
    void testRunScriptWithQuotaCheckInWhileLoopExceed() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_64);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        assertThrows(OutOfMemoryError.class, () -> groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n"
                + "i=0\n"
                + "while(i<1) { garbage[i] = (byte)i; i+=1 }\n"
                + "return 5"
        ));
    }

    @Test
    void testRunScriptWithQuotaCheckInWhileLoopNormal() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_65);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        assertEquals(5, (int) groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n"
                + "i=0\n"
                + "while(i<1) { garbage[i] = (byte)i; i+=1 }\n"
                + "return 5"
        ), "Unexpected result on non failing script");
    }

    @Test
    void testRunScriptWithQuotaCheckInMethodCallExceed() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_64);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        //noinspection GrMethodMayBeStatic, GroovyUnusedAssignment
        assertThrows(OutOfMemoryError.class, () -> groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n"
                + "def method() { throw new AssertionError(\"Failure\") }\n"
                + "method() \n"
                + "return 5"
        ));
    }

    @Test
    void testRunScriptWithQuotaCheckInMethodCallNormal() {
        Map<String, Object> map = new HashMap<>();
        map.put("limit", MEGABYTES_65);
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        //noinspection GroovyUnusedAssignment
        assertEquals(5, (int) groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 64]\n"
                + "def method() { /* do nothing */ }\n"
                + "method() \n"
                + "return 5"
        ), "Unexpected result on non failing script");
    }
}