package danbikel.parser;

import danbikel.switchboard.*;
import java.io.*;

public class EventCountsWriterFactory implements ObjectWriterFactory {
  private final static int minBufSize = 81920;

  /**
   * Constructs a new <code>EventWriterFactory</code>.
   */
  public EventCountsWriterFactory() {
  }

  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new EventCountsWriter(os);
  }

  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new EventCountsWriter(os, encoding, Math.max(minBufSize, bufSize));
  }

  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    return new EventCountsWriter(filename, encoding,
                                 Math.max(minBufSize, bufSize), append);
  }
}