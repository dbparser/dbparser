package danbikel.parser;

import java.util.Random;
import java.util.Iterator;

/**
 * A cache for storing arbitrary objects with their probabilities.  This class
 * uses a hash map, and offers several replacement strategies.
 */
public class ProbabilityCache extends danbikel.util.HashMapDouble {

  // public constants
  /**
   * Integer to indicate to delete a random element every time the size limit of
   * this cache has been exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int RANDOM = 0;
  /**
   * Integer to indicate to delete the least-recently used entry in the same
   * bucket as an entry being added after the size limit of this cache has
   * been reached or exceeded.  Put another way, after the size limit of the
   * cache has been reached (the size limit is specified at construction),
   * every time an element is cached, if its bucket contains at least one item,
   * then the least-recently used item of that bucket is first deleted and
   * then the current item is added.  Thus, this replacement strategy will
   * treat the maximum capacity of this cache as a soft limit, for in instances
   * where the bucket is already empty, this strategy will simply add an item,
   * deleting nothing.  A consequence is that the average bucket size can
   * increase over time, but never by more than 1 + the average bucket size at
   * the time the maximum capacity was reached (because only empty buckets can
   * increase in size from 0 to 1).
   * <p>
   * This strategy is the default.
   * <p>
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int BUCKET_LRU = 1;
  /**
   * Integer to indicate to delete a random half of the elements
   * every time the size limit of this cache has been reached or exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int HALF_LIFE = 2;
  /**
   * Integer to indicate to delete all of the elements
   * every time the size limit of this cache has been reached or exceeded.
   *
   * @see #setStrategy
   * @see #put(Object,double)
   */
  public final static int CLEAR_ALL = 3;

  // private constants
  private final static int MIN_STRATEGY_IDX = 0;
  private final static int MAX_STRATEGY_IDX = 3;
  private final static int defaultStrategy = BUCKET_LRU;

  private int maxCapacity;
  private int strategy;
  private Random rand;

  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   */
  public ProbabilityCache(int maxCapacity) {
    //super(minCapacity);
    super();
    this.maxCapacity = maxCapacity;
    setStrategy(defaultStrategy);
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity and the specified initial capacity.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * hash map
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public ProbabilityCache(int maxCapacity, int initialCapacity) {
    //super(minCapacity, initialCapacity);
    super(initialCapacity);
    this.maxCapacity = maxCapacity;
    setStrategy(defaultStrategy);
  }
  /**
   * Constructs a <code>ProbabilityCache</code> with the specified maximum
   * capacity, the specified initial capacity and the specified load factor.
   *
   * @param maxCapacity the maximum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * hash map
   * @param loadFactor the load factor of the underlying
   * hash map
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public ProbabilityCache(int maxCapacity, int initialCapacity,
			  float loadFactor) {
    //super(inCapacity, initialCapacity, loadFactor);
    super(initialCapacity, loadFactor);
    this.maxCapacity = maxCapacity;
    setStrategy(defaultStrategy);
  }

  /**
   * Sets the strategy for replacement when the size limit of this cache has
   * been reached.
   *
   * @see #BUCKET_LRU
   * @see #setStrategy(int)
   */
  public void setStrategy(int strategy) {
    if (strategy < MIN_STRATEGY_IDX || strategy > MAX_STRATEGY_IDX)
      throw new IllegalArgumentException();
    this.strategy = strategy;
    if (strategy == RANDOM)
      rand = new Random(System.currentTimeMillis());
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
   * As a side effect, if the maximum capacity of this cache has been
   * reached or exceeded at the time this method is invoked, then
   * one or more cached elements may be removed, depending on the
   * cache strategy being used.
   *
   * @param key the key to add to this cache
   * @param probability the probability of the specified key to be cached
   * @return the old value associated with <code>key</code>, or
   * <code>null</code> if there was no mapping for this key
   *
   * @see #setStrategy(int)
   */
  public synchronized void put(Object key, double probability) {
    if (size() >= maxCapacity) {
      switch (strategy) {
      case RANDOM:
        removeRandom();
        break;
      case BUCKET_LRU:
        //return super.putAndRemove(key, new Double(probability));
        super.putAndRemove(key, probability);
        return;
      case HALF_LIFE:
        clearHalf();
        break;
      case CLEAR_ALL:
        clear();
        break;
      }
    }
    //return super.put(key, new Double(probability));
    super.put(key, probability);
    return;
  }

  public synchronized void put(Object key, int hashCode, double probability) {
    if (size() >= maxCapacity) {
      switch (strategy) {
      case RANDOM:
        removeRandom();
        break;
      case BUCKET_LRU:
        //return super.putAndRemove(key, new Double(probability));
        super.putAndRemove(key, hashCode, probability);
        return;
      case HALF_LIFE:
        clearHalf();
        break;
      case CLEAR_ALL:
        clear();
        break;
      }
    }
    //return super.put(key, new Double(probability));
    super.put(key, hashCode, probability);
    return;
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
  public synchronized double getProb(Object key) {
    return (strategy == BUCKET_LRU ?
            super.getAndMakeMRU(key) :
            super.getDouble(key));
  }

  public synchronized double getProb(Object key, int hashCode) {
    return (strategy == BUCKET_LRU ?
            super.getAndMakeMRU(key, hashCode) :
            super.getDouble(key, hashCode));
  }

  public synchronized boolean containsKey(Object key) {
    return super.containsKey(key);
  }

  private void removeRandom() {
    int randIdx = rand.nextInt(capacity());
    removeRandom(randIdx);
  }

  private void clearHalf() {
    /*
    System.err.print("yea!  we're clearing half from a cache of size " +
                     size() + "...");
    */
    Iterator it = keySet().iterator();
    while (it.hasNext()) {
      it.next();
      if (it.hasNext()) {
        it.next();
        it.remove();
      }
    }
    // System.err.println("done");
  }
}
