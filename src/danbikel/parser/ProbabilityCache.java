package danbikel.parser;

import danbikel.util.LRUCache;

/**
 * A least-recently used cache storing arbitrary objects with their
 * probabilities.
 */
public class ProbabilityCache extends java.util.WeakHashMap {

  /**
   * Constructs a <code>ProbabilityCache</code> with the specified minimum
   * capacity.
   *
   * @param minCapacity the minimum number of elements held by this cache
   * @see LRUCache#LRUCache(int)
   */
  public ProbabilityCache(int minCapacity) {
    //super(minCapacity);
    super();
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified minimum
   * capacity and the specified initial capacity.
   *
   * @param minCapacity the minimum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * <code>WeakHashMap</code>
   * @see LRUCache#LRUCache(int,int)
   */
  public ProbabilityCache(int minCapacity, int initialCapacity) {
    //super(minCapacity, initialCapacity);
    super(initialCapacity);
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified minimum
   * capacity, the specified initial capacity and the specified load factor.
   *
   * @param minCapacity the minimum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * <code>WeakHashMap</code>
   * @param loadFactor the load factor of the underlying
   * <code>WeakHashMap</code>
   * @see LRUCache#LRUCache(int,int,float)
   */
  public ProbabilityCache(int minCapacity, int initialCapacity,
			  float loadFactor) {
    //super(minCapacity, initialCapacity, loadFactor);
    super(initialCapacity, loadFactor);
  }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as the only
   * way to add keys to this specialized cache is through the
   * <code>put(Object,double)</code> method.
   *
   * @see #put(Object,double)
   */
  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds the specified key with the specified probability to this cache.
   *
   * @param key the key to add to this cache
   * @param probability the probability of the specified key to be cached
   * @return the old value associated with <code>key</code>, or
   * <code>null</code> if there was no mapping for this key
   */
  public synchronized Object put(Object key, double probability) {
    return super.put(key, new Double(probability));
  }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as the only
   * way to get values from this specialized cache is through the
   * <code>getProb(Object)</code> method.
   *
   * @see #getProb(Object)
   */
  public Object get(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the <code>Double</code> containing the probability of the specified
   * key, or <code>null</code> if the specified key is not in this
   * cache.
   *
   * @param key the key to look up in this cache
   * @return the probability of the specified key or <code>null</code>
   * if it is not in this cache
   */
    public synchronized Double getProb(Object key) {
    return (Double)super.get(key);
  }
}

