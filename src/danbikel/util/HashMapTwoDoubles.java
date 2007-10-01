/*
 * This is a modified version of HashMap.java from Sun Microsystems.  Modified
 * by Dan Bikel, 06/15/2001 (or 01/06/15, in Sun's notation).  We needed to be
 * able to access the objects used as keys in a map, which the default
 * implementation does not allow for (in constant time, anyway).  Also, we
 * added diagnostic methods to determine the average and maximum bucket sizes,
 * which is useful (and we would argue, necessary) when developing/debugging
 * hash functions.  Finally, we wanted to have a specific map of arbitrary
 * objects to two double's (not Double objects, but the basic type double).
 *
 *
 * Copyright and version information are as follows.
 * @(#)HashMap.java	1.38 00/02/02
 *
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * */

package danbikel.util;
import java.util.*;
import java.io.*;

/**
 *
 */
public class HashMapTwoDoubles<K> extends HashMapDouble<K> {
  /**
   * Constructs a new, empty map with the specified initial
   * capacity and the specified load factor.
   *
   * @param      initialCapacity   the initial capacity of the HashMap.
   * @param      loadFactor        the load factor of the HashMap
   * @throws     IllegalArgumentException  if the initial capacity is less
   *               than zero, or if the load factor is nonpositive.
   */
  public HashMapTwoDoubles(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty map with the specified initial capacity
   * and {@link HashMapPrimitive#defaultLoadFactor default load factor}.
   *
   * @param   initialCapacity   the initial capacity of the HashMap.
   * @throws    IllegalArgumentException if the initial capacity is less
   *              than zero.
   */
  public HashMapTwoDoubles(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new, empty map with a
   * {@link HashMapPrimitive#defaultInitialCapacity default capacity} and
   * {@link #defaultLoadFactor load factor}.
   */
  public HashMapTwoDoubles() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The
   * map is created with a capacity of twice the number of mappings in
   * the given map or {@link HashMapPrimitive#defaultInitialCapacity the default size}
   * (whichever is greater), and
   * {@link HashMapPrimitive#defaultLoadFactor a default load factor}.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapTwoDoubles(Map t) {
    this(Math.max(2*t.size(), defaultInitialCapacity), defaultLoadFactor);
    putAll(t);
  }

  protected HashMapPrimitive.Entry<K> getNewEntry(int hash, K key,
						  HashMapPrimitive.Entry<K> next) {
    return new Entry<K>(hash, key, next);
  }

  protected static class Entry<K> extends HashMapPrimitive.Entry<K> {
    transient protected double doubleVal0;
    transient protected double doubleVal1;

    public Entry() {}

    protected Entry(int hash, K key, HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
    }

    protected Entry(int hash, K key, double value0, double value1,
                    HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
      doubleVal0 = value0;
      doubleVal1 = value1;
    }

    public int numDoubles() { return 2; }

    public final double getDoubleValue(int index) {
      switch (index) {
      case 0:
        return doubleVal0;
      case 1:
        return doubleVal1;
      default:
        throw new IllegalArgumentException();
      }
    }

    public final double set(int index, double value) {
      double oldVal = 0;
      switch (index) {
      case 0:
        oldVal = doubleVal0;
        doubleVal0 = value;
        break;
      case 1:
        oldVal = doubleVal1;
        doubleVal1 = value;
        break;
      default:
        throw new IllegalArgumentException();
      }
      return oldVal;
    }

    public final void add(int index, double addend) {
      switch (index) {
      case 0:
        doubleVal0 += addend;
        break;
      case 1:
        doubleVal1 += addend;
        break;
      default:
        throw new IllegalArgumentException();
      }
    }

    public void copyValuesFrom(HashMapPrimitive.Entry copyFrom) {
      Entry other = (Entry)copyFrom;
      this.doubleVal0 = other.doubleVal0;
      this.doubleVal1 = other.doubleVal1;
    }

    public String toString() {
      return key + "=" + doubleVal0 + "," + doubleVal1;
    }

    public int hashCode() {
      long v1 = Double.doubleToLongBits(doubleVal0);
      long v2 = Double.doubleToLongBits(doubleVal1);
      int doubleVal1Hash = (int)(v1^(v1>>>32));
      int doubleVal2Hash = (int)(v2^(v2>>>32));
      return ((keyHash ^ doubleVal1Hash) << 2) ^ doubleVal2Hash;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry))
        return false;
      Entry other = (Entry)o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
              doubleVal0 == other.doubleVal0 &&
              doubleVal1 == other.doubleVal1);
    }

    public Object clone() {
      return new Entry<K>(keyHash, key, doubleVal0, doubleVal1, next);
    }
    public void writeValues(java.io.ObjectOutput out) throws IOException {
      out.writeDouble(doubleVal0);
      out.writeDouble(doubleVal1);
    }
    public void readValues(java.io.ObjectInput in)
    throws IOException, ClassNotFoundException {
      doubleVal0 = in.readDouble();
      doubleVal1 = in.readDouble();
    }
  }
}
