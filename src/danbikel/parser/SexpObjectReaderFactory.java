package danbikel.parser;

import danbikel.switchboard.*;
import java.io.*;

/**
 * The default factory used to construct <code>ObjectReader</code> objects
 * by the <code>Switchboard</code> class.  This class returns
 * <code>SexpObjectReader</code> objects.
 */
public class SexpObjectReaderFactory
  implements ObjectReaderFactory {

  /** Constructs a new object reader factory for reading <code>Sexp</code>
      objects from a text file. */
  public SexpObjectReaderFactory() {
  }

  public ObjectReader get(InputStream in) throws IOException {
    return new SexpObjectReader(in);
  }

  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpObjectReader(in, encoding, bufSize);
  }

  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpObjectReader(filename, encoding, bufSize);
  }
}
