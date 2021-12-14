package trust.nccgroup.jndibegone;

import io.github.classgraph.*;

import java.util.HashSet;
import java.util.Set;

public class JndiLookupClassFinder {

  private static final String MARKER_FIELD_NAME = "CONTAINER_JNDI_RESOURCE_PATH_PREFIX";
  private static final String MARKER_METHOD_NAME = "lookup";
  private static final String STRING_TYPE = "java.lang.String";

  private final Logger logger;

  public JndiLookupClassFinder(Logger logger) {
    this.logger = logger;
  }

  public Set<String> findJndiLookupClassNames() {
    final ScanResult scanResult =
      new ClassGraph()
        .ignoreFieldVisibility()
        .enableMethodInfo()
        .enableClassInfo()
        .scan();

    try {
      final ClassInfoList JndiLookupIshClassInfos =
        scanResult
          .getAllStandardClasses()
          .filter(new ClassInfoList.ClassInfoFilter() {
            public boolean accept(ClassInfo ci) {
              return isJndiLookupIshClass(ci);
            }
          });

      final Set<String> resultClassNameSet = new HashSet<String>();
      for (ClassInfo ci : JndiLookupIshClassInfos) {
        final String className = ci.getName();
        resultClassNameSet.add(className);
        logger.log("Detected log4j JndiLookup class: " + className);
      }
      return resultClassNameSet;

    } finally {
      scanResult.close();
    }
  }

  private static boolean isJndiLookupIshClass(ClassInfo ci) {
    return !(ci.isSynthetic()
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
}
