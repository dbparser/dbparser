package danbikel.switchboard;

import java.io.*;

/**
 * Specifies methods for reading objects from an underlying
 * <code>Reader</code> or <code>InputStream</code> object.
 * This interface contains a strict subset of the methods specified
 * in <code>ObjectInput</code>, making it easy to adapt classes that
 * already implement <code>ObjectInput</code> to become implementors
 * of this interface.
 *
 * @see ObjectReaderFactory
 */
public interface ObjectReader {
  /**
   * Reads and returns the next object from the underlying
   * <code>Reader</code> or stream.
   *
   * @return the next object of the underlying <code>Reader</code> or stream, or
   * <code>null</code> if the end of the file or stream has been
   * reached
   *
   * @throws IOException if the underlying <code>Reader</code> or
   * input stream throws an <code>IOException</code>
   */
  public Object readObject() throws IOException;

  /**
   * Closes the underlying stream or <code>Reader</code> of this
   * <code>ObjectReader</code> object.
   */
  public void close() throws IOException;
}
