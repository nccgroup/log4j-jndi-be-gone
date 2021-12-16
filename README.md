# log4j-jndi-be-gone

A [Byte Buddy](https://bytebuddy.net/) Java agent-based fix for CVE-2021-44228,
the log4j 2.x "JNDI LDAP" vulnerability.

It does three things:

* Disables the internal method handler for `jndi:` format strings ("lookups").
* Logs a message to `System.err` (i.e stderr) indicating that a log4j JNDI
  attempt has been made (including the format string attempted, with any `${}`
  characters sanitized to prevent transitive injections).
* Resolves the format string to `"(log4j jndi disabled)"` in the log message
  (to prevent transitive injections).

### `JndiLookup` class detection algorithm
1. The class names matching the **classPattern** are considered a match and their `lookup` methods are blocked.
2. Otherwise, if the **classSigDetection** option is enabled the agent blocks the classes that meet all following criteria:
* The simple class name is `JndiLookup`, and
* The class is annotated with `@Plugin(name = "jndi", category = "Lookup")`, and
* The class has a field `static final String CONTAINER_JNDI_RESOURCE_PATH_PREFIX`, and
* The class has a public method `lookup` that returns a string and takes two parameters, the 1st one of a type with a simple name `LogEvent`
  and the 2nd one is of type string.
3. If the **classSigDetection** is set to _LOG_ONLY_, then the classes matched by signature aren't blocked, but the fact of match is logged.
4. The classes matching **excludeClassPattern** are never blocked. This could be useful, for example, if **classSigDetection** option is enabled,
   but there is a known benign class in some library whose name and the signature accidentally match the `JndiLookup` class signature described above,
   and we don't want that class to be blocked.


# Usage

Add `-javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar` to your `java` commands.

***Note:*** If you already have Byte Buddy in the classpath, try using
`log4j-jndi-be-gone-1.0.0.jar`.

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar -jar path/to/some.jar
```

The agent could also be enabled for all Java processes system-wide. For example, by replacing the `java` binary with the following script: 

```shell
#!/bin/bash 
$(dirname "$0")/java.original "--javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar" "$@"
```

all new Java processes will automatically be guarded by the agent.  

### Shaded JARs

Detection of *shaded log4j* is disabled by default, but it can be enabled like this:

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar=classSigDetection=ENABLED ...
```

The detection is done by using combination of several anchors (see above) 
that are present in all versions of `JndiLookup` class affected by CVE-2021-44228 (2.0-beta9 to 2.14.1)


### Logging

**Note**: the agent **do not** use log4j for logging.

By default, the logs are written to the standard error stream.

The logs include the following information:
- Intercepted class names
- Classes that look like a shaded `JndiLookup`
- Events of blocked `lookup` method calls

The logs can optionally be stored in files in a specified directory. 

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar=logDir=/tmp/my/logs/ ...
```

The files are created per jvm process, and contain the JVM PID in the name.
It could be useful to find a corresponding Java application if the agent is installed system-wide.

# Configuration

The agent can take the following options:

|Option Name| Type                      | Default Value                                               | Description                                                                                                                  |
|---|---------------------------|-------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
|**logToStdErr**        | boolean                   | TRUE                                                        | If logs should be written to stderr                                                                                          |
|**logDir**             | path                      | _(empty)_                                                   | If specified, the logs will also be written to the given directory. If empty, no logs are written to the disk.               |
|**classPattern**       | regular expression        | org\\.apache\\.logging\\.log4j\\.core\\.lookup\\.JndiLookup | Fully qualified class names to block                                                                                         |
|**excludeClassPattern**| regular expression        | _(empty)_                                                   | Fully qualified class names that should never be blocked                                                                     |
|**classSigDetection**  | ENABLED DISABLED LOG_ONLY | DISABLED                                                    | Should the `JndiLookup` detection be attempted by examining the class signature **(required for shaded JARs support)** |
| |                           |                                                             |
# Obtaining log4j-jndi-be-gone

You can build the JAR with `./gradlew` (`build/libs/log4j-jndi-be-gone-1.0.0(-standalone).jar`)
or get it from the [releases page](https://github.com/nccgroup/log4j-jndi-be-gone/releases).

The output JAR file is located in the `$PROJECT_HOME/build/libs/` directory.

# Compatibility

The log4j-jndi-be-gone agent JAR supports Java 6-17+.

# Caveats

* log4j-jndi-be-gone might not work if the log4j library has been obfuscated.


* It might block calls to a benign `lookup` method if the declaring class internal 
  structure happens to resemble the `JndiLookup` class. (See above).


* `log4j-jndi-be-gone-1.0.0-standalone.jar` bundles in Byte Buddy. If you
  already use Byte Buddy, you may run into issues with it. Try
  `log4j-jndi-be-gone-1.0.0.jar` instead, though note that log4j-jndi-be-gone
  expects Byte Buddy 1.12.x.

# Example

The `tests/jnditest` directory has a simple test case where a log4j logging
call passes in a JNDI LDAP format string. It also sets up its own port
listener to determine if a connection attempt was made by log4j and fails
the test if a connection was received.

```
$ ./tests/jnditest/test-uninstrumented.sh

BUILD SUCCESSFUL in 1s
6 actionable tasks: 5 executed, 1 up-to-date

BUILD SUCCESSFUL in 1s
3 actionable tasks: 3 up-to-date
JUnit version 4.12
.16:08:49.547 [main] ERROR trust.nccgroup.jnditest.test.JndiTest - Hello, _${jndi:ldap://127.0.0.1:8899/evil}_!
E
Time: 0.929
There was 1 failure:
1) logging(trust.nccgroup.jnditest.test.JndiTest)
java.lang.AssertionError: jndi ldap connection received
	at org.junit.Assert.fail(Assert.java:88)
	at trust.nccgroup.jnditest.test.JndiTest.logging(JndiTest.java:55)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runners.Suite.runChild(Suite.java:128)
	at org.junit.runners.Suite.runChild(Suite.java:27)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runners.Suite.runChild(Suite.java:128)
	at org.junit.runners.Suite.runChild(Suite.java:27)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
	at org.junit.runner.JUnitCore.runMain(JUnitCore.java:77)
	at org.junit.runner.JUnitCore.main(JUnitCore.java:36)
	at trust.nccgroup.jnditest.Main.main(Main.java:24)

FAILURES!!!
Tests run: 1,  Failures: 1

$ ./tests/jnditest/test-instrumented.sh

BUILD SUCCESSFUL in 1s
6 actionable tasks: 5 executed, 1 up-to-date

BUILD SUCCESSFUL in 1s
3 actionable tasks: 3 up-to-date
JUnit version 4.12
.log4j jndi lookup attempted: (sanitized) ldap://127.0.0.1:8899/evil
16:09:06.064 [main] ERROR trust.nccgroup.jnditest.test.JndiTest - Hello, _(log4j jndi disabled)_!

Time: 1.362

OK (1 test)
```

# License

Licensed under the Apache 2 license.
