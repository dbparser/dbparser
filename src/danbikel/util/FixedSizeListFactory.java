package danbikel.util;

import java.util.*;

/**
 * A factory for {@link FixedSizeList} instances.
 *
 * @see FixedSizeSingletonList
 * @see FixedSizeArrayList
 */
public class FixedSizeListFactory {

  private FixedSizeListFactory() {
  }

  private final static FixedSizeList emptyFixedSizeList =
    new FixedSizeArrayList(0);

  /**
   * Returns a new {@link FixedSizeList} instance.
   *
   * @param size the size of the new list
   * @return a new {@link FixedSizeList} instance
   */
  public static FixedSizeList newList(int size) {
    if (size < 0) {
      throw new IllegalArgumentException();
    }
    switch (size) {
    case 0:
      return emptyFixedSizeList;
    case 1:
      return new FixedSizeSingletonList(size);
    default:
      return new FixedSizeArrayList(size);
    }
  }

  /**
   * Returns a new {@link FixedSizeList} instance whose elements will be that of
   * the specified collection.
   *
   * @param c the collection whose elements are to be included in the fixed-size
   *          list created by this method
   * @return a new {@link FixedSizeList} instance whose elements will be that of
   *         the specified collection
   */
  public static FixedSizeList newList(Collection c) {
    int size = c.size();
    switch (size) {
    case 0:
      return emptyFixedSizeList;
    case 1:
      return new FixedSizeSingletonList(c);
    default:
      return new FixedSizeArrayList(c);
    }
  }
}
