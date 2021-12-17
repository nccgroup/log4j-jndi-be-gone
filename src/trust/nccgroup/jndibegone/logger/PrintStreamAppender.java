package trust.nccgroup.jndibegone.logger;

import java.io.PrintStream;

public class PrintStreamAppender implements Appender {
  private final PrintStream stream;

  public PrintStreamAppender(PrintStream stream) {
    this.stream = stream;
  }

  @Override
  public void appendMessage(String msg) {
    stream.println(msg);
  }
}
