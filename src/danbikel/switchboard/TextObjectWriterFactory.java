package danbikel.switchboard;

import java.io.*;

/**
 * A factory for returning <code>TextObjectWriter</code> objects.
 * This factory is provided as a convenient output mechanism to
 * complement custom <code>ObjectReader</code> factories that are
 * using character-based input.
 *
 * @see TextObjectWriter */
public class TextObjectWriterFactory implements ObjectWriterFactory {

  /** Constructs a new <code>TextObjectWriterFactory</code>. */
  public TextObjectWriterFactory() {
  }

  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new TextObjectWriter(os);
  }

  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new TextObjectWriter(os, encoding, bufSize);
  }

  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    return new TextObjectWriter(filename, encoding, bufSize, append);
  }
}
