/*
 * @(#)HashMap.java	1.38 00/02/02
 *
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 *
 */

/**
 * Version 1.38 of HashMap has been modified by Dan Bikel to create the
 * SofterHashMap class defined below.
 */

package danbikel.util;
import java.io.*;
import java.util.*;
import java.util.TreeMap;
import java.lang.ref.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.ref.PhantomReference;

/**
 * A hashtable-based <code>Map</code> implementation with <em>softer
 * keys</em>, which means that the key objects are referred to by
 * either soft, weak or phantom references (the actual type of
 * references used is specified at construction). An entry in a
 * <code>SofterHashMap</code> will automatically be removed when its
 * key is no longer in ordinary use.  More precisely, the presence of
 * a mapping for a given key will not prevent the key from being
 * discarded by the garbage collector, that is, made finalizable,
 * finalized, and then reclaimed.  When a key has been discarded its
 * entry is effectively removed from the map, so this class behaves
 * somewhat differently than other <code>Map</code> implementations.
 *
 * <p> Both null values and the null key are supported.  This class
 * has performance characteristics similar to those of the
 * <code>HashMap</code> class, and has the same efficiency parameters
 * of <em>initial capacity</em> and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <code>SofterHashMap</code> may be constructed using the
 * <code>Collections.synchronizedMap</code> method.
 *
 * <p> This class is intended primarily for use with key objects whose
 * <code>equals</code> methods test for object identity using the
 * <code>==</code> operator.  Once such a key is discarded it can
 * never be recreated, so it is impossible to do a lookup of that key
 * in a <code>SofterHashMap</code> at some later time and be surprised
 * that its entry has been removed.  This class will work perfectly
 * well with key objects whose <code>equals</code> methods are not
 * based upon object identity, such as <code>String</code> instances.
 * With such recreatable key objects, however, the automatic removal
 * of <code>SofterHashMap</code> entries whose keys have been
 * discarded may prove to be confusing.
 *
 * <p> The behavior of the <code>SofterHashMap</code> class depends in
 * part upon the actions of the garbage collector, so several familiar
 * (though not required) <code>Map</code> invariants do not hold for
 * this class.  Because the garbage collector may discard keys at any
 * time, a <code>SofterHashMap</code> may behave as though an unknown
 * thread is silently removing entries.  In particular, even if you
 * synchronize on a <code>SofterHashMap</code> instance and invoke
 * none of its mutator methods, it is possible for the
 * <code>size</code> method to return smaller values over time, for
 * the <code>isEmpty</code> method to return <code>false</code> and
 * then <code>true</code>, for the <code>containsKey</code> method to
 * return <code>true</code> and later <code>false</code> for a given
 * key, for the <code>get</code> method to return a value for a given
 * key but later return <code>null</code>, for the <code>put</code>
 * method to return <code>null</code> and the <code>remove</code>
 * method to return <code>false</code> for a key that previously
 * appeared to be in the map, and for successive examinations of the
 * key set, the value set, and the entry set to yield successively
 * smaller numbers of elements.
 *
 * <p> Each key object in a <code>SofterHashMap</code> is stored
 * indirectly as the referent of a soft, weak or phantom reference.
 * Therefore a key will automatically be removed only after the soft,
 * weak or phantom references to it, both inside and outside of the
 * map, have been cleared by the garbage collector.
 *
 * <p> <strong>Implementation note:</strong> The value objects in a
 * <code>SofterHashMap</code> are held by ordinary strong references.
 * Thus care should be taken to ensure that value objects do not
 * strongly refer to their own keys, either directly or indirectly,
 * since that will prevent the keys from being discarded.  Note that a
 * value object may refer indirectly to its key via the
 * <code>SofterHashMap</code> itself; that is, a value object may
 * strongly refer to some other key object whose associated value
 * object, in turn, strongly refers to the key of the first value
 * object.  This problem may be fixed in a future release.
 *
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Dan Bikel
 * @version 1.38, 02/02/00 (HashMap version, modified by Dan Bikel to create
 *                          SofterHashMap, 01/14/2001)
 * @see     Object#hashCode()
 * @see     Collection
 * @see	    Map
 * @see	    TreeMap
 * @see	    Hashtable
 * @see     java.lang.ref.SoftReference
 * @see     java.lang.ref.WeakReference
 * @see     java.lang.ref.PhantomReference
 */
public class SofterHashMap extends AbstractMap implements Map, Cloneable {

  private final static boolean debug = false;

  /*
   * <code>SofterHashMap</code> objects contain mappings from one of three
   * of the classes below to arbitrary value objects.
   */

