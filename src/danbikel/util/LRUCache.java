package danbikel.util;

import java.util.*;

/**
 * Provides a cache that makes least-recently used items available for
 * garbage collection.  <code>LRUCache</code> objects maintain an
 * internal data structure, storing a certain number of the cache keys
 * using strong references; the number of such items is called the
 * <i>minimum capacity</i> of the cache, and is specifiable at
 * construction time.  When the minimum capacity is exceeded, the
 * least-recently used key is removed from this data structure of
 * strong references, and the key will only be cached using a soft
 * reference, thus making it available for garbage collection.  While
 * it is not required, Java VMs are encouraged to reap older soft
 * referents before newer ones (this is true of Sun's JVMs, for
 * example); therefore, even when an object moves from the strong reference
 * list of this class to become simply a soft referent, it will likely still
 * be garbage-collected in a "least-recently used" fashion.
 */
public class LRUCache extends SofterHashMap {
  // constant
  private final static int softerHashMapType = SofterHashMap.weak;


  // inner class for linked list nodes
  private static class Node {
    Object element;
    Node next;

    Node(Object element, Node next) {
      this.element = element;
      this.next = next;
    }
  }

  // data members
  /**
   * Head of the circular linked list that holds strong keys in order
   * of usage.
   */
  private transient Node head;
  /** Map of strong keys to <code>Node</code> objects in LRU list. */
  private transient HashMap strongKeys;
  /**
   * Number of items in this cache that will not be garbage collected:
   * once this cache grows to have at least this number of items, it can never
   * shrink to have fewer items.  This property is achieved by maintaining
   * an internal structure to store up to <code>minCapacity</code> with
   * strong references.
   */
  private int minCapacity;
  /**
   * 1-element object array used by <code>SofterHashMap.getEntry</code>
   * to ensure that key has a strong reference to it prior to returning
   * the <code>Map.Entry</code> object.
   */
  private Object[] keyArr = new Object[1];


  // constructors

  /**
   * Constructs a cache where the most-recently used
   * <code>minCapacity</code> elements are guaranteed not to be
   * garbage collected.  The underlying <code>SofterHashMap</code> will
   * be constructed with the default initial capacity and the default
   * load factor.
   *
   * @param minCapacity the minimum number of elements held by this cache
   */
  public LRUCache(int minCapacity) {
    super(softerHashMapType);
    construct(minCapacity);
  }

  /**
   * Constructs an cache where the most-recently used
   * <code>minCapacity</code> elements are guaranteed not to be
   * garbage collected.  The underlying <code>SofterHashMap</code> will
   * be constructed with the specified initial capacity and the
   * default load factor.
   *
   * @param minCapacity the minimum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * <code>SofterHashMap</code>
   */
  public LRUCache(int minCapacity, int initialCapacity) {
    super(softerHashMapType, initialCapacity);
    construct(minCapacity);
  }

  /**
   * Constructs an cache where the most-recently used
   * <code>minCapacity</code> elements are guaranteed not to be
   * garbage collected.  The underlying <code>SofterHashMap</code> will
   * be constructed with the specified initial capacity and the
   * specified load factor.
   *
   * @param minCapacity the minimum number of elements held by this cache
   * @param initialCapacity the initial capacity of the underlying
   * <code>SofterHashMap</code>
   * @param loadFactor the load factor of the underlying
   * <code>SofterHashMap</code>
   */
  public LRUCache(int minCapacity, int initialCapacity, float loadFactor) {
    super(softerHashMapType, initialCapacity, loadFactor);
    construct(minCapacity);
  }

  private void construct(int minCapacity) {
    this.minCapacity = minCapacity;
    strongKeys = new HashMap(minCapacity);
    head = new Node(null, null);
    head.next = head;
  }

