package danbikel.switchboard;

import java.io.*;

/**
 * Specifies methods for writing objects to an unerlying
 * <code>Writer</code> or <code>OutputStream</code> object.
 * This interface contains a strict subset of the methods specified
 * in <code>ObjectOutput</code>, making it easy to adapt classes that
 * already implement <code>ObjectOutput</code> to become implementors
 * of this interface.
 */
public interface ObjectWriter {
  /**
   * Closes the underlying stream or <code>Writer</code> of this
   * <code>ObjectWriter</code> object.
   */
  public void close() throws IOException;

  /**
   * Writes the specified object to the underlying stream or
   * <code>Writer</code>.
   *
   * @throws IOException if the underlying <code>Writer</code> or
   * output stream throws an <code>IOException</code>
   */
  public void writeObject(Object obj) throws IOException;
}
