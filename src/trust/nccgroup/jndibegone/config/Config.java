package trust.nccgroup.jndibegone.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Config {
  public static final String OPTION_LOG_DIR = "logDir";
  public static final String OPTION_LOG_TO_STDERR = "logToStdErr";
  public static final String OPTION_INCLUDE_CLASS_PATTERN = "classPattern";
  public static final String OPTION_EXCLUDE_CLASS_PATTERN = "excludeClassPattern";
  public static final String OPTION_CLASS_SIGNATURE_MODE = "classSigDetection";

  public static final String DEFAULT_INCLUDE_CLASS_PATTERN = "org\\.apache\\.logging\\.log4j\\.core\\.lookup\\.JndiLookup";
  public static final String DEFAULT_EXCLUDE_CLASS_PATTERN = null;
  public static final String DEFAULT_LOG_TO_STDERR = Boolean.TRUE.toString();
  public static final ClassSigMode DEFAULT_CLASS_SIG_MODE = ClassSigMode.DISABLED;

  public final String logDir;
  public final boolean logToStdErr;
  public final Pattern excludeClassPattern;
  public final Pattern includeClassPattern;
  public final ClassSigMode classSigMode;

  private Config(
    String logDir,
    boolean logToStdErr,
    Pattern includeClassPattern,
    Pattern excludeClassPattern,
    ClassSigMode classSigMode
  ) {
    this.logDir = logDir;
    this.logToStdErr = logToStdErr;
    this.includeClassPattern = includeClassPattern;
    this.excludeClassPattern = excludeClassPattern;
    this.classSigMode = classSigMode;
  }

  public static Config parse(String args) {
    final Map<String, String> options = new HashMap<String, String>();
    if (args != null) {
      for (String arg : args.split(",")) {
        final int i = arg.indexOf("=");
        final String key = i < 0 ? arg : arg.substring(0, i).trim();
        final String value = i < 0 ? null : arg.substring(i + 1).trim();
        options.put(key, value);
      }
    }

    final String logDir = trimToNull(options.get(OPTION_LOG_DIR));
    final boolean logToStdErr = Boolean.parseBoolean(getOrDefault(options.get(OPTION_LOG_TO_STDERR), DEFAULT_LOG_TO_STDERR));

    final String csModeStr = trimToNull(options.get(OPTION_CLASS_SIGNATURE_MODE));
    final ClassSigMode csMode = csModeStr == null ? DEFAULT_CLASS_SIG_MODE : ClassSigMode.valueOf(csModeStr.toUpperCase());

    final Pattern inclPattern = compilePattern(getOrDefault(trimToNull(options.get(OPTION_INCLUDE_CLASS_PATTERN)), DEFAULT_INCLUDE_CLASS_PATTERN));
    final Pattern exclPattern = compilePattern(getOrDefault(trimToNull(options.get(OPTION_EXCLUDE_CLASS_PATTERN)), DEFAULT_EXCLUDE_CLASS_PATTERN));

    return new Config(
      logDir,
      logToStdErr,
      inclPattern,
      exclPattern,
      csMode
    );
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    } else {
      final String trimmed = s.trim();
      return trimmed.isEmpty() ? null : trimmed;
    }
  }

  private static <T> T getOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static Pattern compilePattern(String pattern) {
    return pattern == null ? null : Pattern.compile(pattern);
  }
}
