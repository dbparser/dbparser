package danbikel.lisp;

import java.io.*;
import java.util.*;

/**
 * Stores a list of <code>Sexp</code> objects, which are either symbols
 * or themselves lists.
 */
public class SexpList extends Sexp implements Externalizable {
  // constants
  /**
   * An immutable object to represent the empty list.
   */
  public static final SexpList emptyList = new EmptyList();

  private static final class EmptyList extends SexpList {
    public EmptyList() { super(0); }

    public SexpList add(Sexp sexp) {
      throw new UnsupportedOperationException();
    }
    public boolean addAll(SexpList elementsToAdd) {
      throw new UnsupportedOperationException();
    }
    public boolean addAll(int index, SexpList elementsToAdd) {
      throw new UnsupportedOperationException();
    }
    public Sexp deepCopy() {
      return this;
    }
    public void ensureCapacity(int minCapacity) {}
    public void trimToSize() {}
    //public boolean equals(Object o) { return this == o; }
    public Sexp first() {
      throw new UnsupportedOperationException();
    }
    public Sexp last() {
      throw new UnsupportedOperationException();
    }
    public Sexp get(int index) {
      throw new UnsupportedOperationException();
    }
    public String toString() { return "()"; }

    public Object readResolve() throws ObjectStreamException {
      return SexpList.emptyList;
    }
  };



  /**
   * A simple canonicalization method that returns the unique object
   * representing the empty list if the specified list contains no elements.
   */
  public static final SexpList getCanonical(SexpList list) {
    return ((list.size == 0) ? emptyList : list);
  }

  // data members
  /** The array for storage of list items. */
  private Sexp[] items;
  /** The number of items in this list. */
  private int size;

  /**
   * Constructs a <code>SexpList</code> with the default initial capacity.
   */
  public SexpList() {
    items = new Sexp[2];
    size = 0;
  }

  /**
   * Constructs a <code>SexpList</code> with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity of this list
   */
  public SexpList(int initialCapacity) {
    items = new Sexp[initialCapacity];
    size = 0;
  }

  /**
   * Constructs a <code>SexpList</code> whose initial elements are those
   * of <code>initialElements</code>.  The initial capacity of this
   * newly-constructed list will be exactly that of the specified list.
   * Note that the elements of <code>initialElements</code> are not cloned.
   *
   * @param initialElements a list of the initial elements of this new list
   */
  public SexpList(SexpList initialElements) {
    this.size = initialElements.size;
    items = new Sexp[size];
    System.arraycopy(initialElements.items, 0, this.items, 0, size);
  }

  /**
   * Constructs a <code>SexpList</code> whose initial elements are those
   * of <code>initialElements</code>.  Note that the elements of
   * <code>initialElements</code> are not cloned.  It is the responsibility
   * of the caller that <code>initialElements</code> only contain objects
   * of type <code>Sexp</code>.
   *
   * @param initialElements a list of the initial elements of this new list
   */
  public SexpList(List initialElements) {
    size = initialElements.size();
    items = new Sexp[size];
    Iterator it = initialElements.iterator();
    for (int i = 0; it.hasNext(); i++) {
      items[i] = (Sexp)it.next();
    }
  }

  /**
   * Appends <code>sexp</code> to the end of this list.
   *
   * @param sexp the S-expression to append
   * @return this <code>SexpList</code> object
   */
  public SexpList add(Sexp sexp) {
    ensureCapacity(size + 1);
    items[size++] = sexp;
    return this;
  }

  /**
   * Adds <code>sexp</code> at position <code>index</code>, shifting all
   * elements to the right by one position to make room (an O(n) operation).
   *
   * @param index the index at which to add the specified S-expression
   * @param sexp the S-expression to add
   * @return this <code>SexpList</code> object
   */
  public SexpList add(int index, Sexp sexp) {
    if (index == size)
      return add(sexp);
    ensureCapacity(size + 1);
    System.arraycopy(items, index, items, index+1, size - index);
    items[index] = sexp;
    size++;
    return this;
  }

  /**
   * Appends all the elements in <code>elementsToAdd</code> to this list.
   *
   * @return whether this list was modified
   */
  public boolean addAll(SexpList elementsToAdd) {
    if (elementsToAdd.size == 0)
      return false;
    ensureCapacity(this.size + elementsToAdd.size);
    System.arraycopy(elementsToAdd.items, 0, items, size, elementsToAdd.size);
    size += elementsToAdd.size;
    return true;
  }

  /**
   * Adds all the elements in <code>elementsToAdd</code> to this list
   * at the specified index.
   *
   * @param index the index at which to add all the elements of
   * <code>elementsToAdd</code>
   * @param elementsToAdd the elements to add at <code>index</code>
   * @return whether this list was modified
   */
  public boolean addAll(int index, SexpList elementsToAdd) {
    if (elementsToAdd.size == 0)
      return false;
    ensureCapacity(this.size + elementsToAdd.size);
    System.arraycopy(items, index, items, index + elementsToAdd.size,
                     size - index);
    System.arraycopy(elementsToAdd.items, 0, items, index, elementsToAdd.size);
    size += elementsToAdd.size;
    return true;
  }

