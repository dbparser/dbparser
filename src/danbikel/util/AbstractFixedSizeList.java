package danbikel.util;

import java.util.*;

/**
 * Provides a convenient default implementation for most of the methods of
 * <code>List</code> and <code>FixedSizeList</code>.
 */
public abstract class AbstractFixedSizeList
  extends AbstractList implements FixedSizeList {

  protected AbstractFixedSizeList(int size) {
    initialize(size);
  }

  protected AbstractFixedSizeList(Collection c) {
    initialize(c.size());
    addAll(c);
  }

  abstract protected void initialize(int size);

  public boolean add(Object obj) {
    return shift(obj);
  }

  public boolean addAll(Collection c) {
    Iterator it = c.iterator();
    int size = size();
    int i = 0;
    for ( ; i < size && it.hasNext(); i++) {
      set(i, it.next());
    }
    return i > 0;
  }

  public boolean addAll(int index, Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  abstract public Object set(int index, Object element);

  abstract public boolean shift(Object obj);

  /**
   * Compres this <code>FixedSizeList</code> to the specified object
   * for equality.  This implementation assumes that the <code>get(int)</code>
   * and <code>size()</code> methods take constant time.
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof FixedSizeList))
      return false;
    FixedSizeList other = (FixedSizeList)o;
    if (this.size() != other.size())
      return false;
    int size = size();
    for (int i = 0; i < size; i++)
      if (!this.get(i).equals(other.get(i)))
        return false;
    return true;
  }

  /**
   * Generates a hash code for this list.  This implementation assumes that
   * the <code>get(int)</code> and <code>size()</code> methods take constant
   * time.
   */
  public int hashCode() {
    int size = size();
    int code = 0;
    if (size < 10) {
      Object first = get(0);
      code = first == null ? 0 : first.hashCode();
      for (int i = 1; i < size; i++) {
        Object curr = get(i);
        code = (code << 2) ^ (curr == null ? 0 : curr.hashCode());
      }
    }
    else {
      code = 1;
      for (int i = 0; i < size; i++) {
        Object curr = get(i);
        code = 31*code + (curr == null ? 0 : curr.hashCode());
      }
    }
    return code;
  }
}