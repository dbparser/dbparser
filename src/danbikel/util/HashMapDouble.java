package danbikel.util;
import java.util.*;
import java.io.*;

/**
 *
 */
public class HashMapDouble<K> extends HashMapPrimitive<K>
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
  public HashMapDouble(int initialCapacity, float loadFactor) {
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
  public HashMapDouble(int initialCapacity) {
    this(initialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new, empty map with a
   * {@link HashMapPrimitive#defaultInitialCapacity default capacity} and
   * {@link HashMapPrimitive#defaultLoadFactor load factor}.
   */
  public HashMapDouble() {
    this(defaultInitialCapacity, defaultLoadFactor);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The
   * map is created with a capacity of twice the number of mappings in
   * the given map or {@link HashMapPrimitive#defaultInitialCapacity the default size}
   * (whichever is greater), and a
   * {@link HashMapPrimitive#defaultLoadFactor default load factor}.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapDouble(Map t) {
    this(Math.max(2*t.size(), defaultInitialCapacity), defaultLoadFactor);
    putAll(t);
  }

  protected HashMapPrimitive.Entry<K> getNewEntry(int hash, K key,
						  HashMapPrimitive.Entry<K> next) {
    return new Entry<K>(hash, key, next);
  }

  protected static class Entry<K> extends HashMapPrimitive.Entry<K> {
    transient protected double doubleVal0;

    public Entry() {}

    protected Entry(int hash, K key, HashMapPrimitive.Entry<K> next) {
      super(hash, key, next);
    }

    protected Entry(int hash, K key, double value,
                    HashMapPrimitive.Entry next) {
      super(hash, key, next);
      doubleVal0 = value;
    }

    public Object getValue() {
      return doubleVal0;
    }

    public int numDoubles() { return 1; }
    public double getDoubleValue(int index) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      return doubleVal0;
    }

    public double set(int index, double value) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      double oldVal = doubleVal0;
      doubleVal0 = value;
      return oldVal;
    }

    public void add(int index, double addend) {
      /*
      if (index != 0)
        throw new IllegalArgumentException();
      */
      doubleVal0 += addend;
    }

    public void copyValuesFrom(HashMapPrimitive.Entry copyFrom) {
      Entry other = (Entry)copyFrom;
      this.doubleVal0 = other.doubleVal0;
    }

    public String toString() {
      return key + "=" + doubleVal0;
    }

    public int hashCode() {
      long v = Double.doubleToLongBits(doubleVal0);
      return keyHash ^ (int)(v^(v>>>32));
    }

    public boolean equals(Object o) {
      if (!(o instanceof Entry))
        return false;
      Entry other = (Entry)o;
      return ((key == null ? other.key == null : key.equals(other.key)) &&
              doubleVal0 == other.doubleVal0);
    }

    public Object clone() {
      return new Entry(keyHash, key, doubleVal0, next);
    }

    public void writeValues(java.io.ObjectOutput out) throws IOException {
      out.writeDouble(doubleVal0);
    }
    public void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException {
      doubleVal0 = in.readDouble();
    }
  }
}
