package com.corunet.groovy.limiter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import javax.validation.constraints.NotNull;

import com.sun.management.ThreadMXBean;

/**
 * Watchdog for memory use of a Groovy script.
 *
 * It stores a base memory usage amount that will not be taken into account when calculating current memory usage.
 */
public class MemoryQuotaCheck {

    public static final String CHECKER_FIELD = "$$memoryQuotaCheck";
    /* ThreadMXBean used to enforce memory quota */
    private final ThreadMXBean threadMXBean;
    /* Thread ID whose memory usage will be checked */
    private long threadId;
    /* Initial thread memory consumption before script execution */
    private long baseUsage = 0L;
    /* Desired memory limit */
    private long limit = 0L;
    /* Maximum registered memory */
    private long maximum = 0L;
    /* Infringement handler */
    private Consumer<MemoryQuotaCheck> handler;

    /* Average memory consumption */
    private long average = 0L;
    /* Check count */
    private long checks = 0L;

    /**
     * Creates a MemoryCheck that uses the given ThreadMXBean to watch a given thread's memory consumption
     *
     * @param threadMXBean {@link ThreadMXBean} that will be used to measure thread memory allocation
     */
    public MemoryQuotaCheck(@NotNull ThreadMXBean threadMXBean) {
        this.threadMXBean = threadMXBean;
    }

    /**
     * Utility method to make a handler from an static method referenced by class and method name
     *
     * @param clazz the class that holds the method
     * @param methodName the name of the method
     * @return a LongConsumer that can be used as a handler
     */
    private static Consumer<MemoryQuotaCheck> methodToConsumer(Class<?> clazz, String methodName)
        throws NoSuchMethodException {
        Method handler = clazz.getMethod(methodName, MemoryQuotaCheck.class);
        return value -> {
            try {
                // This is a static method call, the first parameter should
                // be the instance reference so we leave it null
                handler.invoke(null, value);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                // If an exception occurs during method evaluation
                // then throw it as is.
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                }
                if (e.getTargetException() instanceof Error) {
                    throw (Error) e.getTargetException();
                }
                // Unless it's a checked one. This shouldn't happen ever
                // as Consumer<> doesn't support checked exceptions.
                throw new AssertionError(" threw a checked exception", e);
            }
        };
    }

    /**
     * @return the current handler used in case of quota infringement
     */
    public Consumer<MemoryQuotaCheck> getHandler() {
        return handler;
    }

    /**
     * Allows setting a handler that will be executed in case of quota infringement. This should be a {@link
     * Consumer} accepting a {@link MemoryQuotaCheck} that will receive this instance.
     *
     * In order to stop the script, the handler can throw any {@link RuntimeException} or {@link Error}. If this is not
     * catched by Groovy itself, it will bubble up to the toplevel and immediately stop script execution.
     *
     * @param handler the method used to handle memory quota infringements
     */
    public void setHandler(Consumer<MemoryQuotaCheck> handler) {
        this.handler = handler;
    }

    /**
     * Allows setting a handler that will be executed in case of quota infringement. This should be a {@link
     * Consumer} accepting a {@link MemoryQuotaCheck} that will receive this instance.
     *
     * This implementation accepts such a method as a Class + method name reference.
     *
     * In order to stop the script, the handler can throw any {@link RuntimeException} or {@link Error}. If this is not
     * catched by Groovy itself, it will bubble up to the toplevel and immediately stop script execution.
     *
     * @param clazz the class holding the static method to handle memory quota infringements
     * @param method the name of the method to be called
     * @throws NoSuchMethodException if the method doesn't exist
     */
    public void setHandler(Class<?> clazz, String method) throws NoSuchMethodException {
        this.setHandler(methodToConsumer(clazz, method));
    }

    /**
     * @return the watched thread's ID
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Define the thread's id
     *
     * @param threadId thread id whose memory comsumption will be checked
     */
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * Defines this thread's base memory usage
     *
     * @return thread's base memory usage in bytes
     */
    public long getBaseUsage() {
        return baseUsage;
    }

    /**
     * Defines the thread's base memory usage, this will fail if the provided value is not positive. The base usage will
     * be discounted when checking the quota
     *
     * @param baseUsage allocation limit in bytes
     * @throws IllegalArgumentException when the provided number is not positive
     */
    public void setBaseUsage(long baseUsage) {
        if (baseUsage < 0) {
            throw new IllegalArgumentException("Base memory usage should be a positive number but it was " + baseUsage);
        }
        this.baseUsage = baseUsage;
    }

    /**
     * Stores current memory usage to base memory usage as reported by {@link ThreadMXBean#getThreadAllocatedBytes(long
     * threadId)} for the thread that this MemoryQuotaCheck watches.
     */
    public void recordBaseUsage() {
        this.baseUsage = this.threadMXBean.getThreadAllocatedBytes(this.threadId);
    }

    /**
     * Gets the current memory usage limit defined for this checker
     *
     * @return memory usage limit in bytes
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Defines the memory quota, this will fail if the provided value is not positive
     *
     * @param limit allocation limit in bytes
     * @throws IllegalArgumentException when the provided number is not positive
     */
    public void setLimit(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Memory usage limit should be a positive number but it was " + limit);
        }
        this.limit = limit;
    }

    /**
     * Returns the maximum amount of memory allocated to this thread registered by a call to check
     *
     * @return maximum amount of memory in bytes
     */
    public long getMaximum() {
        return maximum;
    }

    /**
     * @return the average memory measured on all checks performed by this checker.
     */
    public long getAverage() {
        return average;
    }

    /**
     * @return the count of checks performed by this checker
     */
    public long getChecks() {
        return checks;
    }

    private void updateStats(long current) {
        if (current > maximum) {
            maximum = current;
        }

        if (checks == 0L) {
            average = current;
        }

        checks += 1;
        average = (average * (checks - 1) + current) / checks;
    }

    /**
     * Check the thread's memory usage, executes infringement handler if defined.
     */
    void check() {
        if (threadId == 0L) {
            throw new IllegalStateException("Invalid thread id for memory quota check");
        }
        final long current = threadMXBean.getThreadAllocatedBytes(threadId) - baseUsage;

        updateStats(current);

        if (handler != null && (current) > limit) {
            handler.accept(this);
        }
    }

    /**
     * This is a convenience method to set this MemoryQuotaChecker's thrad id to the current threads id and base memory
     * usage to the current thread's memory usage
     */
    void init() {
        threadId = Thread.currentThread().getId();
        recordBaseUsage();
    }
}
