package danbikel.parser;

import danbikel.util.Text;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.io.*;

/**
 * Reads an underlying stream with a <code>SexpTokenizer</code>,
 * converting S-expressions of the form
 * <tt>(num&nbsp;processed&nbsp;obj)</tt>, where <code>obj</code>
 * is a <code>Sexp</code> and <tt>processed</tt> is a <code>Symbol</code>
 * whose print-name is the output of <code>String.valueOf(boolean)</code>, to
 * NumberedObject objects.
 *
 * @see SexpNumberedObjectReaderFactory
 */
public class SexpNumberedObjectReader implements ObjectReader {
  private SexpTokenizer tok;

  public SexpNumberedObjectReader(InputStream in) {
    tok = new SexpTokenizer(new InputStreamReader(in));
  }

  public SexpNumberedObjectReader(InputStream in, String encoding, int bufSize)
    throws IOException {
    tok = new SexpTokenizer(in, encoding, bufSize);
  }


  /**
   * Constructs a new numbered object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   *
   * @see SexpTokenizer#SexpTokenizer(String,String,int)
   */
  public SexpNumberedObjectReader(String filename, String encoding,
				  int bufSize)
    throws IOException {
    tok = new SexpTokenizer(filename, encoding, bufSize);
  }

  public Object readObject() throws IOException {
    Sexp sent = Sexp.read(tok);
    if (sent == null)
      return null;
    if (sent.isSymbol())
      throw new IOException("Sexp is wrong type for NubmeredObject: Symbol");
    if (sent.list().size() != 3 ||
	sent.list().get(0).isList() || sent.list().get(1).isList())
      throw new IOException("Sexp has wrong format for Object");
    if (Text.isAllDigits(sent.list().get(0).toString()) == false)
      throw new IOException("first element of Sexp representing " +
			    "NubmeredObject is not all digits: " +
			    sent.list().get(0));
    int sentNum = Integer.parseInt(sent.list().get(0).toString());
    boolean processed =
      Boolean.valueOf(sent.list().get(1).toString()).booleanValue();
    return new NumberedObject(sentNum, processed, sent.list().get(2));
  }

  public void close() throws IOException {
    tok.close();
  }
}
