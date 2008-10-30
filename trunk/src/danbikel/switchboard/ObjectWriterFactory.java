package danbikel.switchboard;

import java.io.*;

/**
 * A specification for constructing <code>ObjectWriter</code> instances.
 * This type of factory is used by the switchboard.
 *
 * @see ObjectWriter
 * @see Switchboard
 */
public interface ObjectWriterFactory {
  /**
   * Returns a newly-constructed <code>ObjectWriter</code> using the specified
   * stream. The two final arguments, <code>append</code> and
   * <code>emptyFile</code>, should be used to determine whether
   * stream header information needs to be written (by stream
   * implementations that use headers, such as
   * <code>ObjectOutputStream</code>).
   *
   * @param os the output stream around which to build an
   * <code>ObjectWriter</code>
   * @param append if <code>true</code>, indicates that the output stream
   * belongs to a file that is being appended to
   * @param emptyFile indicates whether the underlying file (if there is one)
   * for the output stream is currently empty
   */
  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException;

  /**
   * Returns a newly-constructed <code>ObjectWriter</code> using the
   * specified stream. If the underlying writer is character-based,
   * the encoding argument will be used; otherwise, it will be
   * ignored.  The <code>bufSize</code> argument should be used by
   * <code>ObjectWriter</code> implementations for construction of a
   * buffered stream or <code>Writer</code>. The two final arguments,
   * <code>append</code> and <code>emptyFile</code>, should be
   * used to determine whether stream header information needs to be
   * written (by stream implementations that use headers, such as
   * <code>ObjectOutputStream</code>).
   * 
   * @param os the output stream around which to build an
   * <code>ObjectWriter</code>
   * @param encoding the character encoding to be used (ignored if
   * the <code>ObjectWriter</code> implementor returned by this factory
   * is not character-based)
   * @param bufSize the suggested buffer size to be used by
   * <code>ObjectWriter</code> objects returned by this factory
   * @param append if <code>true</code>, indicates that the output stream
   * belongs to a file that is being appended to
   * @param emptyFile indicates whether the underlying file (if there is one)
   * for the output stream is currently empty */
  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException;

  /**
   * Returns a newly-constructed <code>ObjectWriter</code> using the
   * specified stream. If the underlying writer is character-based,
   * the encoding argument will be used; otherwise, it will be
   * ignored.  The <code>bufSize</code> argument should be used by
   * <code>ObjectWriter</code> implementations for construction of a
   * buffered stream or <code>Writer</code>.
   * 
   * @param filename the name of an output file around which to build an
   * <code>ObjectWriter</code>
   * @param encoding the character encoding to be used (ignored if
   * the <code>ObjectWriter</code> implementor returned by this factory
   * is not character-based)
   * @param bufSize the suggested buffer size to be used by
   * <code>ObjectWriter</code> objects returned by this factory
   * @param append indicates whether to append to the opened file
   */
  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append) throws IOException;
}

