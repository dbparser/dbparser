package danbikel.util;

import java.util.*;
import java.io.*;

/**
 *
 */
public class HashMapInt<K> extends HashMapPrimitive<K>
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
   * and {@link HashMapPrimitive#defaultLoadFactor default load factor}.
   *
   * @param   initialCapacity   the initial capacity of the HashMap.
   * @throws    IllegalArgumentException if the initial capacity is less
   *              than zero.
   */
  public HashMapInt(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new, empty map with a
   * {@link HashMapPrimitive#defaultInitialCapacity default capacity} and
   * {@link #defaultLoadFactor load factor}.
   */
  public HashMapInt() {
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
  public HashMapInt(Map t) {
    this(Math.max(2*t.size(), defaultInitialCapacity), defaultLoadFactor);
    putAll(t);
  }

  protected HashMapPrimitive.Entry<K> getNewEntry(int hash, K key,
						  HashMapPrimitive.Entry<K> next) {
    return new Entry<K>(hash, key, next);
  }

  protected static class Entry<K> extends HashMapPrimitive.Entry<K> {
    transient protected int intVal0;

    public Entry() {}

    protected Entry(int hash, K key, HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
    }

    protected Entry(int hash, K key, int value,
                    HashMapPrimitive.Entry<K> next) {
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
      return keyHash ^ intVal0;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry))
        return false;
      Entry other = (Entry)o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
              intVal0 == other.intVal0);
    }

    public Object clone() {
      return new Entry<K>(keyHash, key, intVal0, next);
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
