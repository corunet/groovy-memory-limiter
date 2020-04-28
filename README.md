# groovy-memory-limiter
Limits memory allocation in a Groovy script.

#### Caveats
This uses `com.sun.management.ThreadMXBean` to watch memory allocation of a thread and thus it will only run on JVM
providing such class (ie. Oracle's JVM).

#### TODO
* Use isThreadAllocatedMemorySupported to check for plausibility
* Use setThreadAllocatedMemoryEnabled(true) to ensure memory management is enabled
* Improve documentation
* Add mean memory consumption