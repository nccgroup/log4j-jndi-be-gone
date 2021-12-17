package trust.nccgroup.jndibegone.logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class Logger {

  private final List<Appender> appenders = new ArrayList<Appender>();

  public Logger(String logDirPath, boolean logToStdErr) {
    if (logToStdErr) {
      appenders.add(new PrintStreamAppender(System.err));
    }
    if (logDirPath != null) {
      final File logFile = createLogFile(new File(logDirPath));
      final Appender fileAppender = logFile == null ? null : new FileAppender(logFile);
      if (fileAppender != null) {
        appenders.add(fileAppender);
      }
    }
  }

  public void log(String str) {
    final String msg = "[LOG4J_JNDI_BE_GONE] " + str;
    for (Appender appender : appenders) {
      appender.appendMessage(msg);
    }
  }

  private static File createLogFile(File dir) {
    final boolean dirReady = (dir.exists() || dir.mkdirs()) && dir.canWrite();
    if (dirReady) {
      try {
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return File.createTempFile("log4j_jndi_be_gone." + jvmName + ".", ".log", dir);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