  /**
   * Gets the <code>Sexp</code> at the specified index.
   */
  public Sexp get(int index) {
    return items[index];
  }

  /**
   * Replaces the element at <code>index</code> with <code>element</code>.
   *
   * @return the value of the element that used to be at <code>index</code>
   */
  public Sexp set(int index, Sexp element) {
    Sexp former = items[index];
    items[index] = element;
    return former;
  }

  /**
   * Removes the element at <code>index</code>.
   *
   * @return the element that was removed from the list
   * @exception IndexOutOfBoundsException if the index is out of range
   * (<code>(index < 0 || index >= size())</code>)
   */
  public Sexp remove(int index) {
    Sexp removed = items[index];
    System.arraycopy(items, index+1, items, index, size - (index + 1));
    size--;
    return removed;
  }

  /**
   * Removes all elements from this list.
   */
  public void clear() {
    size = 0;
  }

  /**
   * Increases the number of elements that this list can hold, if necessary, to
   * be at least <code>minCapacity</code>.
   */
  public void ensureCapacity(int minCapacity) {
    if (items.length < minCapacity) {
      int newCapacity = size * 2;
      if (newCapacity < minCapacity)
        newCapacity = minCapacity;
      Sexp[] newItems = new Sexp[newCapacity];
      if (size > 0)
        System.arraycopy(items, 0, newItems, 0, size);
      items = newItems;
    }
  }

  /**
   * Causes the capacity of this list to be its size.  An application can
   * use this method to reduce the memory footprint of <code>SexpList</code>
   * objects.
   */
  public void trimToSize() {
    if (size < items.length) {
      Sexp[] newItems = new Sexp[size];
      System.arraycopy(items, 0, newItems, 0, size);
      items = newItems;
    }
  }

  /**
   * Invokes <code>trimToSize</code> not only on this list, but recursively
   * invokes this method on all elements of this list, thereby trimming
   * every list in the implicit tree of this list.  This method will never
   * return if this list represents a cyclic graph rather than a tree.
   */
  public void trimToSizeRecursive() {
    int thisSize = size();
    for (int i = 0; i < thisSize; i++)
      if (get(i).isList())
	get(i).list().trimToSizeRecursive();
    trimToSize();
  }

  /**
   * Returns the number of elements in this list.
   */
  public int length() {
    return size;
  }
  /**
   * Returns the number of elements in this list.
   */
  public int size() {
    return size;
  }

  /**
   * Returns the first element of this list (identical to calling
   * <code>get(0)</code>).
   */
  public Sexp first() {
    return items[0];
  }

  /**
   * Returns the last element of this list (identical to calling
   * <code>get(size() - 1)</code>).
   */
  public Sexp last() {
    return items[size - 1];
  }

  /**
   * Finds the index of the specified S-expresion.
   * @param toFind the S-expression to find in this list
   * @return the index of <code>toFind</code> in this list, or -1 if
   * it does not appear in this list
   */
  public int indexOf(Sexp toFind) {
    for (int i = 0; i < size; i++) {
      if (items[i].equals(toFind))
        return i;
    }
    return -1;
  }

  /**
   * Returns whether the specified S-expression is an element of this list.
   * @param toFind the S-expression to find in this list
   */
  public boolean contains(Sexp toFind) {
    return indexOf(toFind) != -1;
  }

  /**
   * Performs an in-place reversal of the elements in this list.
   *
   * @return this <code>SexpList</code> object
   */
  public SexpList reverse() {
    Sexp temp;
    int midPoint = size / 2;
    int otherIndex;
    for (int i = 0; i < midPoint; i++) {
      otherIndex = size - i - 1;
      temp = items[i];
      items[i] = items[otherIndex];
      items[otherIndex] = temp;
    }
    return this;
  }

  // convenience methods

  /**
   * Returns the symbol at the specified index.  Calling this method is
   * equivalent to calling
   * <pre>get(index).symbol()</pre>
   *
   * @param index the index of the symbol to retrieve
   * @return the symbol at <code>index</code>
   * @exception ClassCastException if the element at <code>index</code> is
   * not of type <code>Symbol</code>
   */
  public Symbol symbolAt(int index) {
    return get(index).symbol();
  }

  /**
   * Returns the list at the specified index.  Calling this method is
   * equivalent to calling
   * <pre>get(index).list()</pre>
   *
   * @param index the index of the symbol to retrieve
   * @return the symbol at <code>index</code>
   * @exception ClassCastException if the element at <code>index</code> is
   * not of type <code>SexpList</code>
   */
  public SexpList listAt(int index) {
    return get(index).list();
  }

