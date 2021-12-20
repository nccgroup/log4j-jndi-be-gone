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

# Usage

Add `-javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar` to your `java` commands.

***Note:*** If you already have Byte Buddy in the classpath, try using
`log4j-jndi-be-gone-1.0.0.jar`.

```
$ java -javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar -jar path/to/some.jar
```

As of version 1.1.0, log4j-jndi-be-gone defaults to attempting to handle
repackaged (aka "shaded") versions of log4j that may be embedded in a JAR under
an alternate package name to prevent collisions between an application's
version of a depdencency and a depdendency's version of the same depdendency.

This can be disabled by placing `=structureMatch=0` after the agent JAR path
in the `-javaagent:` argument, e.g.:

```
-javaagent:path/to/log4j-jndi-be-gone-1.0.0-standalone.jar=structureMatch=0
```

This will result in the same matching behavior as 1.0.0, a simple exact string
comparison against the class name.

# Obtaining log4j-jndi-be-gone

You can build the JAR with `./gradlew` (`build/libs/log4j-jndi-be-gone-1.0.0(-standalone).jar`)
or get it from the [releases page](https://github.com/nccgroup/log4j-jndi-be-gone/releases).

# Compatibility

The log4j-jndi-be-gone agent JAR supports Java 6-17+.

## Class Matching

The implementation starts by matching against classes with suffixes that match
the innermost sub-packages and classname of
`org.apache.logging.log4j.core.lookup.JndiLookup`,
`lookup.JndiLookup`, as `org.apache.logging.log4j.core` can reasonably be
expected to have been mangled by repackaging rules that did not seek to
preserve package names. Additionally, instead of just performing similar checks
for all other expected log4j types, it ensures that they exist under the same
base package as well.

The implementation then walks through the structure of any identified potential
log4j `lookup.JndiLookup` classes, attempting to validate against:

* the modifiers on the class itself
* the class' parent class and/or implemented interfaces (these differ between
  log4j versions)
* the `org.apache.logging.log4j.core.config.plugins.Plugin` annotation expected
  across all 2.x versions, including annotation parameters and their values
* the method `lookup()`, matching against its modifiers and type signature
  (and ignoring the 1-argument version from 2.0)
* the method `convertJndiName()`, matching against its modifiers and type signature
* the field `CONTAINER_JNDI_RESOURCE_PATH_PREFIX`, matching against its modifiers

# Caveats

* log4j-jndi-be-gone will not work if the log4j library has been obfuscated or
if its class packages/names have been modified other than basic re-packaging
(i.e. "shading").
    * FWIW, log4j 2.x is pretty inflexible with regards to being repackaged, so
      it's unclear how common such practices are.

* `log4j-jndi-be-gone-1.0.0-standalone.jar` bundles in Byte Buddy. If you
  already use Byte Buddy, you may run into issues with it. Try
  `log4j-jndi-be-gone-1.0.0.jar` instead, though note that log4j-jndi-be-gone
  expects Byte Buddy 1.12.x.

* If you have replaced your `JndiLookup` classes with implementations that
  attempt to do honeypotting or log `lookup()` calls, log4j-jndi-be-gone will
  potentially disable their `lookup` method, preventing them from working.

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

# Log4j Versions Tested

* 2.0
* 2.0.1
* 2.0.2
* 2.1
* 2.2
* 2.3
* 2.4
* 2.4.1
* 2.5
* 2.6
* 2.6.1
* 2.6.2
* 2.7
* 2.8
* 2.8.1
* 2.8.2
* 2.9.0
* 2.9.1
* 2.10.0
* 2.11.0
* 2.11.1
* 2.11.2
* 2.12.0
* 2.12.1
* 2.12.2
* 2.13.0
* 2.13.1
* 2.13.2
* 2.13.3
* 2.14.0
* 2.14.1
* 2.15.0
* 2.16.0
* 2.17.0


