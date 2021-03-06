[![Build Status](https://travis-ci.org/corunet/groovy-memory-limiter.svg?branch=master)](https://travis-ci.org/corunet/groovy-memory-limiter)

# groovy-memory-limiter
Limits memory allocation in a Groovy script by regularly checking allocated memory.
It also offers stats on script memory use.

#### Checking memory
The limiter will regularly check for the amount of memory allocated
by the script's executing thread. If it is exceeded, a callback
will be executed to a method expecting a `MemoryQuotaCheck` instance,
which can be queried to find out more about the infringement or 
used to reconfigure the limit.

If you are not interrupting the script in response to the quota
infringement, you may want to disable further reporting via
`MemoryQuotaCheck.setEnabled()`.

Also, for added flexibility, the script's binding can also be retrieved
via `MemoryQuotaCheck.getScriptBinding()`.

###### From Java
The following code will call the QuotaInfringementHandler#handle method at the beginning of the Groovy `method()`.
```java
public class MemoryLimit {
    public static void main() {}
        Map<String, Object> map = new HashMap<>();
        map.put("limit", 1024 * 1024 * 2); // 1KB
        map.put("handlerClass", QuotaInfringementHandler.class);
        map.put("handlerMethod", "handle");
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(map, CheckMemoryQuota.class));
        GroovyShell groovyShell = new GroovyShell(compilerConfiguration);
        //noinspection GroovyUnusedAssignment
        groovyShell.evaluate(
            "def garbage = new byte[1024 * 1024 * 3]\n"
                + "def method() { /* do nothing */ }\n"
                + "method() \n"
                + "return 5"
        );
    }
}
```

###### From Groovy
Just annotate your script with `@CheckMemoryQuota(limit=bytes, handlerClass=Handler.class, handlerMethod="methodName")`.

#### Recovering stats after execution
Average and peak memory consumption can be recovered from the `MemoryQuotaCheck`
instance after the script finishes execution. Just recover it from the `Script`
object like this.

```
Script script = groovyShell.parse(yourGroovy);

script.run();
MemoryQuotaCheck memoryQuotaCheck =
    (MemoryQuotaCheck) script.getProperty(MemoryQuotaCheck.CHECKER_FIELD);
```

#### Caveats
This uses `com.sun.management.ThreadMXBean` to watch memory allocation of a thread and thus it will only run on JVM
providing such class (ie. Oracle's JVM).

The quota only applies to the thread that first launches the Groovy script. This will not
work on multi-threaded code.

Checks are inserted at the beginning of every loop iteration, closure and method call. If the memory limit is infringed
during a library call or on a long row of assignments, the limiter will not be notified until one of those ocurrs.

#### Credit

This project is a fork of [groovy-memory-limiter](https://github.com/d0k1/groovy-memory-limiter) by Denis Kirpichenkov (d0k1).