package danbikel.util;

import java.io.*;
import java.util.*;

/**
 * A fixed-size list of objects backed by an array.  By providing a
 * constant-time {@link #shift(Object)} method, this implementation supports
 * an efficient implementation of a circular buffer.
 */
public class FixedSizeArrayList
  extends AbstractFixedSizeList implements Serializable {
  protected Object[] data;
  protected int startIdx;

  public FixedSizeArrayList(int size) {
    super(size);
  }

  public FixedSizeArrayList(Collection c) {
    super(c);
  }

  protected void initialize(int size) {
    data = new Object[size];
  }

  public Object get(int index) {
    int size = data.length;

    /*
    if (index < 0 || index >= size)
      throw new IndexOutOfBoundsException();
    */

    int internalIdx = startIdx + index;
    if (internalIdx >= size)
      internalIdx -= size;
    return data[internalIdx];
  }

  public Object set(int index, Object obj) {
    int size = data.length;

    /*
    if (index < 0 || index >= size)
      throw new IndexOutOfBoundsException();
    */

    int internalIdx = startIdx + index;
    if (internalIdx >= size)
      internalIdx -= size;
    Object old = data[internalIdx];
    data[internalIdx] = obj;
    return old;
  }

  public int size() { return data.length; }

  public boolean shift(Object obj) {
    startIdx--;
    if (startIdx < 0)
      startIdx = data.length - 1;
    data[startIdx] = obj;
    return true;
  }
}
