package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides a mapping between objects and integer counts that may be
 * incremented or decremented.
 */
public class CountsTable extends danbikel.util.HashMap {
  /**
   * Constructs an empty <code>CountsTable</code>.
   */
  public CountsTable() {
    super();
  }

  /**
   * Constructs an empty <code>CountsTable</code> with the specified initial
   * number of hash buckets.
   *
   * @param initialCapacity the number of hash buckets that this object
   * will initially have
   */
  public CountsTable(int initialCapacity) {
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
  public CountsTable(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Adds the specified object to this counts table with an initial count
   * of 1.
   *
   * @param o the object to add
   */
  public final void add(Object o) {
    add(o, 1);
  }

  /**
   * Adds the specified object to this counts table with the specified
   * count.  If the object is already in this counts table, then its count
   * is incremented by the specified count; if <code>count</code> is negative
   * then the object's counts is decremented.
   *
   * @param o the object to add
   * @param count the amount by which to increment <code>o</code>'s count
   */
  public final void add(Object o, int count) {
    IntCounter counter = (IntCounter)get(o);
    if (counter == null)
      super.put(o, new IntCounter(count));
    else
      counter.increment(count);
  }

  /**
   * Adds the specified key-value mapping to this map.  Invoking this method
   * with a key that is not an instance of <code>IntCounter</code> will cause
   * a runtime error.
   *
   * @deprecated This method has been overridden so that
   * <code>CountsTable</code> objects may not be used as normal
   * <code>HashMap</code> objects.
   */
  public Object put(Object key, Object value) {
    if (!(value instanceof IntCounter))
      return new IllegalArgumentException("value must be IntCounter");
    else
      return super.put(key, value);
  }

  /**
   * Returns the counts of the specified object.
   *
   * @param o the object whose count is to be retrieved
   * @return the count of object <code>o</code> if it exists in this
   * counts table, or 0 if it does not exist
   */
  public final int count(Object o) {
    IntCounter counter = (IntCounter)get(o);
    return ((counter == null) ? 0 : counter.get());
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
      writer.write(count(o));
      writer.write(")\n");
    }
  }
}
