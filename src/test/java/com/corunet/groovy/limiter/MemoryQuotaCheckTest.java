package com.corunet.groovy.limiter;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

public class MemoryQuotaCheckTest {

    private Random random = new Random();

    @Test
    void testCheckExceeded() {
        AtomicReference<MemoryQuotaCheck> result = new AtomicReference<>();
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean()
        );
        memoryQuotaCheck.setThreadId(Thread.currentThread().getId());
        memoryQuotaCheck.setHandler(result::set);
        memoryQuotaCheck.setLimit(1024 * 1024 * 64L);
        memoryQuotaCheck.recordBaseUsage();
        memoryQuotaCheck.check();
        assertNull(result.get(), "Quota exceeded before any allocations");
        final byte[] aux = new byte[1024 * 1024 * 64];
        random.nextBytes(aux);
        memoryQuotaCheck.check();
        assertNotNull(result.get(), "Quota not exceeded after allocations");
    }

    @Test
    void testCheckNotExceeded() {
        AtomicReference<MemoryQuotaCheck> result = new AtomicReference<>();
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        memoryQuotaCheck.setThreadId(Thread.currentThread().getId());
        memoryQuotaCheck.setHandler(result::set);
        memoryQuotaCheck.setLimit(1024 * 1024 * 65L);
        memoryQuotaCheck.recordBaseUsage();
        memoryQuotaCheck.check();
        assertNull(result.get(), "Quota exceeded before any allocations");
        final byte[] aux = new byte[1024 * 1024 * 64];
        random.nextBytes(aux);
        memoryQuotaCheck.check();
        assertNull(result.get(), "Quota exceeded after allocations");
    }

    @Test
    void testInit() {
        AtomicLong result = new AtomicLong();
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        long threadId = Thread.currentThread().getId();
        memoryQuotaCheck.init();
        assertEquals(threadId, memoryQuotaCheck.getThreadId(), "Unexpected thread ID after init");
        assertNotEquals(0, memoryQuotaCheck.getBaseUsage(), "Unexpected base usage after init");
    }

    @Test
    void testSetLimitBadLimit() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        assertThrows(IllegalArgumentException.class, () -> memoryQuotaCheck.setLimit(-1));
    }

    @Test
    void testSetBaseUsageBadBaseUsage() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        assertThrows(IllegalArgumentException.class, () -> memoryQuotaCheck.setBaseUsage(-1));
    }

    @Test
    void testGetBaseUsage() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        memoryQuotaCheck.setBaseUsage(1337L);
        assertEquals(1337L, memoryQuotaCheck.getBaseUsage());
    }

    @Test
    void testGetLimit() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        memoryQuotaCheck.setLimit(1337L);
        assertEquals(1337L, memoryQuotaCheck.getLimit());
    }

    @Test
    void testGetHandler() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        memoryQuotaCheck.setHandler(mqc -> fail());
        assertNotNull(memoryQuotaCheck.getHandler());
    }

    public static class TestHandler {
        private void handle(MemoryQuotaCheck memoryQuotaCheck) {
            fail();
        }
    }

    @Test
    void testSetHandlerPrivateMethod() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        assertThrows(NoSuchMethodException.class, () -> memoryQuotaCheck.setHandler(TestHandler.class, "handle"));
    }

    @Test
    void testSetHandlerNoSuchMethod() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        assertThrows(NoSuchMethodException.class, () -> memoryQuotaCheck.setHandler(TestHandler.class, "hondel"));
    }

    @Test
    void testCheckThreadUnset() {
        final MemoryQuotaCheck memoryQuotaCheck = new MemoryQuotaCheck(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        assertThrows(IllegalStateException.class, memoryQuotaCheck::check);
    }
}