  /**
   * This convenience method gets the symbol that is the first element of the
   * list that is the element at <code>index</code>.  Invoking
   * this method is equivalent to the following: <pre>
   * get(index).list().get(0).symbol(); </pre> This method is useful for when
   * <code>Sexp</code>s are used to represent labeled trees, where the first
   * element of a list represents a node label and subsequent elements
   * represent the children of the node with that label.
   * @param index the index of the element whose first symbol this method
   * retrieves
   * @exception ClassCastException if the element at <code>index</code> is not
   * a list or if the first element of the list at <code>index</code> is not
   * of type <code>Symbol</code>
   * @exception IndexOutOfBoundsException if this list has no element at the
   * specified index, or if the list that is the element at <code>index</code>
   * has no elements
   */
  public Symbol getChildLabel(int index) {
    return get(index).list().get(0).symbol();
  }

  /**
   * This convenience method replaces the first element of the list that is the
   * element at <code>index</code> to be the symbol <code>newLabel</code>.
   * Invoking this method is equivalent to the following: <pre>
   * get(index).list().set(0, newLabel); </pre> This method is useful for when
   * <code>Sexp</code>s are used to represent labeled trees, where the first
   * element of a list represents a node label and subsequent elements
   * represent the children of the node with that label.
   * @param index the index of the element whose first element is to be replaced
   * @exception ClassCastException if the element at <code>index</code> is not
   * a list
   * @exception IndexOutOfBoundsException if this list has no element at the
   * specified index, or if the list that is the element at <code>index</code>
   * has no elements
   */
  public void setChildLabel(int index, Symbol newLabel) {
    get(index).list().set(0, newLabel);
  }

  public Sexp deepCopy() {
    int thisSize = size();
    SexpList listCopy = new SexpList(thisSize);
    for (int i = 0; i < thisSize; i++)
      listCopy.add(get(i).deepCopy());
    return listCopy;
  }

  /** Returns <code>true</code> if and only if all the elements of this list are
      <code>Symbol</code> objects. */
  public boolean isAllSymbols() {
    int thisSize = size();
    for (int i = 0; i < thisSize; i++)
      if (items[i].isList())
	return false;
    return true;
  }

  /**
   * Returns <code>true</code> if the specified object is not <code>null</code>
   * and is an instance of <code>SexpList</code> where the underlying lists
   * are of the same size and contain <code>Sexp</code> objects that are
   * equal.  List equality is determined via the implementation of the
   * <code>equals</code> method in {@link AbstractList}. Note that two
   * <code>SexpList</code> objects may be print-equal but not equal via
   * this method, which checks for symbol equality (a <code>StringSymbol</code>
   * and an <code>IntSymbol</code> may have the same string representation
   * but can never be the same symbol).
   *
   * @see AbstractList#equals
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SexpList))
      return false;
    SexpList otherList = (SexpList)o;
    if (this.size != otherList.size)
      return false;
    for (int i = 0; i < size; i++)
      if (this.items[i].equals(otherList.items[i]) == false)
        return false;
    return true;
  }

  /**
   * Returns the hash code value for this list.
   * @return the hash code value for this list
   */
  public int hashCode() {
    int code = 0;
    if (size < 7) {
      for (int i = 0; i < size; i++)
        code = (code << 2) ^ items[i].hashCode();
    }
    else {
      for (int i = 0; i < size; i++)
        code = 31*code + items[i].hashCode();
    }
    return code;
  }

  /**
   * Returns a string representation of this S-expression, consisting of
   * an open parenthesis, a space-separated string representation of the
   * elements of this list and a close parenthesis.
   * @return a string representation of this list
   */
  public String toString() {
    StringBuffer result = new StringBuffer(5 * size());
    toString(result);
    return result.toString();
  }

  private void toString(StringBuffer result) {
    int size = size();
    result.append('(');
    for (int i = 0; i < size; i++) {
      if (i > 0)
	result.append(' ');
      Sexp curr = items[i];
      if (curr.isSymbol())
	result.append(curr);
      else
	curr.list().toString(result);
    }
    result.append(')');
  }

  public Iterator iterator() {
    return new Iterator() {
      int currIdx = 0;
      public boolean hasNext() { return currIdx < size; }
      public Object next() {
        if (currIdx < size)
          return items[++currIdx];
        else
          throw new NoSuchElementException();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /*
  public ListIterator listIterator() {
    return list.listIterator();
  }
  */

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(size);
    out.writeInt(items.length);
    for (int i = 0; i < size; i++)
      out.writeObject(items[i]);
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    size = in.readInt();
    int arrayLength = in.readInt();
    items = new Sexp[arrayLength];
    for (int i = 0; i < size; i++)
      items[i] = (Sexp)in.readObject();
  }
}
