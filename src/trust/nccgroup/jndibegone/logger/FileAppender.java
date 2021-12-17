package trust.nccgroup.jndibegone.logger;

import java.io.*;

public class FileAppender implements Appender {

  private final File file;

  public FileAppender(File file) {
    this.file = file;
  }

  @Override
  public void appendMessage(String msg) {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new FileWriter(file, true));
      writer.println(msg);
    } catch (IOException e) {
      // Just ignore as there is anything we can do about it.
    } finally {
      closeQuietly(writer);
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
