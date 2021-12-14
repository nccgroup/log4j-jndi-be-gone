package trust.nccgroup.jndibegone;

import io.github.classgraph.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JndiLookupClassFinder {

  private static final String DEFAULT_JNDI_LOOKUP_CLASS_NAME = "org.apache.logging.log4j.core.lookup.JndiLookup";
  private static final String MARKER_FIELD_NAME = "CONTAINER_JNDI_RESOURCE_PATH_PREFIX";
  private static final String MARKER_METHOD_NAME = "lookup";
  private static final String STRING_TYPE = "java.lang.String";

  public static Set<String> findJndiLookupClassNames() {
    final ClassGraph classGraph = new ClassGraph()
      .ignoreFieldVisibility()
      .enableMethodInfo()
      .enableClassInfo();
    final ScanResult scanResult = classGraph.scan();
    try {
      final List<Class<?>> culpritClasses =
        scanResult
          .getAllClasses()
          .filter(new ClassInfoList.ClassInfoFilter() {
            public boolean accept(ClassInfo ci) {
              return isJndiLookupClass(ci);
            }
          })
          .loadClasses(true);
      final Set<String> resultClassNameSet = new HashSet<String>();
      for (Class<?> c : culpritClasses) {
        final String className = c.getCanonicalName();
        resultClassNameSet.add(className);
        logInfo("Found Log4j2 JndiLookup class: " + className);
        if (!DEFAULT_JNDI_LOOKUP_CLASS_NAME.equals(className)) {
          logWarn("Found a class that looks like a renamed Log4j2 JndiLookup: " + className);
        }
      }
      return resultClassNameSet;
    } finally {
      scanResult.close();
    }
  }

  private static boolean isJndiLookupClass(ClassInfo ci) {
    return ci.isStandardClass()
      && !(ci.isSynthetic()
      || ci.isAbstract()
      || ci.isInnerClass()
      || ci.isArrayClass()
      || ci.isEnum()
    ) && isClassSignatureMatch(ci);
  }

  private static boolean isClassSignatureMatch(ClassInfo ci) {
    return isFieldSignatureMatch(ci.getDeclaredFieldInfo(MARKER_FIELD_NAME))
      && isMethodSignatureMatch(ci.getDeclaredMethodInfo(MARKER_METHOD_NAME));
  }

  private static boolean isFieldSignatureMatch(FieldInfo fi) {
    return fi != null
      && STRING_TYPE.equals(fi.getTypeDescriptor().toString())
      && fi.isStatic()
      && fi.isFinal();
  }

  private static boolean isMethodSignatureMatch(MethodInfoList methodInfolist) {
    if (methodInfolist.isEmpty()) {
      return false;
    }
    try {
      final MethodInfo mi = methodInfolist.getSingleMethod(MARKER_METHOD_NAME);
      if (mi == null || !STRING_TYPE.equals(mi.getTypeDescriptor().getResultType().toString())) {
        return false;
      }
      final MethodParameterInfo[] parameterInfos = mi.getParameterInfo();
      return parameterInfos.length == 2
        && !STRING_TYPE.equals(parameterInfos[0].getTypeDescriptor().toString())
        && STRING_TYPE.equals(parameterInfos[1].getTypeDescriptor().toString());
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static void logWarn(String msg) {
    doLog("[WARNING] " + msg);
  }

  private static void logInfo(String msg) {
    doLog("[INFO] " + msg);
  }

  private static void doLog(String msg) {
    System.out.println("[LOG4J_JNDI_BE_GONE]" + msg);
    // todo: log to a file
  }

}
