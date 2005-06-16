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

  /**
   * Constructs a new instance, reading S-expressions from the
   * specified input stream using the default character encoding.
   * @param in the input stream from which to read S-expressions
   */
  public SexpObjectReader(InputStream in) {
    tok = new SexpTokenizer(new InputStreamReader(in));
  }

  /**
   * Constructs a new object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   * @param in the input stream from which to read S-expressions
   * @param encoding the character encoding to use when reading from the
   * specified file
   * @param bufSize the buffer size to use when reading from the specified
   * file
   *
   * @see SexpTokenizer#SexpTokenizer(InputStream,String,int)
   */
  public SexpObjectReader(InputStream in, String encoding, int bufSize)
    throws IOException {
    tok = new SexpTokenizer(in, encoding, bufSize);
  }

  /**
   * Constructs a new object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   * @param filename the filename from which to read S-expressions
   * @param encoding the character encoding to use when reading from the
   * specified file
   * @param bufSize the buffer size to use when reading from the specified
   * file
   *
   * @see SexpTokenizer#SexpTokenizer(String,String,int)
   */
  public SexpObjectReader(String filename, String encoding,
			  int bufSize)
    throws IOException {
    tok = new SexpTokenizer(filename, encoding, bufSize);
  }

  /**
   * Reads the next S-expression from the underlying S-expression reader.
   * @return the next S-expression from the underlying S-expression reader
   * @throws IOException
   */
  public Object readObject() throws IOException {
    return Sexp.read(tok);
  }

  /**
   * Closes the underlying stream for this reader.
   * @throws IOException if there is a problem closing the underlying stream
   * for this reader
   */
  public void close() throws IOException {
    tok.close();
  }
}
