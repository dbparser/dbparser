package danbikel.util;

import java.util.*;

public class FixedSizeListFactory {

  private FixedSizeListFactory() {}

  private final static FixedSizeList emptyFixedSizeList =
    new FixedSizeArrayList(0);

  public static FixedSizeList newList(int size) {
    if (size < 0)
      throw new IllegalArgumentException();
    switch (size) {
    case 0:
      return emptyFixedSizeList;
    case 1:
      return new FixedSizeSingletonList(size);
    default:
      return new FixedSizeArrayList(size);
    }
  }

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