  private static class SoftKey extends SoftReference {
    private int hash;	/* Hashcode of key, stored here since the key
			   may be tossed by the GC */

    private SoftKey(Object k) {
      super(k);
      hash = k.hashCode();
    }

    private SoftKey(Object k, ReferenceQueue q) {
      super(k, q);
      hash = k.hashCode();
    }

    /* A SoftKey is equal to another SoftKey iff they both refer to objects
       that are, in turn, equal according to their own equals methods */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SoftKey)) return false;
      Object t = this.get();
      Object u = ((SoftKey)o).get();
      if ((t == null) || (u == null)) return false;
      if (t == u) return true;
      return t.equals(u);
    }

    public int hashCode() {
      return hash;
    }

  }

  private static class WeakKey extends WeakReference {
    private int hash;	/* Hashcode of key, stored here since the key
			   may be tossed by the GC */

    private WeakKey(Object k) {
      super(k);
      hash = k.hashCode();
    }

    private WeakKey(Object k, ReferenceQueue q) {
      super(k, q);
      hash = k.hashCode();
    }

    /* A WeakKey is equal to another WeakKey iff they both refer to objects
       that are, in turn, equal according to their own equals methods */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof WeakKey)) return false;
      Object t = this.get();
      Object u = ((WeakKey)o).get();
      if ((t == null) || (u == null)) return false;
      if (t == u) return true;
      return t.equals(u);
    }

    public int hashCode() {
      return hash;
    }

  }


  private static class PhantomKey extends PhantomReference {
    private int hash;	/* Hashcode of key, stored here since the key
			   may be tossed by the GC */

    private PhantomKey(Object k, ReferenceQueue q) {
      super(k, q);
      hash = k.hashCode();
    }

    /* A PhantomKey is equal to another PhantomKey iff they both refer
       to objects that are, in turn, equal according to their own
       equals methods */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PhantomKey)) return false;
      Object t = this.get();
      Object u = ((PhantomKey)o).get();
      if ((t == null) || (u == null)) return false;
      if (t == u) return true;
      return t.equals(u);
    }

    public int hashCode() {
      return hash;
    }

  }


  /**
   * The hash table data.
   */
  private transient Entry table[];

  /**
   * The total number of mappings in the hash table.
   */
  private transient int count;

  /**
   * The table is rehashed when its size exceeds this threshold.  (The
   * value of this field is (int)(capacity * loadFactor).)
   *
   * @serial
   */
  private int threshold;

  /**
   * The load factor for the hashtable.
   *
   * @serial
   */
  private float loadFactor;

  /**
   * The number of times this HashMap has been structurally modified
   * Structural modifications are those that change the number of mappings in
   * the HashMap or otherwise modify its internal structure (e.g.,
   * rehash).  This field is used to make iterators on Collection-views of
   * the HashMap fail-fast.  (See ConcurrentModificationException).
   */
  private transient int modCount = 0;

  private transient ReferenceQueue queue = new ReferenceQueue();

  private int referenceType;

  // constants
  /**
   * The integer to specify the use of soft references for keys in this map.
   *
   * @see #SofterHashMap(int,int,float)
   * @see #SofterHashMap(int,int)
   * @see #SofterHashMap(int)
   * @see #SofterHashMap(int,Map)
   */
  public static final int soft = 0;
  /**
   * The integer to specify the use of weak references for keys in this map.
   *
   * @see #SofterHashMap(int,int,float)
   * @see #SofterHashMap(int,int)
   * @see #SofterHashMap(int)
   * @see #SofterHashMap(int,Map)
   */
  public static final int weak = 1;
  /**
   * The integer to specify the use of phantom references for keys in this map.
   *
   * @see #SofterHashMap(int,int,float)
   * @see #SofterHashMap(int,int)
   * @see #SofterHashMap(int)
   * @see #SofterHashMap(int,Map)
   */
  public static final int phantom = 2;
  /**
   * The number of types of references this map can use to store keys:
   * 3.  The first parameter of each of the constructors of this class
   * must be in the interval [0,&nbsp;<tt>numTypes</tt>).  This
   * constant allows range-checking to occur before invocation of a
   * constructor of this class, if desired.
   *
   * @see #soft
   * @see #weak
   * @see #phantom
   * @see #SofterHashMap(int,int,float)
   * @see #SofterHashMap(int,int)
   * @see #SofterHashMap(int)
   * @see #SofterHashMap(int,Map)
   */
  public static final int numTypes = 3;


  /* Remove all invalidated entries from the map, that is, remove all entries
     whose keys have been discarded.  This method should be invoked once by
     each public mutator in this class.  We don't invoke this method in
     public accessors because that can lead to surprising
     ConcurrentModificationExceptions. */
  private void processQueue() {
    Reference keyRef;
    while ((keyRef = (Reference)queue.poll()) != null) {
      removeKeyRef(keyRef);
    }
  }

  /**
   * Constructs a new, empty map with the specified initial 
   * capacity and the specified load factor, and one that will use
   * the specified type of references to store keys.
   *
   * @param referenceType the type of references used to hold keys in
   * this map; the value of this parameter must be either {@link #soft},
   * {@link #weak} or {@link #phantom}
   * @param      initialCapacity   the initial capacity of the HashMap.
   * @param      loadFactor        the load factor of the HashMap
   * @throws     IllegalArgumentException  if the initial capacity is less
   *             than zero, if the load factor is nonpositive, or if
   *             <code>referenceType</code> is not in the interval
   *             [0,&nbsp;{@link #numTypes})
   */
  public SofterHashMap(int referenceType,
		       int initialCapacity, float loadFactor) {
    if (referenceType < 0 || referenceType >= numTypes)
      throw new IllegalArgumentException("Illegal type index: " +
					 referenceType);
    this.referenceType = referenceType;
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal Initial Capacity: "+
					 initialCapacity);
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
      throw new IllegalArgumentException("Illegal Load factor: "+
					 loadFactor);
    if (initialCapacity==0)
      initialCapacity = 1;
    this.loadFactor = loadFactor;
    table = new Entry[initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
  }

  /**
   * Constructs a new, empty map with the specified initial capacity
   * and default load factor, which is <tt>0.75</tt>; the new map
   * will use the specified type of references to store keys.
   *
   * @param referenceType the type of references used to hold keys in
   * this map; the value of this parameter must be either {@link #soft},
   * {@link #weak} or {@link #phantom}
   * @param   initialCapacity   the initial capacity of the HashMap.
   * @throws IllegalArgumentException if the initial capacity is less
   *         than zero, or if <code>referenceType</code> is not in the
   *         interval [0,&nbsp;{@link #numTypes})
   */
  public SofterHashMap(int referenceType, int initialCapacity) {
    this(referenceType, initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty map with a default capacity and load
   * factor, which is <tt>0.75</tt>; the new map will use the specified
   * type of references to store keys.
   *
   * @param referenceType the type of references used to hold keys in
   * this map; the value of this parameter must be either {@link #soft},
   * {@link #weak} or {@link #phantom}
   * @throws IllegalArgumentException if <code>referenceType</code> is
   *         not in the interval [0,&nbsp;{@link #numTypes})
   */
  public SofterHashMap(int referenceType) {
    this(referenceType, 11, 0.75f);
  }

  /**
   * Constructs a new map with the same mappings as the given map, using
   * the specified type of references to store keys.  The
   * map is created with a capacity of twice the number of mappings in
   * the given map or 11 (whichever is greater), and a default load factor,
   * which is <tt>0.75</tt>.
   *
   * @param referenceType the type of references used to hold keys in
   * this map; the value of this parameter must be one either {@link
   * #soft}, {@link #weak} or {@link #phantom}
   * @param t the map whose mappings are to be placed in this map.
   * @throws IllegalArgumentException if <code>referenceType</code> is
   *         not in the interval [0,&nbsp;{@link #numTypes})
   */
  public SofterHashMap(int referenceType, Map t) {
    this(referenceType, Math.max(2*t.size(), 11), 0.75f);
    putAll(t);
  }

  /**
   * Returns the number of key-value mappings in this map.
   * <strong>Note:</strong> <em>In contrast with most implementations of the
   * <code>Map</code> interface, the time required by this operation is
   * linear in the size of the map.</em>
   *
   * @return the number of key-value mappings in this map.
   */
  public int size() {
    //return count;
    return entrySet().size();
  }

  /**
   * Returns <tt>true</tt> if this map contains no key-value mappings.
   * <strong>Note:</strong> <em>In contrast with most implementations of the
   * <code>Map</code> interface, the time required by this operation is
   * linear in the size of the map.</em>
   *
   * @return <tt>true</tt> if this map contains no key-value mappings.
   */
  public boolean isEmpty() {
    return entrySet().isEmpty();
  }

  /**
   * Returns <tt>true</tt> if this map maps one or more keys to the
   * specified value.
   *
   * @param value value whose presence in this map is to be tested.
   * @return <tt>true</tt> if this map maps one or more keys to the
   *         specified value.
   */
  public boolean containsValue(Object value) {
    Entry tab[] = table;

    if (value==null) {
      for (int i = tab.length ; i-- > 0 ;)
	for (Entry e = tab[i] ; e != null ; e = e.next)
	  if (e.value==null)
	    return true;
    } else {
      for (int i = tab.length ; i-- > 0 ;)
	for (Entry e = tab[i] ; e != null ; e = e.next)
	  if (e.valid() && value.equals(e.value))
	    return true;
    }

    return false;
  }

  /**
   * Returns <tt>true</tt> if this map contains a mapping for the specified
   * key.
   * 
   * @return <tt>true</tt> if this map contains a mapping for the specified
   * key.
   * @param key key whose presence in this Map is to be tested.
   */
  public boolean containsKey(Object key) {
    Entry tab[] = table;
    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      Object currKey = null;
      for (Entry e = tab[index]; e != null; e = e.next) {
	currKey = e.keyRef.get();
	if (currKey == null)
	  continue;
	if (e.hash==hash && key.equals(currKey))
	  return true;
      }
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.keyRef==null)
	  return true;
    }

    return false;
  }

  /**
   * Returns the value to which this map maps the specified key.  Returns
   * <tt>null</tt> if the map contains no mapping for this key.  A return
   * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
   * map contains no mapping for the key; it's also possible that the map
   * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
   * operation may be used to distinguish these two cases.
   *
   * @return the value to which this map maps the specified key.
   * @param key key whose associated value is to be returned.
   */
  public Object get(Object key) {
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      Object currKey = null;
      for (Entry e = tab[index]; e != null; e = e.next) {
        currKey = e.keyRef.get(); // establish strong reference to current key
	if (currKey == null)
	  continue;
	if ((e.hash == hash) && key.equals(currKey))
	  return e.value;
      }
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.keyRef==null)
	  return e.value;
    }

    return null;
  }

  /**
   * If a key equal to the specified key exists in this map, a
   * reference is returned to the key object in this map.  If the key
   * being looked up is <code>null</code>, then <code>null</code> is
   * returned.  This method allows subclasses to get a handle on the
   * actual key objects used in this map, which is essential for
   * caches that wish to maintain strong references to a subset of key
   * objects when the keys are not unique (when two separate key
   * objects can be equal without being the same object).
   *
   * @param key the key to find in this map
   * @return a strong reference to the key object in this map that is equal
   * to the specified key, or <code>null</code> if either the specified key does
   * not exist in this map or the specified key is itself <code>null</code>
   */
  protected Object getKeyInMap(Object key) {
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      Object currKey = null;
      for (Entry e = tab[index]; e != null; e = e.next) {
	currKey = e.keyRef.get(); // establish strong reference to current key
	if (currKey == null)
	  continue;
	if ((e.hash == hash) && key.equals(currKey))
	  return currKey;
      }
    } else {
      return null;
    }

    return null;
  }

  /**
   * Returns the map entry corresponding to the specified key.  To
   * guarantee the validity of the map entry between the return of
   * this function and any use of the key, the specified parameter should
   * be an array of size 1, which will be filled in with the key object of the
   * map entry (if it exists).  This ensures that there will be a
   * strong reference to a valid key (when one exists) both during and
   * immediately after the invocation of this method.  The caller should
   * be sure to clear this array when the key is no longer needed.
   *
   * @param keyArr an array of length 1 containing the key whose map
   * entry is to be looked up
   * @return the map entry whose key is equal to the specified key, or
   * <code>null</code> if no such map entry exists
   */
  protected Map.Entry getEntry(Object[] keyArr) {
    Entry tab[] = table;
    Object key = keyArr[0];        // grab key to look up

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      Object currKey = null;
      for (Entry e = tab[index]; e != null; e = e.next) {
	currKey = e.keyRef.get();  // est. strong reference to current key
	if (currKey == null)
	  continue;
	if ((e.hash == hash) && key.equals(currKey)) {
	  keyArr[0] = currKey;
	  return e;
	}
      }
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.keyRef==null)
	  return e;
    }

    return null;
  }

  /**
   * Rehashes the contents of this map into a new
   * <tt>SofterHashMap</tt> instance with a larger capacity. This
   * method is called automatically when the number of keys in this
   * map exceeds its capacity and load factor.
   */
  private void rehash() {
    int oldCapacity = table.length;
    Entry oldMap[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    Entry newMap[] = new Entry[newCapacity];

    modCount++;
    threshold = (int)(newCapacity * loadFactor);
    table = newMap;

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (Entry old = oldMap[i] ; old != null ; ) {
	Entry e = old;
	old = old.next;

	int index = (e.hash & 0x7FFFFFFF) % newCapacity;
	e.next = newMap[index];
	newMap[index] = e;
      }
    }
  }

  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for this key, the old
   * value is replaced.
   *
   * @param key key with which the specified value is to be associated.
   * @param value value to be associated with the specified key.
   * @return previous value associated with specified key, or <tt>null</tt>
   *	       if there was no mapping for key.  A <tt>null</tt> return can
   *	       also indicate that the HashMap previously associated
   *	       <tt>null</tt> with the specified key.
   */
  public Object put(Object key, Object value) {
    if (debug)
      System.err.println("put: entered put");

    processQueue();

    if (debug)
      System.err.println("put: processed queue");

    // Makes sure the key is not already in the HashMap.
    Entry tab[] = table;
    int hash = 0;
    int index = 0;

    if (key != null) {

      if (debug)
	System.err.println("put: trying to put non-null key");

      hash = key.hashCode();
      index = (hash & 0x7FFFFFFF) % tab.length;
      Object currKey = null;
      for (Entry e = tab[index] ; e != null ; e = e.next) {
	currKey = e.keyRef.get();
	if (currKey == null)
	  continue;
	if ((e.hash == hash) && key.equals(currKey)) {

	  if (debug)
	    System.err.println("put: found existing key and replacing value");

	  Object old = e.value;
	  e.value = value;
	  return old;
	}
      }
    } else {

      if (debug)
	System.err.println("put: trying to put null key");

      for (Entry e = tab[0] ; e != null ; e = e.next) {
	if (e.keyRef == null) {

	  if (debug)
	    System.err.println("put: found existing null key and replacing " +
			       "value");

	  Object old = e.value;
	  e.value = value;
	  return old;
	}
      }
    }

    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded

      if (debug)
	System.err.println("put: rehashing table");

      rehash();

      tab = table;
      index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // Creates the new entry.
    if (debug)
      System.err.println("put: adding new entry");

    Entry e = new Entry(hash, getKey(key, queue), value, tab[index]);
    tab[index] = e;
    count++;
    return null;
  }

  private Object removeKeyRef(Reference keyRef) {
    if (debug)
      System.err.println("removeKeyRef: entered method");

    Entry tab[] = table;

    if (keyRef != null) {
      int hash = keyRef.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;

      for (Entry e = tab[index], prev = null; e != null;
	   prev = e, e = e.next) {
	/* Since this method is only called by processQueue, we know
	   that the specified keyRef is an actual key reference object
	   that is in this map, and thus we only need to test for
	   object equality.  */
	if ((e.hash == hash) && keyRef == e.keyRef) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[index] = e.next;

	  count--;
	  Object oldValue = e.value;
	  e.value = null;
	  return oldValue;
	}
      }
    } else {
      for (Entry e = tab[0], prev = null; e != null;
	   prev = e, e = e.next) {
	if (e.keyRef == null) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[0] = e.next;

	  count--;
	  Object oldValue = e.value;
	  e.value = null;
	  return oldValue;
	}
      }
    }

    return null;
  }

  /**
   * Removes the mapping for this key from this map if present.
   *
   * @param key key whose mapping is to be removed from the map.
   * @return previous value associated with specified key, or <tt>null</tt>
   *	       if there was no mapping for key.  A <tt>null</tt> return can
   *	       also indicate that the map previously associated <tt>null</tt>
   *	       with the specified key.
   */
  public Object remove(Object key) {
    processQueue();

    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;

      Object currKey = null;
      for (Entry e = tab[index], prev = null; e != null;
	   prev = e, e = e.next) {
	currKey = e.keyRef.get();
	if (currKey == null)
	  continue;
	if ((e.hash == hash) && key.equals(currKey)) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[index] = e.next;

	  count--;
	  Object oldValue = e.value;
	  e.value = null;
	  return oldValue;
	}
      }
    } else {
      for (Entry e = tab[0], prev = null; e != null;
	   prev = e, e = e.next) {
	if (e.keyRef == null) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[0] = e.next;

	  count--;
	  Object oldValue = e.value;
	  e.value = null;
	  return oldValue;
	}
      }
    }

    return null;
  }

  /**
   * Copies all of the mappings from the specified map to this one.
   * 
   * These mappings replace any mappings that this map had for any of the
   * keys currently in the specified Map.
   *
   * @param t Mappings to be stored in this map.
   */
  public void putAll(Map t) {
    Iterator i = t.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry e = (Map.Entry) i.next();
      put(e.getKey(), e.getValue());
    }
  }

  /**
   * Removes all mappings from this map.
   */
  public void clear() {
    processQueue();
    Entry tab[] = table;
    modCount++;
    for (int index = tab.length; --index >= 0; )
      tab[index] = null;
    count = 0;
  }

  /**
   * Returns a shallow copy of this <tt>SofterHashMap</tt> instance:
   * the keys and values themselves are not cloned.
   *
   * @return a shallow copy of this map.
   */
  public Object clone() {
    try { 
      SofterHashMap t = (SofterHashMap)super.clone();
      t.table = new Entry[table.length];
      for (int i = table.length ; i-- > 0 ; ) {
	t.table[i] = (table[i] != null) 
	  ? (Entry)table[i].clone() : null;
      }
      t.keySet = null;
      t.entrySet = null;
      t.values = null;
      t.modCount = 0;
      t.queue = new ReferenceQueue();
      return t;
    } catch (CloneNotSupportedException e) { 
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  // Views

  private transient Set keySet = null;
  private transient Set entrySet = null;
  private transient Collection values = null;

  /**
   * Returns a set view of the keys contained in this map.  The set is
   * backed by the map, so changes to the map are reflected in the set, and
   * vice-versa.  The set supports element removal, which removes the
   * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
   * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
   * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
   * <tt>addAll</tt> operations.
   *
   * @return a set view of the keys contained in this map.
   */
  public Set keySet() {
    if (keySet == null) {
      keySet = new AbstractSet() {
	  public Iterator iterator() {
	    return getHashIterator(KEYS);
	  }
	  public int size() {
	    return count;
	  }
	  public boolean contains(Object o) {
	    return containsKey(o);
	  }
	  public boolean remove(Object o) {
	    int oldSize = count;
	    SofterHashMap.this.remove(o);
	    return count != oldSize;
	  }
	  public void clear() {
	    SofterHashMap.this.clear();
	  }
	};
    }
    return keySet;
  }

  /**
   * Returns a collection view of the values contained in this map.  The
   * collection is backed by the map, so changes to the map are reflected in
   * the collection, and vice-versa.  The collection supports element
   * removal, which removes the corresponding mapping from this map, via the
   * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
   * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the values contained in this map.
   */
  /*
  public Collection values() {
    if (values==null) {
      values = new AbstractCollection() {
	  public Iterator iterator() {
	    return getHashIterator(VALUES);
	  }
	  public int size() {
	    return count;
	  }
	  public boolean contains(Object o) {
	    return containsValue(o);
	  }
	  public void clear() {
	    SofterHashMap.this.clear();
	  }
	};
    }
    return values;
  }
  */

  /**
   * Returns a collection view of the mappings contained in this map.  Each
   * element in the returned collection is a <tt>Map.Entry</tt>.  The
   * collection is backed by the map, so changes to the map are reflected in
   * the collection, and vice-versa.  The collection supports element
   * removal, which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
   * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the mappings contained in this map.
   * @see Map.Entry
   */
  public Set entrySet() {
    if (entrySet==null) {
      entrySet = new AbstractSet() {
	  public Iterator iterator() {
	    return getHashIterator(ENTRIES);
	  }

	  public boolean contains(Object o) {
	    if (!(o instanceof Map.Entry))
	      return false;
	    Map.Entry entry = (Map.Entry)o;
	    Object key = entry.getKey();
	    Entry tab[] = table;
	    int hash = (key==null ? 0 : key.hashCode());
	    int index = (hash & 0x7FFFFFFF) % tab.length;

	    for (Entry e = tab[index]; e != null; e = e.next)
	      if (e.hash==hash && e.equals(entry))
		return true;
	    return false;
	  }

	  public boolean remove(Object o) {
	    if (!(o instanceof Map.Entry))
	      return false;
	    Map.Entry entry = (Map.Entry)o;
	    Object key = entry.getKey();
	    Entry tab[] = table;
	    int hash = (key==null ? 0 : key.hashCode());
	    int index = (hash & 0x7FFFFFFF) % tab.length;

	    for (Entry e = tab[index], prev = null; e != null;
		 prev = e, e = e.next) {
	      if (e.hash==hash && e.equals(entry)) {
		modCount++;
		if (prev != null)
		  prev.next = e.next;
		else
		  tab[index] = e.next;

		count--;
		e.value = null;
		return true;
	      }
	    }
	    return false;
	  }

	  public boolean isEmpty() {
	    return !(iterator().hasNext());
	  }

	  public int size() {
	    //return count;

	    int j = 0;
	    for (Iterator i = iterator(); i.hasNext(); i.next()) j++;
	    return j;

	  }

	  public void clear() {
	    SofterHashMap.this.clear();
	  }
	};
    }

    return entrySet;
  }

  private Iterator getHashIterator(int type) {
    if (count == 0) {
      return emptyHashIterator;
    } else {
      return new HashIterator(type);
    }
  }

  /**
   * SofterHashMap collision list entry.
   */
  private static class Entry implements Map.Entry {
    int hash;
    Reference keyRef;
    Object value;
    Entry next;

    Entry(int hash, Reference keyRef, Object value, Entry next) {
      this.hash = hash;
      this.keyRef = keyRef;
      this.value = value;
      this.next = next;
    }

    protected Object clone() {
      return new Entry(hash, keyRef, value,
		       (next==null ? null : (Entry)next.clone()));
    }

    /**
     * Tests validity of this entry: either keyRef must be null (indicating
     * a <code>null</code> key), or its reference must be valid (i.e.,
     * non-<code>null</code>).
     */
    final boolean valid() {
      return (keyRef == null || keyRef.get() != null);
    }


    // Map.Entry Ops 

    public Object getKey() {
      return (keyRef == null ? null : keyRef.get());
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry))
	return false;

      if (!valid() || (o instanceof Entry && !((Entry)o).valid()))
	return false;

      Map.Entry e = (Map.Entry)o;

      return
	(keyRef==null ? e.getKey()==null :
	 keyRef.get().equals(e.getKey())) &&
	(value==null ? e.getValue()==null : value.equals(e.getValue()));
    }

    public int hashCode() {
      return hash ^ (value==null ? 0 : value.hashCode());
    }

    public String toString() {
      return getKey()+"="+value;
    }
  }

  // Types of Iterators
  private static final int KEYS = 0;
  private static final int VALUES = 1;
  private static final int ENTRIES = 2;

  private static EmptyHashIterator emptyHashIterator 
  = new EmptyHashIterator();
					     
  private static class EmptyHashIterator implements Iterator {
	
    EmptyHashIterator() {
	    
    }

    public boolean hasNext() {
      return false;
    }

    public Object next() {
      throw new NoSuchElementException();
    }
	
    public void remove() {
      throw new IllegalStateException();
    }

  }			
		    
  private class HashIterator implements Iterator {
    Entry[] table = SofterHashMap.this.table;
    int index = table.length;
    Entry entry = null;
    Entry lastReturned = null;
    Object nextKey = null;
    int type;

    /**
     * The modCount value that the iterator believes that the backing
     * List should have.  If this expectation is violated, the iterator
     * has detected concurrent modification.
     */
    private int expectedModCount = modCount;

    HashIterator(int type) {
      this.type = type;
    }

    public boolean hasNext() {
      Entry e = entry;
      int i = index;
      Entry t[] = table;

      if (debug)
	System.err.println("hasNext: starting entry = " + e);

      /* Use locals for faster loop iteration */
      boolean foundGoodEntry = false;
      while (!foundGoodEntry) {
	// look for good entry list
	while (e == null && i > 0) {
	  e = t[--i];

	  if (debug)
	    System.err.println("hasNext: looking for good list; curr list " +
			       "head: " + e);

	}
	// look for good entry in current list
	for (; e != null; e = e.next) {

	  if (debug)
	    System.err.println("hasNext: looking for good entry in list; " +
			       "current node: " + e);

	  nextKey = e.getKey(); // grab strong reference to next possible key
	  if (e.valid()) {

	    if (debug)
	      System.err.println("hasNext: yea! entry " + e + " is valid!");

	    foundGoodEntry = true;
	    break;
	  }
	}
	if (i == 0)
	  break;
      }

      if (debug)
	System.err.println("hasNext: finished looking for next node; " +
			   "curr entry is " + e + "; curr index: " + i);

      entry = e;
      index = i;
      return foundGoodEntry;
    }

    public Object next() {
      if (modCount != expectedModCount)
	throw new ConcurrentModificationException();
      if (hasNext()) {
	Entry e = lastReturned = entry;
	entry = e.next;
	return (type == KEYS ?
		(e.keyRef == null ? null : e.keyRef.get()) :
		(type == VALUES ? e.value : e));
      }
      throw new NoSuchElementException();
    }

    public void remove() {
      if (lastReturned == null)
	throw new IllegalStateException();
      if (modCount != expectedModCount)
	throw new ConcurrentModificationException();

      Entry[] tab = SofterHashMap.this.table;
      int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

      for (Entry e = tab[index], prev = null; e != null;
	   prev = e, e = e.next) {
	if (e == lastReturned) {
	  modCount++;
	  expectedModCount++;
	  if (prev == null)
	    tab[index] = e.next;
	  else
	    prev.next = e.next;
	  count--;
	  lastReturned = null;
	  return;
	}
      }
      throw new ConcurrentModificationException();
    }
  }

  /**
     * Save the state of the <tt>SofterHashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *		   bucket array) is emitted (int), followed  by the
     *		   <i>size</i> of the HashMap (the number of key-value
     *		   mappings), followed by the key (Object) and value (Object)
     *		   for each key-value mapping represented by the HashMap
     * The key-value mappings are emitted in no particular order.
     */

  /*
  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException
  {
    // Write out the threshold, loadfactor, and any hidden stuff
    s.defaultWriteObject();

    // Write out number of buckets
    s.writeInt(table.length);

    // Write out size (number of Mappings)
    s.writeInt(count);

    // Write out keys and values (alternating)
    for (int index = table.length-1; index >= 0; index--) {
      Entry entry = table[index];

      while (entry != null) {
	s.writeObject(entry.getKey());
	s.writeObject(entry.value);
	entry = entry.next;
      }
    }
  }
  */

  /*
  private static final long serialVersionUID = 362498820763181265L;
  */

    /**
     * Reconstitute the <tt>SofterHashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
  /*
  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException
  {
    // Read in the threshold, loadfactor, and any hidden stuff
    s.defaultReadObject();

    // Read in number of buckets and allocate the bucket array;
    int numBuckets = s.readInt();
    table = new Entry[numBuckets];

    // Read in size (number of Mappings)
    int size = s.readInt();

    // create new reference queue
    queue = new ReferenceQueue();

    // Read the keys and values, and put the mappings in the HashMap
    for (int i=0; i<size; i++) {
      Object key = s.readObject();
      Object value = s.readObject();
      put(key, value);
    }
  }
  */

  int capacity() {
    return table.length;
  }

  float loadFactor() {
    return loadFactor;
  }

  /**
   * Returns a <code>SoftKey</code> or <code>WeakKey</code>
   * containing the specified object, depending on the reference type of
   * this <code>SofterHashMap</code> object.
   */
  private Reference getKey(Object obj) {
    if (obj == null)
      return null;

    switch (referenceType) {
    case soft:
      return new SoftKey(obj);
    case weak:
      return new WeakKey(obj);
    default:
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Returns a <code>SoftKey</code>, <code>WeakKey</code>
   * or <code>PhantomKey</code> constructed on the specified
   * reference queue and containing the specified object, depending on
   * the reference type of this <code>SofterHashMap</code> object.
   */
  private Reference getKey(Object obj, ReferenceQueue q) {
    if (obj == null)
      return null;

    switch (referenceType) {
    case soft:
      return new SoftKey(obj, q);
    case weak:
      return new WeakKey(obj, q);
    case phantom:
      return new PhantomKey(obj, q);
    default:
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Returns a string representation of this map of the form
   * <tt>{key1=value1, key2=value2, ..., keyN=valueN}</tt>
   * for the <tt>N</tt> key-value pairs of this map.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    Iterator i = entrySet().iterator();
    
    buf.append("{");
    while (i.hasNext()) {
      Entry e = (Entry)i.next();
      buf.append(e.getKey() + "=" + e.getValue());
      if (i.hasNext())
	buf.append(", ");
    }
    buf.append("}");
    return buf.toString();
  }

  private final static String[] usageMsg = {
    "usage: <type> <max> <remove>"
  };

  private final static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
    System.exit(1);
  }

  /** A test driver for this class. */
  public static void main(String[] args) {
    if (args.length != 3)
      usage();

    int type = Integer.parseInt(args[0]);
    if (!(type >= 0 && type < SofterHashMap.numTypes)) {
      System.err.println("bad type argument: " + args[0]);
      usage();
    }

    SofterHashMap map = new SofterHashMap(type);


    int max = Integer.parseInt(args[1]);
    int lessThanMax = Integer.parseInt(args[2]);
    if (lessThanMax >= max)
      usage();

    for (int i = 0; i < max; i++)
      map.put(new Integer(i), new Integer(i));

    System.err.println("\ndone putting stuff into map\n");

    System.err.println("containsKey(new Integer(" + (max - 1) + ")) = " +
		       map.containsKey(new Integer(max - 1)) + "\n");

    //System.err.println("\nsize = " + map.size() + "\n");

    System.err.println(map);


    Iterator keys = map.keySet().iterator();
    int i = max - 1;
    Object key = null;
    for (i = 0 ; i < lessThanMax && keys.hasNext(); i++) {
      key = keys.next();
    }
    System.err.println("\nremoving the " + lessThanMax +
		       "th key in the iteration: " + key + "\n");
    keys.remove();

    System.err.println(map);

    System.err.println("\ngarbage collecting");
    System.gc();

    System.err.println("\n\nmap:\n");

    System.err.println(map);
  }
}
