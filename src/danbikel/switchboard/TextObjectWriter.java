package danbikel.switchboard;

import java.io.*;

/**
 * A simple <code>ObjectWriter</code> that merely prints out objects' string
 * representations (as determined by their <code>toString</code> methods)
 * to an underlying character stream (<code>Writer</code>) followed by a
 * newline.
 *
 * @see TextObjectWriterFactory
 */
public class TextObjectWriter extends PrintWriter implements ObjectWriter {

  public TextObjectWriter(OutputStream os) throws IOException {
    super(new OutputStreamWriter(os));
  }

  public TextObjectWriter(OutputStream os, String encoding, int bufSize)
    throws IOException {
    super(new BufferedWriter(new OutputStreamWriter(os, encoding),
			     bufSize));
  }

  public TextObjectWriter(String filename, String encoding, int bufSize,
			  boolean append) 
    throws IOException {
    super(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append),
						    encoding),
			     bufSize));
  }
  /**
   * Writes the string representation of the specified object (as
   * determined by its <code>toString</code> method) followed by a
   * newline to the underlying <code>Writer</code> and flushes the
   * stream.
   */
  public void writeObject(Object obj) throws IOException {
    println(obj);
    flush();
  }
}
