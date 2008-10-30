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

  /**
   * Returns a new {@link SexpObjectReader} constructed with the
   * specified input stream argument.
   *
   * @param in the input stream around which to construct a new
   * {@link SexpObjectReader}
   *
   * @return a new {@link SexpObjectReader} constructed with the
   * specified input stream argument
   */
  public ObjectReader get(InputStream in) throws IOException {
    return new SexpObjectReader(in);
  }

  /**
   * Returns a new {@link SexpObjectReader} constructed with the
   * specified arguments.
   * @param in the input stream around which to construct the returned
   * {@link SexpObjectReader}
   * @param encoding the character encoding to use for reading S-expressions
   * from the specified input stream
   * @param bufSize the buffer size for the S-expression reader in the returned
   * {@link SexpObjectReader} to use
   * @return a new {@link SexpObjectReader} constructed with the
   * specified arguments
   * @throws IOException if there is a problem constructing a new
   * {@link SexpObjectReader} using the specified arguments of
   * this method
   */
  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpObjectReader(in, encoding, bufSize);
  }

  /**
   * Returns a new {@link SexpObjectReader} constructed with the
   * specified arguments.
   * @param filename the name of the file around which to construct the returned
   * {@link SexpObjectReader}
   * @param encoding the character encoding to use for reading S-expressions
   * from the specified input stream
   * @param bufSize the buffer size for the S-expression reader in the returned
   * {@link SexpObjectReader} to use
   * @return a new {@link SexpObjectReader} constructed with the
   * specified arguments
   * @throws IOException if there is a problem constructing a new
   * {@link SexpObjectReader} using the specified arguments of
   * this method
   */
  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpObjectReader(filename, encoding, bufSize);
  }
}
