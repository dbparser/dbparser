package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides a mapping between objects and integer counts that may be
 * incremented or decremented.
 */
public class BiCountsTable extends danbikel.util.HashMapTwoInts {
  /**
   * Constructs an empty <code>CountsTable</code>.
   */
  public BiCountsTable() {
    super();
  }

  /**
   * Constructs an empty <code>CountsTable</code> with the specified initial
   * number of hash buckets.
   *
   * @param initialCapacity the number of hash buckets that this object
   * will initially have
   */
  public BiCountsTable(int initialCapacity) {
    super(initialCapacity);
  }
  /**
   * Constructs an empty <code>CountsTable</code> with the specified initial
   * number of hash buckets and the specified load factor.  If the load
   * factor, which is average number of items per bucket, is exceeded
   * at runtime, the number of buckets is roughly doubled and the entire
   * map is re-hashed, as implemented by the parent class, {@link HashMap}.
   *
   * @param initialCapacity the number of hash buckets that this object
   * will initially have
   * @param loadFactor the load factor of this <code>HashMap</code> object
   */
  public BiCountsTable(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public void add(Object key, int index) {
    add(key, index, 1);
  }

  public int count(Object key, int index) {
    /*
    if (index < 0 || index > 1)
      throw new IllegalArgumentException();
    */
    MapToPrimitive.Entry e = getEntry(key);
    return (e == null ? 0 : e.getIntValue(index));
  }

  /**
   * Removes items in this table whose counts are less than the specified
   * threshold.
   *
   * @param threshold the count threshold below which to remove items from
   * this table
   * @param atIndex the index at which to check an item's count to see if
   * it falls below the specified threshold; the value of this argument
   * must be either 0 or 1
   */
  public void removeItemsBelow(int threshold, int atIndex) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getIntValue(atIndex) < threshold)
        it.remove();
    }
  }

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key value)</pre>
   */
  public void output(String eventName, Writer writer) throws IOException {
    Iterator keys = keySet().iterator();
    while (keys.hasNext()) {
      Object o = keys.next();
      writer.write("(");
      writer.write(eventName);
      writer.write(" ");
      writer.write(String.valueOf(o));
      writer.write(" ");
      writer.write(String.valueOf(count(o, 0)));
      writer.write(" ");
      writer.write(String.valueOf(count(o, 1)));
      writer.write(")\n");
    }
  }
}
