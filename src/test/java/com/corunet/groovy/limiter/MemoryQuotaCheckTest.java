package com.corunet.groovy.limiter;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
}