  /**
   * Associates the specified key with the specified value in this cache.
   * The specified key is considered to be the most-recently used key of this
   * cache, and the internal data structure containing strong references
   * is updated accordingly.
   *
   * @param key the key to put into this cache with the specified value
   * @param value the value to associate with the specified key in this cache
   * @return the old value associated with <code>key</code>, or
   * <code>null</code> if there was no mapping for this key
   */
  public Object put(Object key, Object value) {
    synchronized (strongKeys) {
      Object keyInMap = getKeyInMap(key);
      if (keyInMap != null)
	key = keyInMap;
      makeMostRecentlyUsed(key);

      return super.put(key, value);
    }
  }

  /**
   * Gets the value associated in this cache with the specified key.
   * The specified key is considered to be the most-recently used key of this
   * cache, and the internal data structure containing strong references
   * is updated accordingly.
   *
   * @param key the key whose value is to be retrieved from this cache
   * @return the value associated with the specified key, or <code>null</code>
   * if the key is not in this cache
   */
  public Object get(Object key) {
    synchronized (strongKeys) {
      keyArr[0] = key;
      Map.Entry entry = getEntry(keyArr);
      if (entry != null) {
	makeMostRecentlyUsed(keyArr[0]);
	keyArr[0] = null;                // it has served its purpose
	return entry.getValue();
      }
      else
	return null;
    }
  }

  /**
   * Makes the specified key the most recently used one in the internal
   * linked list.
   */
  private final void makeMostRecentlyUsed(Object key) {
    // if key is among strong references, remove it
    Node oldKeyNode = (Node)strongKeys.get(key);
    boolean inStrongKeys = oldKeyNode != null;
    if (inStrongKeys)
      remove(oldKeyNode);
    else if (strongKeys.size() == minCapacity) // if strong keys are maxed out,
      removeFirst();                      // remove least-recently used element

    addLast(key); // last element is most-recently used
  }

  /**
   * Removes the specified key from this cache.
   * @param key the key to remove from this cache
   * @return the value of the cached key in this cache, or <code>null</code>
   * if this cache did not containg <code>key</code>
   */
  public Object remove(Object key) {
    synchronized (strongKeys) {
      if (strongKeys.containsKey(key))
	remove((Node)strongKeys.get(key));
      return super.remove(key);
    }
  }

  private final void removeFirst() {
    remove(head.next);
  }

  /**
   * Remove the specified node in the linked list.
   */
  private final void remove(Node node) {
    if (strongKeys.size() == 0 || node == head) {
      return;
    }
    else {
      // first, grab data at specified node
      Object removedElement = node.element;

      // make specified node become the node after it
      Node nextNode = node.next;
      node.element = nextNode.element;
      node.next = nextNode.next;
      if (nextNode == head)
	head = node;

      // keep strongKeys map up to date
      strongKeys.remove(removedElement);
      if (node != head)
	strongKeys.put(node.element, node);
    }
  }

  private final void addLast(Object element) {
    addBefore(head, element);
  }

  /**
   * Adds a new node containing the specified element before the specified
   * existing node.
   */
  private final void addBefore(Node node, Object element) {
    // make specified node become a "new" node with specified element;
    // make its next node be a copy of the specified old node
    Node oldNode = new Node(node.element, node.next);
    node.element = element;
    node.next = oldNode;
    if (node == head)
      head = oldNode;

    // keep strongKeys map up to date
    if (oldNode != head)
      strongKeys.put(oldNode.element, oldNode);
    strongKeys.put(node.element, node);
  }

  /**
   * Returns a string representation of the subset of key-value pairs of
   * this cache for which strong references are used for the keys.
   * This method is intended to be used for debugging.
   */
  public final String listToString() {
    synchronized (strongKeys) {
      StringBuffer sb = new StringBuffer();
      sb.append("{");
      Node curr = head.next;
      for (; curr != head; curr = curr.next) {
	sb.append(curr.element).append("=").append(super.get(curr.element));
	if (curr.next != head)
	  sb.append(", ");
      }
      sb.append("}");
      return sb.toString();
    }
  }
}
