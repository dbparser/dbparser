package danbikel.switchboard;

import java.io.*;

/**
 * The default factory used to construct <code>ObjectReader</code> objects
 * by the <code>Switchboard</code> class.  This class returns
 * <code>DefaultObjectReader</code> objects.
 */
class DefaultObjectReaderFactory
  implements ObjectReaderFactory {

  /** Constructs a new object reader factory. */
  public DefaultObjectReaderFactory() {
  }

  public ObjectReader get(InputStream in) throws IOException {
    return new DefaultObjectReader(in);
  }

  public ObjectReader get(InputStream in, String encoding, int bufSize)
    throws IOException {
    return new DefaultObjectReader(in, encoding, bufSize);
  }

  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new DefaultObjectReader(filename, encoding, bufSize);
  }
}
