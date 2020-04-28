package com.corunet.groovy.limiter;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

public class MemoryQuotaCheckerTest {

    private Random random = new Random();

    @Test
    void testCheckExceeded() {
        AtomicLong result = new AtomicLong();
        final MemoryQuotaChecker memoryQuotaChecker = new MemoryQuotaChecker(
            (ThreadMXBean) ManagementFactory.getThreadMXBean()
        );
        memoryQuotaChecker.setThreadId(Thread.currentThread().getId());
        memoryQuotaChecker.setHandler(result::set);
        memoryQuotaChecker.setLimit(1024 * 1024 * 64L);
        memoryQuotaChecker.recordBaseUsage();
        memoryQuotaChecker.check();
        assertEquals(0, result.get(), "Quota exceeded before any allocations");
        final byte[] aux = new byte[1024 * 1024 * 64];
        random.nextBytes(aux);
        memoryQuotaChecker.check();
        assertNotEquals(0, result.get(), "Quota not exceeded after allocations");
    }

    @Test
    void testCheckNotExceeded() {
        AtomicLong result = new AtomicLong();
        final MemoryQuotaChecker memoryQuotaChecker = new MemoryQuotaChecker(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        memoryQuotaChecker.setThreadId(Thread.currentThread().getId());
        memoryQuotaChecker.setHandler(result::set);
        memoryQuotaChecker.setLimit(1024 * 1024 * 65L);
        memoryQuotaChecker.recordBaseUsage();
        memoryQuotaChecker.check();
        assertEquals(0, result.get(), "Quota exceeded before any allocations");
        final byte[] aux = new byte[1024 * 1024 * 64];
        random.nextBytes(aux);
        memoryQuotaChecker.check();
        assertEquals(0, result.get(), "Quota exceeded after allocations");
    }

    @Test
    void testInit() {
        AtomicLong result = new AtomicLong();
        final MemoryQuotaChecker memoryQuotaChecker = new MemoryQuotaChecker(
            (ThreadMXBean) ManagementFactory.getThreadMXBean());
        long threadId = Thread.currentThread().getId();
        memoryQuotaChecker.init();
        assertEquals(threadId, memoryQuotaChecker.getThreadId(), "Unexpected thread ID after init");
        assertNotEquals(0, memoryQuotaChecker.getBaseUsage(), "Unexpected base usage after init");
    }
}
