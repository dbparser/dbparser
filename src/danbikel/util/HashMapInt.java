/*
 * This is a modified version of HashMap.java from Sun Microsystems.
 * Modified by Dan Bikel, 06/15/2001 (or 01/06/15, in Sun's notation).
 * We needed to be able to access the objects used as keys in a map,
 * which the default implementation does not allow for (in constant time,
 * anyway).  Also, we added diagnostic methods to determine the average
 * and maximum bucket sizes, which is useful (and we would argue, necessary)
 * when developing/debugging hash functions.  Finally, we wanted to
 * have a specific map of arbitrary objects to int's (not Integer objects,
 * but the basic type int).
 *
 *
 * Copyright and version information are as follows.
 * @(#)HashMap.java	1.38 00/02/02
 *
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package danbikel.util;
import java.util.*;
import java.io.*;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.<p>
 *
 * This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the intial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.<p>
 *
 * An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the capacity is roughly doubled by calling the
 * <tt>rehash</tt> method.<p>
 *
 * As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs.  Higher values decrease the space overhead
 * but increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>).  The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the
 * number of <tt>rehash</tt> operations.  If the initial capacity is greater
 * than the maximum number of entries divided by the load factor, no
 * <tt>rehash</tt> operations will ever occur.<p>
 *
 * If many mappings are to be stored in a <tt>HashMap</tt> instance, creating
 * it with a sufficiently large capacity will allow the mappings to be stored
 * more efficiently than letting it perform automatic rehashing as needed to
 * grow the table.<p>
 *
 * <b>Note that this implementation is not synchronized.</b> If multiple
 * threads access this map concurrently, and at least one of the threads
 * modifies the map structurally, it <i>must</i> be synchronized externally.
 * (A structural modification is any operation that adds or deletes one or
 * more mappings; merely changing the value associated with a key that an
 * instance already contains is not a structural modification.)  This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.  If no such object exists, the map should be
 * "wrapped" using the <tt>Collections.synchronizedMap</tt> method.  This is
 * best done at creation time, to prevent accidental unsynchronized access to
 * the map: <pre> Map m = Collections.synchronizedMap(new HashMap(...));
 * </pre><p>
 *
 * The iterators returned by all of this class's "collection view methods" are
 * <i>fail-fast</i>: if the map is structurally modified at any time after the
 * iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> or <tt>add</tt> methods, the iterator will throw a
 * <tt>ConcurrentModificationException</tt>.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Dan Bikel
 * @version 1.38, 02/02/00
 * @see     Object#hashCode()
 * @see     Collection
 * @see	    Map
 * @see	    Hashtable
 * @since 1.2
 */

public class HashMapInt extends HashMapPrimitive
  implements Cloneable, java.io.Serializable {
  /**
   * Constructs a new, empty map with the specified initial
   * capacity and the specified load factor.
   *
   * @param      initialCapacity   the initial capacity of the HashMap.
   * @param      loadFactor        the load factor of the HashMap
   * @throws     IllegalArgumentException  if the initial capacity is less
   *               than zero, or if the load factor is nonpositive.
   */
  public HashMapInt(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty map with the specified initial capacity
   * and default load factor, which is <tt>0.75</tt>.
   *
   * @param   initialCapacity   the initial capacity of the HashMap.
   * @throws    IllegalArgumentException if the initial capacity is less
   *              than zero.
   */
  public HashMapInt(int initialCapacity) {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty map with a default capacity and load
   * factor, which is <tt>0.75</tt>.
   */
  public HashMapInt() {
    this(11, 0.75f);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The
   * map is created with a capacity of twice the number of mappings in
   * the given map or 11 (whichever is greater), and a default load factor,
   * which is <tt>0.75</tt>.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapInt(Map t) {
    this(Math.max(2*t.size(), 11), 0.75f);
    putAll(t);
  }

  protected HashMapPrimitive.Entry getNewEntry(int hash, Object key,
                                               HashMapPrimitive.Entry next) {
    return new Entry(hash, key, next);
  }

  protected static class Entry extends HashMapPrimitive.Entry {
    transient protected int intVal0;

    public Entry() {}

    protected Entry(int hash, Object key, HashMapPrimitive.Entry next) {
      super(hash, key, next);
    }

    protected Entry(int hash, Object key, int value,
                    HashMapPrimitive.Entry next) {
      super(hash, key, next);
      intVal0 = value;
    }

    public int numInts() { return 1; }
    public int getIntValue(int index) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      return intVal0;
    }

    public int set(int index, int value) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      int oldVal = intVal0;
      intVal0 = value;
      return oldVal;
    }

    public void add(int index, int addend) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      intVal0 += addend;
    }

    public void copyValuesFrom(HashMapPrimitive.Entry copyFrom) {
      Entry other = (Entry)copyFrom;
      this.intVal0 = other.intVal0;
    }

    public String toString() {
      return key + "=" + intVal0;
    }

    public int hashCode() {
      return hash ^ intVal0;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry))
        return false;
      Entry other = (Entry)o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
              intVal0 == other.intVal0);
    }

    public Object clone() {
      return new Entry(hash, key, intVal0, next);
    }

    public void writeValues(java.io.ObjectOutput out) throws IOException {
      out.writeInt(intVal0);
    }
    public void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException {
      intVal0 = in.readInt();
    }
  }
}
