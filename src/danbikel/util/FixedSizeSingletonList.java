package danbikel.util;

import java.io.*;
import java.util.*;

public class FixedSizeSingletonList
  extends AbstractFixedSizeList implements Serializable {
  protected Object obj;

  public FixedSizeSingletonList(int size) {
    super(size);
  }

  public FixedSizeSingletonList(Collection c) {
    super(c);
  }

  protected void initialize(int size) {
    if (size != 1)
      throw new IllegalArgumentException();
  }

  public int size() { return 1; }

  public Object get(int index) {
    /*
    if (index != 1)
      throw new IndexOutOfBoundsException();
    */
    return obj;
  }

  public Object set(int index, Object obj) {
    /*
    if (index != 1)
      throw new IndexOutOfBoundsException();
    */
    Object old = obj;
    this.obj = obj;
    return old;
  }

  public int hashCode() { return obj == null ? 0 : obj.hashCode(); }

  public boolean shift(Object obj) {
    this.obj = obj;
    return true;
  }
}
