package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides a mapping between objects and integer counts that may be
 * incremented or decremented.
 */
public class BiCountsTable extends danbikel.util.HashMapTwoDoubles {
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

  /**
   * Adds 1 to the counter at the specified index for the specified key in
   * this map.  If no such key exists, then a mapping is added, with 1 as
   * the value of the counter at the specified index and 0 as the value of
   * all other counters (this map contains only two counters).
   *
   * @param key the key whose count at the specified index is to be incremented
   * @param index the index of the counter to be incremented for the specified
   * key
   */
  public void add(Object key, int index) {
    add(key, index, 1.0);
  }

  public double count(Object key, int index) {
    /*
    if (index < 0 || index > 1)
      throw new IllegalArgumentException();
    */
    MapToPrimitive.Entry e = getEntry(key);
    return (e == null ? 0.0 : e.getDoubleValue(index));
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
  public void removeItemsBelow(double threshold, int atIndex) {
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
      if (entry.getDoubleValue(atIndex) < threshold)
        it.remove();
    }
  }

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key count0 count1)</pre>
   * where <tt>count0</tt> is the integer at index 0 and <tt>count1</t>
   * is the integer at index 1 for the key.
   *
   * @param eventName the name of this type of event, to be the first element
   * of the 4-element S-expression output by this method
   * @param writer the character stream to which this map's entries are to
   * be written
   * @throws IOException if the specified <tt>Writer</tt> throws an
   * <tt>IOException</tt> while it is being written to
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
