package trust.nccgroup.jndibegone;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
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
    final String fqn = target.getCanonicalName();

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

  private static boolean isMatchBySignature(TypeDescription target) {
    // TODO: implement it
    throw new UnsupportedOperationException("not implemented");
  }
}
