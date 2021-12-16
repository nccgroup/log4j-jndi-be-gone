package trust.nccgroup.jndibegone;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import trust.nccgroup.jndibegone.config.ClassSigMode;
import trust.nccgroup.jndibegone.logger.Logger;

import java.util.regex.Pattern;

public class JndiLookupClassMatcher implements ElementMatcher<TypeDescription> {

  private final Pattern excludeClassPattern;
  private final Pattern includeClassPattern;
  private final ClassSigMode classSigMode;
  private final Logger logger;

  public JndiLookupClassMatcher(Pattern excludeClassPattern, Pattern includeClassPattern, ClassSigMode classSigMode, Logger logger) {
    this.excludeClassPattern = excludeClassPattern;
    this.includeClassPattern = includeClassPattern;
    this.classSigMode = classSigMode;
    this.logger = logger;
  }

  public boolean matches(TypeDescription target) {
    if (target == null) return false;

    final String fqn = target.getCanonicalName();
    if (fqn == null) return false;

    if (excludeClassPattern != null && excludeClassPattern.matcher(fqn).matches()) {
      // The given class is benign and has to be skipped.
      return false;
    }

    if (includeClassPattern != null && includeClassPattern.matcher(fqn).matches()) {
      logger.log("Blocked " + fqn);
      return true;
    }

    if (classSigMode == ClassSigMode.DISABLED) {
      // no match found based on class name pattern
      return false;
    }

    // attempt matching by class signature
    if (isMatchBySignature(target)) {
      if (classSigMode == ClassSigMode.LOG_ONLY) {
        logger.log("Possibly relocated 'JndiLookup' (unblocked): " + fqn);
        return false;
      } else {
        logger.log("Possibly relocated 'JndiLookup' (blocked): " + fqn);
        return true;
      }
    }

    // no match found
    return false;
  }

  private static boolean isMatchBySignature(TypeDescription td) {
    return hasJndiLookupName(td) && hasPluginAnnotation(td) && hasJndiField(td) && hasLookupMethod(td);
  }

  private static boolean hasJndiLookupName(TypeDescription target) {
    return "JndiLookup".equals(target.getSimpleName());
  }

  private static boolean hasPluginAnnotation(TypeDescription target) {
    return !target.getDeclaredAnnotations().filter(JndiAnnotationMatcher).isEmpty();
  }

  private static boolean hasJndiField(TypeDescription target) {
    return !target.getDeclaredFields().filter(JndiFieldMatcher).isEmpty();
  }

  private static boolean hasLookupMethod(TypeDescription target) {
    return !target.getDeclaredMethods().filter(JndiMethodMatcher).isEmpty();
  }

  /*
   * matching on the annotation:
   * @Plugin(name = "jndi", category = "Lookup")
   */
  private final static ElementMatcher<AnnotationDescription> JndiAnnotationMatcher = new ElementMatcher<AnnotationDescription>() {
    @Override
    public boolean matches(AnnotationDescription ad) {
      return ad != null &&
        "Plugin".equals(ad.getAnnotationType().getSimpleName()) &&
        "jndi".equals(ad.getValue("name").resolve()) &&
        "Lookup".equals(ad.getValue("category").resolve());
    }
  };

  /*
   * matching on the field:
   * static final String CONTAINER_JNDI_RESOURCE_PATH_PREFIX = "java:comp/env/";
   */
  private final static ElementMatcher<FieldDescription> JndiFieldMatcher = new ElementMatcher<FieldDescription>() {
    @Override
    public boolean matches(FieldDescription fd) {
      return fd != null &&
        "CONTAINER_JNDI_RESOURCE_PATH_PREFIX".equals(fd.getName()) &&
        fd.getType().represents(String.class) &&
        fd.isStatic() &&
        fd.isFinal();
    }
  };

  private final static ElementMatcher<TypeDescription> LogEventTypeMatcher = new ElementMatcher<TypeDescription>() {
    @Override
    public boolean matches(TypeDescription td) {
      return td != null &&
        "LogEvent".equals(td.getSimpleName());
    }
  };

  /*
   * matching on the method:
   * public String lookup(final LogEvent event, final String key)
   */
  private final static ElementMatcher<MethodDescription> JndiMethodMatcher = ElementMatchers
    .named("lookup")
    .and(ElementMatchers.<MethodDescription>isPublic())
    .and(ElementMatchers.returns(String.class))
    .and(ElementMatchers.takesArguments(2))
    .and(ElementMatchers.takesArgument(0, LogEventTypeMatcher))
    .and(ElementMatchers.takesArgument(1, String.class));

}
