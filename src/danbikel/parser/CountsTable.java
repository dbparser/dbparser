package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;
import java.util.*;

/**
 * Specifies a mapping between objects and floating-point (<code>double</code>)
 * counts that may be incremented or decremented.
 */
public interface CountsTable extends danbikel.util.MapToPrimitive {
  /**
   * Adds all the counts from the specified table to this table, adding any
   * new keys in the specified map to this map, if necessary.
   *
   * @param other the other counts table whose counts are to be added
   * to this table
   */
  public void addAll(CountsTable other);

  /**
   * Puts the specified map of key objects to their counts into this
   * counts table.  If a key from the specified map already exists in this
   * map, it is simply replaced.
   *
   * @param other another counts table whose counts are to be put into
   * this table
   */
  public void putAll(CountsTable other);

  /**
   * Adds the specified key with a count of <code>1.0</code>.
   *
   * @param key the key to be added to this counts table
   */
  public void add(Object key);

  /**
   * Returns the count of the specified key, or <code>0</code> if this
   * counts table does not contain a count for the specified key.
   *
   * @param key the key whose count is to be gotten
   * @return the count of the specified key, or <code>0</code> if this
   * counts table does not contain a count for the specified key
   */
  public double count(Object key);

  /**
   * Returns the count of the specified key with the specified hash code, or
   * <code>0</code> if this counts table does not contain a count for the
   * specified key.
   *
   * @param key the key whose count is to be gotten
   * @param hashCode the hash code of the specified key
   * @return the count of the specified key with the specified hash code, or
   * <code>0</code> if this counts table does not contain a count for the
   * specified key
   */
  public double count(Object key, int hashCode);

  /**
   * Removes items in this table whose counts are less than the specified
   * threshold.
   *
   * @param threshold the count threshold below which to remove items from
   * this table
   */
  public void removeItemsBelow(double threshold);

  /**
   * Outputs all the mappings of this map in as S-expressions of the form
   * <pre>(name key value)</pre>
   */
  public void output(String eventName, Writer writer) throws IOException;
}
