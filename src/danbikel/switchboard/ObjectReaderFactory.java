package danbikel.switchboard;

import java.io.*;

/**
 * A specification for constructing <code>ObjectReader</code> instances.
 * This type of factory is used by the switchboard.
 *
 * @see ObjectReader
 * @see Switchboard
 */
public interface ObjectReaderFactory {
  /**
   * Gets a new object reader for the specified input stream, using
   * a default character encoding and buffer size, if applicable.
   */
  public ObjectReader get(InputStream in) throws IOException;
  
  /**
   * Gets a new object reader for the specified input stream.
   * If the implementation is character-based, the specified encoding
   * should be used; otherwise, the encoding argument should be
   * ignored.  Implementations should use buffering for their underlying read
   * operations, using the specified buffer size (if possible).
   */
  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException;

  /**
   * Gets a new object reader for the specified filename.
   * If the implementation is character-based, the specified encoding
   * should be used; otherwise, the encoding argument should be
   * ignored.  Implementations should use buffering for their underlying read
   * operations, using the specified buffer size (if possible).
   */
  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException;
}
