package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Provides a mapping between objects and integer counts that may be
 * incremented or decremented.
 */
public class CountsTable extends danbikel.util.HashMapInt {
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
