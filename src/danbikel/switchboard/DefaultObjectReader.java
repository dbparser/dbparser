package danbikel.switchboard;

import java.io.*;

/**
 * Reads objects from an underlying Java object serialization stream.
 */
class DefaultObjectReader implements ObjectReader {
  /** The object input stream from which to get objects. */
  private ObjectInputStream ois;
  /** The input stream on which the ObjectInputStream is built (used
      to detect the end of the input stream. */
  private InputStream in;


  public DefaultObjectReader(InputStream in) throws IOException {
    ois = new ObjectInputStream(in);
    this.in = in;
  }

  public DefaultObjectReader(InputStream in, String encoding,
			     int bufSize) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(in, bufSize);
    ois = new ObjectInputStream(bis);
    this.in = bis;
  }



  /**
   * Constructs a new object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   *
   * @see SexpTokenizer#SexpTokenizer(String,String,int)
   */
  public DefaultObjectReader(String filename, String encoding,
			     int bufSize) throws IOException {
    BufferedInputStream bis =
      new BufferedInputStream(new FileInputStream(filename), bufSize);
    ois = new ObjectInputStream(bis);
  }

  public Object readObject() throws IOException {
    if (in.available() == 0)
      return null;

    Object nextObj = null;
    try {
      nextObj = ois.readObject();
    }
    catch (ClassNotFoundException cnfe) {
      throw new IOException(cnfe.toString());
    }
    return nextObj;
  }

  public void close() throws IOException {
    ois.close();
  }
}
