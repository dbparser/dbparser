package danbikel.switchboard;

import java.io.*;

/**
 * Writes objects to an underlying serialization output stream.
 */
class DefaultObjectWriter extends ObjectOutputStream implements ObjectWriter {
  public DefaultObjectWriter(OutputStream os) throws IOException {
    super(os);
  }
  public DefaultObjectWriter(OutputStream os, int bufSize) throws IOException {
    super(new BufferedOutputStream(os, bufSize));
  }
}
