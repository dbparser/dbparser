package danbikel.parser;

import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.io.*;

/**
 * Reads an underlying stream with a <code>SexpTokenizer</code>,
 * reading each S-expression as a object and returning it when
 * {@link #readObject()} is invoked.
 */
public class SexpObjectReader implements ObjectReader {
  private SexpTokenizer tok;

  public SexpObjectReader(InputStream in) {
    tok = new SexpTokenizer(new InputStreamReader(in));
  }

  public SexpObjectReader(InputStream in, String encoding, int bufSize)
    throws IOException {
    tok = new SexpTokenizer(in, encoding, bufSize);
  }

  /**
   * Constructs a new object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   *
   * @see SexpTokenizer#SexpTokenizer(String,String,int)
   */
  public SexpObjectReader(String filename, String encoding,
			  int bufSize)
    throws IOException {
    tok = new SexpTokenizer(filename, encoding, bufSize);
  }

  public Object readObject() throws IOException {
    return Sexp.read(tok);
  }

  public void close() throws IOException {
    tok.close();
  }
}
