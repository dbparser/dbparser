package danbikel.util;

import java.io.*;
import java.util.*;

/**
 * A {@link FixedSizeList} implementation for a singleton list (a list with only
 * one element).
 */
public class FixedSizeSingletonList
  extends AbstractFixedSizeList implements Serializable {
  protected Object obj;

  /**
   * Constructs a new fixed-size list with the specified size.
   *
   * @param size the size of the list to create
   * @throws IllegalArgumentException if the specified size is greater than 1
   */
  public FixedSizeSingletonList(int size) {
    super(size);
  }

  /**
   * Constructs a new fixed-size list containing the element in the specified
   * collection.
   *
   * @param c a collection containing at most one element
   * @throws IllegalArgumentException if the specified collection contains more
   *                                  than one element
   */
  public FixedSizeSingletonList(Collection c) {
    super(c);
  }

  /**
   * Initializes this singleton list.
   *
   * @param size the size of this list (must be 1)
   * @throws IllegalArgumentException if the specified size is greater than 1
   */
  protected void initialize(int size) {
    if (size != 1) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Returns 1.
   *
   * @return 1
   */
  public int size() {
    return 1;
  }

  /**
   * Returns the sole object in this list.
   *
   * @param index the index of the object to return (ignored)
   * @return the sole object in this list
   */
  public Object get(int index) {
    return obj;
  }

  /**
   * Sets the object wrapped by this singleton list.
   *
   * @param index the index of the object to wrap (ignored)
   * @param obj   the object that this list should wrap
   * @return the old object wrapped by this singleton list
   */
  public Object set(int index, Object obj) {
    Object old = obj;
    this.obj = obj;
    return old;
  }

  public int hashCode() {
    return obj == null ? 0 : obj.hashCode();
  }

  /**
   * Shifts the old object wrapped by this list &ldquo;away&rdquo; and causes
   * this list to wrap the specified object.  This method modifies this list in
   * the same way that {@link #set} does.
   *
   * @param obj the object to shift into this singleton list
   * @return <code>true</code> under all circumstances
   */
  public boolean shift(Object obj) {
    this.obj = obj;
    return true;
  }
}
