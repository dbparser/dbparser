/*
 * This is a modified version of HashMap.java from Sun Microsystems.
 * Modified by Dan Bikel, 06/15/2001 (or 01/06/15, in Sun's notation).
 * We needed to be able to access the objects used as keys in a map,
 * which the default implementation does not allow for (in constant time,
 * anyway).  Also, we added diagnostic methods to determine the average
 * and maximum bucket sizes, which is useful (and we would argue, necessary)
 * when developing/debugging hash functions.  Finally, we wanted to
 * have a specific map of arbitrary objects to primitives, as specified
 * by the interface <code>danbikel.util.MapToPrimitive</code>.
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
 * Abstract class implementing all appropriate putting and getting methods
 * for the <code>MapToPrimitive</code> interface; concrete subclasses
 * need only override {@link #getNewEntry(int,Object,HashMapPrimitive.Entry)}
 * to return an object that is a concrete subclass of
 * {@link HashMapPrimitive.Entry}.  The following is Sun's <code>HashMap</code>
 * API documentation.<p>
 *
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

abstract public class HashMapPrimitive extends AbstractMapToPrimitive
  implements FlexibleMap, Cloneable, java.io.Serializable {

  // constants
  /**
   * The default load factor, <tt>0.75f</tt>.
   */
  protected final static float DEFAULT_LOAD_FACTOR = 0.75f;

  /**
   * The default size, <tt>11</tt>.
   */
  protected final static int DEFAULT_SIZE = 11;

  /**
   * The hash table data.
   */
  protected transient Entry table[];

  /**
   * The total number of mappings in the hash table.
   */
  protected transient int count;

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
  protected transient int modCount = 0;

  /**
   * Constructs a new, empty map with the specified initial
   * capacity and the specified load factor.
   *
   * @param      initialCapacity   the initial capacity of the HashMap.
   * @param      loadFactor        the load factor of the HashMap
   * @throws     IllegalArgumentException  if the initial capacity is less
   *               than zero, or if the load factor is nonpositive.
   */
  public HashMapPrimitive(int initialCapacity, float loadFactor) {
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
   * and {@link #DEFAULT_LOAD_FACTOR default load factor}.
   *
   * @param   initialCapacity   the initial capacity of the HashMap.
   * @throws    IllegalArgumentException if the initial capacity is less
   *              than zero.
   */
  public HashMapPrimitive(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Constructs a new, empty map with {@link #DEFAULT_SIZE a default capacity}
   * and {@link #DEFAULT_LOAD_FACTOR load factor}.
   */
  public HashMapPrimitive() {
    this(DEFAULT_SIZE, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Constructs a new map with the same mappings as the given map.  The
   * map is created with a capacity of twice the number of mappings in
   * the given map or {@link #DEFAULT_SIZE the default size} (whichever
   * is greater), and {@link #DEFAULT_LOAD_FACTOR a default load factor}.
   *
   * @param t the map whose mappings are to be placed in this map.
   */
  public HashMapPrimitive(Map t) {
    this(Math.max(2*t.size(), DEFAULT_SIZE), DEFAULT_LOAD_FACTOR);
    putAll(t);
  }

  /**
   * Returns the number of key-value mappings in this map.
   *
   * @return the number of key-value mappings in this map.
   */
  public int size() {
    return count;
  }

  /**
   * Returns <tt>true</tt> if this map contains no key-value mappings.
   *
   * @return <tt>true</tt> if this map contains no key-value mappings.
   */
  public boolean isEmpty() {
    return count == 0;
  }

  /**
   * Returns <tt>true</tt> if this map maps one or more keys to the
   * specified value.
   *
   * @param value value whose presence in this map is to be tested.
   * @return <tt>true</tt> if this map maps one or more keys to the
   *         specified value.
   *
   * @throws ClassCastException if the specified value is not an
   * instance of <code>Integer</code>
   */
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
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
      for (Entry e = tab[index]; e != null; e = e.next)
	if (e.hash==hash && key.equals(e.key))
	  return true;
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
	  return true;
    }

    return false;
  }

  public Object get(Object key, int hashCode) {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
    /*
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
	  //return new Integer(e.value);
          return null;
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
	  //return new Integer(e.value);
          return null;
    }

    return null;
    */
  }

  public MapToPrimitive.Entry getEntry(Object key) {
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e;
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e;
    }
    return null;
  }

  public MapToPrimitive.Entry getEntry(Object key, int hashCode) {
    Entry tab[] = table;

    if (key != null) {
      int index = (hashCode & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index]; e != null; e = e.next)
	if ((e.hash == hashCode) && key.equals(e.key))
          return e;
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e;
    }
    return null;
  }

  public MapToPrimitive.Entry getEntryMRU(Object key) {
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index], prev = null; e != null;
           prev = e, e = e.next) {
	if ((e.hash == hash) && key.equals(e.key)) {
          if (prev != null) {
            prev.next = e.next;
            e.next = tab[index];
            tab[index] = e;
          }
	  return e;
	}
      }
    } else {
      for (Entry e = tab[0], prev = null; e != null;
           prev = e, e = e.next) {
	if (e.key==null) {
          if (prev != null) {
            prev.next = e.next;
            e.next = tab[0];
            tab[0] = e;
          }
	  return e;
	}
      }
    }

    return null;
  }

  public MapToPrimitive.Entry getEntryMRU(Object key, int hashCode) {
    Entry tab[] = table;

    if (key != null) {
      int index = (hashCode & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index], prev = null; e != null;
           prev = e, e = e.next) {
	if ((e.hash == hashCode) && key.equals(e.key)) {
          if (prev != null) {
            prev.next = e.next;
            e.next = tab[index];
            tab[index] = e;
          }
	  return e;
	}
      }
    } else {
      for (Entry e = tab[0], prev = null; e != null;
           prev = e, e = e.next) {
	if (e.key==null) {
          if (prev != null) {
            prev.next = e.next;
            e.next = tab[0];
            tab[0] = e;
          }
	  return e;
	}
      }
    }

    return null;
  }

  /**
  * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
  * with a larger capacity. This method is called automatically when the
  * number of keys in this map exceeds its capacity and load factor.
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
    throw new UnsupportedOperationException();
    /*
    // Makes sure the key is not already in the HashMap.
    Entry tab[] = table;
    int hash = 0;
    int index = 0;

    int intVal = ((Integer)value).intValue();

    if (key != null) {
      hash = key.hashCode();
      index = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index] ; e != null ; e = e.next) {
	if ((e.hash == hash) && key.equals(e.key)) {
	  Object old = e.getValue();
	  //e.value = intVal;
	  return old;
	}
      }
    } else {
      for (Entry e = tab[0] ; e != null ; e = e.next) {
	if (e.key == null) {
	  Object old = e.getValue();
	  //e.value = intVal;
	  return old;
	}
      }
    }

    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // Creates the new entry.
    Entry e = new Entry(hash, key, intVal, tab[index]);
    tab[index] = e;
    count++;
    return null;
    */
  }

  // byte-specific methods
  public byte put(Object key, int index, byte value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, byte addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  // char-specific methods
  public char put(Object key, int index, char value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  // short-specific methods
  public short put(Object key, int index, short value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, short addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  // int-specific methods
  public int put(Object key, int index, int value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, int addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  // long-specific methods
  public long put(Object key, int index, long value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, long addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  // float-specific methods
  public float put(Object key, int index, float value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, float addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  // double-specific methods
  public double put(Object key, int index, double value) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key))
          return e.set(index, value);
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null)
          return e.set(index, value);
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    return e.set(index, value);
  }

  public void add(Object key, int index, double addend) {
    Entry[] tab = table;

    int hash = 0;
    int tableIndex = 0;

    // first, try for existing entry
    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next)
	if ((e.hash == hash) && key.equals(e.key)) {
          e.add(index, addend);
          return;
	}
    } else {
      for (Entry e = tab[0]; e != null; e = e.next)
	if (e.key==null) {
          e.add(index, addend);
          return;
	}
    }


    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }
    // new beginning of linked list
    Entry e = getNewEntry(hash, key, tab[tableIndex]);
    tab[tableIndex] = e;
    count++;
    e.add(index, addend);
  }

  private void addEntry(Entry entryToAdd) {
    // Makes sure the key is not already in the HashMap.
    Entry tab[] = table;
    int hash = 0;
    int tableIndex = 0;

    Object key = entryToAdd.key;

    if (key != null) {
      hash = key.hashCode();
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[tableIndex]; e != null; e = e.next) {
	if ((e.hash == hash) && key.equals(e.key)) {
          e.copyValuesFrom(entryToAdd);
          return;
	}
      }
    } else {
      for (Entry e = tab[0]; e != null; e = e.next) {
	if (e.key == null) {
          e.copyValuesFrom(entryToAdd);
          return;
	}
      }
    }

    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      tableIndex = (hash & 0x7FFFFFFF) % tab.length;
    }

    // make this entry be the new beginning of the singly-linked list
    entryToAdd.next = tab[tableIndex];
    tab[tableIndex] = entryToAdd;
    count++;
  }

  /**
   * Adds the specified entry to the beginning of the singly-linked list
   * at its bucket index (indicating it is the most-recently used entry).
   * <b>Warning</b>: This method does not check whether the specified entry's
   * key already is in the map; subclasses should only invoke this method
   * when it has already been ascertained that the entry's key is not in
   * the map.
   *
   * @param entry the entry to add as most-recently used in its bucket
   */
  protected void addEntryMRU(Entry entry) {
    Entry tab[] = table;
    int index = (entry.hash & 0x7FFFFFFF) % tab.length;
    entry.next = tab[index];
    tab[index] = entry;
  }

  /*
  public final void add(Object key) {
    add(key, 1);
  }
  */

  /*
  public final void add(Object key, int value) {
    // Makes sure the key is not already in the HashMap.
    Entry tab[] = table;
    int hash = 0;
    int index = 0;

    if (key != null) {
      hash = key.hashCode();
      index = (hash & 0x7FFFFFFF) % tab.length;
      for (Entry e = tab[index] ; e != null ; e = e.next) {
	if ((e.hash == hash) && key.equals(e.key)) {
	  //e.value += value;
          return;
	}
      }
    } else {
      for (Entry e = tab[0] ; e != null ; e = e.next) {
	if (e.key == null) {
	  e.value += value;
          return;
	}
      }
    }

    modCount++;
    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab = table;
      index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // Creates the new entry.
    Entry e = new Entry(hash, key, value, tab[index]);
    tab[index] = e;
    count++;
  }
  */

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
    Entry tab[] = table;

    if (key != null) {
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;

      for (Entry e = tab[index], prev = null; e != null;
	   prev = e, e = e.next) {
	if ((e.hash == hash) && key.equals(e.key)) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[index] = e.next;

	  count--;
	  //Object oldValue = e.getValue();
	  //e.value = null;
          //return oldValue;
	  return null;
	}
      }
    } else {
      for (Entry e = tab[0], prev = null; e != null;
	   prev = e, e = e.next) {
	if (e.key == null) {
	  modCount++;
	  if (prev != null)
	    prev.next = e.next;
	  else
	    tab[0] = e.next;

	  count--;
	  //Object oldValue = e.getValue();
	  //e.value = null;
          //return oldValue;
	  return null;
	}
      }
    }

    return null;
  }

  protected MapToPrimitive.Entry removeLRU(Object key) {
    return removeLRU(key.hashCode());
  }

  /**
   * Removes the last entry at the specified bucket index, if that bucket
   * contains at least one entry.
   *
   * @param hashCode the hashCode of an object whose bucket is to be emptied
   * of its least-recently-used entry
   * @return the removed entry, or <code>null</code> if no entry was removed
   */
  protected MapToPrimitive.Entry removeLRU(int hashCode) {
    Entry tab[] = table;
    int index = (hashCode & 0x7FFFFFFF) % tab.length;
    for (Entry e = tab[index], prev = null; e != null;
         prev = e, e = e.next) {
      if (e.next == null) {
        Entry oldEntry = e;
        if (prev != null)
          prev.next = null;
        else
          tab[index] = null;
        return e;
      }
    }
    return null;
  }

  public void removeRandom(int bucketIndex) {
    if (count == 0)
      return;

    Entry tab[] = table;
    int nonEmptyBucketIndex = bucketIndex;
    while (tab[nonEmptyBucketIndex] == null)
      nonEmptyBucketIndex++;
    count--;
    modCount++;
    tab[nonEmptyBucketIndex] = tab[nonEmptyBucketIndex].next;
  }

  public void putAll(HashMapPrimitive t) {
    Iterator i = t.entrySet().iterator();
    while (i.hasNext()) {
      Entry e = (Entry)i.next();
      addEntry((Entry)e.clone());
    }
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
    Entry tab[] = table;
    modCount++;
    for (int index = tab.length; --index >= 0; )
      tab[index] = null;
    count = 0;
  }

  /**
   * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
   * values themselves are not cloned.
   *
   * @return a shallow copy of this map.
   */
  public Object clone() {
    try {
      HashMapPrimitive t = (HashMapPrimitive)super.clone();
      t.table = new Entry[table.length];
      for (int i = table.length ; i-- > 0 ; ) {
	t.table[i] = (table[i] != null)
	  ? (Entry)table[i].clone() : null;
      }
      t.keySet = null;
      t.entrySet = null;
      t.values = null;
      t.modCount = 0;
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
	    HashMapPrimitive.this.remove(o);
	    return count != oldSize;
	  }
	  public void clear() {
	    HashMapPrimitive.this.clear();
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
	    HashMapPrimitive.this.clear();
	  }
	};
    }
    return values;
  }

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
   * @see java.util.Map.Entry
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
		//e.value = null;
		return true;
	      }
	    }
	    return false;
	  }

	  public int size() {
	    return count;
	  }

	  public void clear() {
	    HashMapPrimitive.this.clear();
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

  abstract protected Entry
    getNewEntry(int hash, Object key, Entry next);

  /**
   * HashMap collision list entry.
   */
  abstract public static class Entry extends AbstractMapToPrimitive.Entry
  implements Externalizable {
    transient protected int hash;
    transient protected Entry next;

    public Entry() {}

    protected Entry(int hash, Object key, Entry next) {
      this.hash = hash;
      this.key = key;
      this.next = next;
    }

    /**
     * Returns a new copy of this type of map entry.
     */
    protected abstract Object clone();

    // MapToPrimitive.Entry ops
    public boolean replaceKey(Object key) {
      if (this.key != key && this.key.equals(key)) {
        this.key = key;
        return true;
      }
      return false;
    }

    // Map.Entry Ops

    public Object getValue() {
      throw new UnsupportedOperationException();
    }

    public Object setValue(Object value) {
      throw new UnsupportedOperationException();
    }

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract String toString();

    /**
     * Copies the values from the specified entry to this entry.
     *
     * @throws ClassCastException if the specified entry is not of the same
     * run-time type as this entry
     */
    public abstract void copyValuesFrom(Entry copyFrom);

    public void writeExternal(java.io.ObjectOutput out) throws IOException {
      out.writeObject(key);
      writeValues(out);
    }

    public void readExternal(java.io.ObjectInput in)
    throws IOException, ClassNotFoundException {
      key = in.readObject();
      hash = key.hashCode();
      readValues(in);
    }

    protected abstract void writeValues(java.io.ObjectOutput out)
      throws IOException;
    protected abstract void readValues(java.io.ObjectInput in)
      throws IOException, ClassNotFoundException;
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
    Entry[] table = HashMapPrimitive.this.table;
    int index = table.length;
    Entry entry = null;
    Entry lastReturned = null;
    int type;

    /**
     * The modCount value that the iterator believes that the backing
     * List should have.  If this expectation is violated, the iterator
     * has detected concurrent modification.
     */
    private int expectedModCount = modCount;

    HashIterator(int type) {
      this.type = type;
      if (type == VALUES)
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
      Entry e = entry;
      int i = index;
      Entry t[] = table;
      /* Use locals for faster loop iteration */
      while (e == null && i > 0)
	e = t[--i];
      entry = e;
      index = i;
      return e != null;
    }

    public Object next() {
      if (modCount != expectedModCount)
	throw new ConcurrentModificationException();

      Entry et = entry;
      int i = index;
      Entry t[] = table;

      /* Use locals for faster loop iteration */
      while (et == null && i > 0)
	et = t[--i];

      entry = et;
      index = i;
      if (et != null) {
	Entry e = lastReturned = entry;
	entry = e.next;
	return type == KEYS ? e.key : (type == VALUES ? e.getValue() : e);
      }
      throw new NoSuchElementException();
    }

    public void remove() {
      if (lastReturned == null)
	throw new IllegalStateException();
      if (modCount != expectedModCount)
	throw new ConcurrentModificationException();

      Entry[] tab = HashMapPrimitive.this.table;
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
      for (Entry e = tab[index]; e != null; e = e.next) {
        System.err.println(e);
      }
      throw new ConcurrentModificationException();
    }
  }

  /**
   * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
   * serialize it).
   *
   * @serialData The <i>capacity</i> of the HashMap (the length of the
   *		   bucket array) is emitted (int), followed  by the
   *		   <i>size</i> of the HashMap (the number of key-value
   *		   mappings), followed by the key (Object) and value (Object)
   *		   for each key-value mapping represented by the HashMap
   * The key-value mappings are emitted in no particular order.
   */
  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException {
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
	s.writeObject(entry);
	entry = entry.next;
      }
    }
  }

  /**
   * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
   * deserialize it).
   */
  private void readObject(java.io.ObjectInputStream s)
  throws IOException, ClassNotFoundException {
    // Read in the threshold, loadfactor, and any hidden stuff
    s.defaultReadObject();

    // Read in number of buckets and allocate the bucket array;
    int numBuckets = s.readInt();
    table = new Entry[numBuckets];

    // Read in size (number of Mappings)
    int size = s.readInt();

    // Read the keys and values, and put the mappings in the HashMap
    for (int i=0; i<size; i++) {
      Object entry = s.readObject();
      addEntry((Entry)entry);
    }
  }

  public int getCapacity() {
    return table.length;
  }

  public float getLoadFactor() {
    return loadFactor;
  }

  public String getStats() {
    int maxBucketSize = 0, numNonZeroBuckets = 0;
    for (int index = table.length-1; index >= 0; index--) {
      Entry entry = table[index];

      int numItemsInBucket = 0;
      for ( ; entry != null; entry = entry.next, numItemsInBucket++)
        ;
      if (numItemsInBucket > maxBucketSize)
        maxBucketSize = numItemsInBucket;
      if (numItemsInBucket > 0)
        numNonZeroBuckets++;
    }
    return "size: " + size() + "; load factor: " + getLoadFactor() +
      ";\n\tNo. of buckets: " + getCapacity() +
      " (max.: " + maxBucketSize +
      "; avg.: " + (size() / (float)numNonZeroBuckets) +
      "; non-zero: " + numNonZeroBuckets + ")";
  }
}
