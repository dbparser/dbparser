package danbikel.parser;

import danbikel.switchboard.*;
import java.io.*;

/**
 * The default <code>NumberedSentenceReaderFactory</code> used by
 * <code>Switchboard</code>.  This class provides new instances of
 * <code>SexpNumberedSentenceReader</code> objects.
 *
 * @see SexpNumberedObjectReader
 */
public class SexpNumberedObjectReaderFactory
  implements ObjectReaderFactory {

  /** Constructs a new factory. */
  public SexpNumberedObjectReaderFactory() {
  }

  public ObjectReader get(InputStream in) throws IOException {
    return new SexpNumberedObjectReader(in);
  }

  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpNumberedObjectReader(in, encoding, bufSize);
  }


  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpNumberedObjectReader(filename, encoding, bufSize);
  }
}
