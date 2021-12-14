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


**Note**: _log4j-jndi-be-gone_ does not look at the original Log4J `JndiLookup` class name.
Instead it utilizes [Classgraph](https://github.com/classgraph/classgraph) to find the culprit classes by
looking at their internal structure. In particular, it searches for classes that contain a final static string field
`static final String CONTAINER_JNDI_RESOURCE_PATH_PREFIX` and a `public String lookup(?, String)` method, that both present
in the `JndiLookup` class. That enables support for obfuscated or shaded _Log4j_ libraries (e.g. fat-jars).

# Usage

Add `-javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar` to your `java` commands.

***Note:*** If you already have Byte Buddy in the classpath, try using
`log4j-jndi-be-gone-1.0.0.jar`.

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar -jar path/to/some.jar
```

You can also enable logging to have a list of intercepted classes.

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar=logDir=/tmp/my/logs/ ...
```

# Obtaining log4j-jndi-be-gone

You can build the JAR with `./gradlew` (`build/libs/log4j-jndi-be-gone-1.0.0(-standalone).jar`)
or get it from the [releases page](https://github.com/nccgroup/log4j-jndi-be-gone/releases).

The output JAR file is located in the `$PROJECT_HOME/build/libs/` directory.

# Compatibility

The log4j-jndi-be-gone agent JAR supports Java 6-17+.

# Caveats

* log4j-jndi-be-gone might not work if the log4j library has been heavily obfuscated.


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

