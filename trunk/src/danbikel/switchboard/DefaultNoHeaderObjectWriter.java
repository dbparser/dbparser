package danbikel.switchboard;

import java.io.*;

class DefaultNoHeaderObjectWriter
  extends ObjectOutputStream implements ObjectWriter {

  public DefaultNoHeaderObjectWriter(OutputStream os) throws IOException {
    super(os);
  }

  public DefaultNoHeaderObjectWriter(OutputStream os, int bufSize)
    throws IOException {
    super(new BufferedOutputStream(os, bufSize));
  }

  /**
   * Does nothing (used when appending to a non-empty file).
   */
  protected void writeStreamHeader() throws IOException {
  }
}
