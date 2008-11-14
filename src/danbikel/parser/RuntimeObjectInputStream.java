package danbikel.parser;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A subclass of {@link ObjectInputStream} that contains a {@link Runtime}
 * object with which to resolve all serialized {@link Runtime} objects.
 */
public class RuntimeObjectInputStream extends ObjectInputStream {
  private Runtime rt;

  /**
   * Constructs a new instance containing the specified {@link Runtime} object
   * and reading from the specified input stream.
   *
   * @param rt	  the {@link Runtime} object to use when resolving all
   *                    serialized {@link Runtime} objects read by this {@link
   *                    ObjectInputStream}
   * @param inputStream the input stream from which to deserialize objects
   * @throws IOException if there is a problem reading from the specified input
   *                     stream
   */
  public RuntimeObjectInputStream(Runtime rt,
				  InputStream inputStream) throws IOException {
    super(inputStream);
    this.rt = rt;
    enableResolveObject(true);
  }

  @Override
  /**
   * If the specified object is an instance of {@link Runtime}, resolves
   * it to the {@link Runtime} instance provided at construction time;
   * otherwise, returns the specified object.
   *
   * @param o the object to resolve
   *
   * @return returns the internal {@link Runtime} instance if the specified
   * object is an instance of {@link Runtime}; returns the specified object
   * otherwise
   */
  protected Object resolveObject(Object o) throws IOException {
    if (o instanceof Runtime) {
      enableResolveObject(false);
      return rt;
    } else {
      return o;
    }
  }
}
