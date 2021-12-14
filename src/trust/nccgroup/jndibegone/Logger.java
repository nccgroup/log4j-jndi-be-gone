package trust.nccgroup.jndibegone;

import java.io.*;
import java.lang.management.ManagementFactory;

public class Logger {

  private final File logFile;

  public Logger(String logDirPath) {
    if (logDirPath == null) {
      this.logFile = null;
    } else {
      final File dir = new File(logDirPath);
      final boolean dirReady = (dir.exists() || dir.mkdirs()) && dir.canWrite();

      File logFile = null;
      if (dirReady) {
        try {
          final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
          logFile = File.createTempFile("log4j_jndi_be_gone." + jvmName + ".", ".log", dir);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      this.logFile = logFile;
    }
  }

  public void log(String msg) {
    System.err.println("[LOG4J_JNDI_BE_GONE] " + msg);

    if (logFile != null) {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(new FileWriter(logFile, true));
        writer.println(msg);
      } catch (IOException e) {
        // Just ignore as there is anything we can do about it.
      } finally {
        closeQuietly(writer);
      }
    }
  }

  private static void closeQuietly(Writer writer) {
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }
}
