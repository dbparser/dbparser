package danbikel.util;

/**
 * Specifies useful/necessary diagnostic and lookup methods that the HashMap
 * API lacks.
 */
public interface FlexibleMap extends java.util.Map {

  /**
   * Gets the capacity of this map (optional operation).
   *
   * @return the capacity of this map (the number of buckets, in the case
   * of a hash map)
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  int getCapacity();

  /**
   * Gets the load factor of this map (optional operation).
   *
   * @return the load factor of this map
   * @throws UnsupportedOperationException if this map is not a hash map
   */
  float getLoadFactor();

  /**
   * Returns a string that represents the useful statistics of this map
   * (useful/necessary in the case of hash maps, where it is desirable to
   * know the number of collisions and average and maximum buckets sizes).
   * The format of the string is up to the implementor.
   */
  String getStats();

  /**
   * Returns the value for the specified key.  If the specified hash code
   * is not the value of <code>key.hashCode()</code>, the behavior of this
   * method is not defined.
   *
   * @param key the key whose value is to be looked up
   * @param hashCode the value of <code>key.hashCode()</code>
   */
  Object get(Object key, int hashCode);
}