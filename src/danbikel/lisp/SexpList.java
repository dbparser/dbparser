package danbikel.lisp;

import java.io.Serializable;
import java.util.*;

/**
 * Stores a list of <code>Sexp</code> objects, which are either symbols
 * or themselves lists.
 */
public class SexpList extends Sexp implements Serializable {
  // constants
  /**
   * An immutable object to represent the empty list.
   */
  public static final SexpList emptyList = new SexpList(0);

  /**
   * A simple canonicalization method that returns the unique object
   * representing the empty list if the specified list contains no elements.
   */
  public static final SexpList getCanonical(SexpList list) {
    return ((list.list.size() == 0) ? emptyList : list);
  }

  // data members
  private List list;

  /**
   * Constructs a <code>SexpList</code> with the default initial capacity.
   */
  public SexpList() {
    list = new ArrayList(2);
  }

  /**
   * Constructs a <code>SexpList</code> with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity of this list
   */
  public SexpList(int initialCapacity) {
    list = new ArrayList(initialCapacity);
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
    int otherLen = initialElements.length();
    list = new ArrayList(otherLen);
    for (int i = 0; i < otherLen; i++)
      this.list.add(initialElements.list.get(i));
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
    list = new ArrayList(initialElements);
    ((ArrayList)list).trimToSize();
  }

  /**
   * Appends <code>sexp</code> to the end of this list.
   *
   * @param sexp the S-expression to append
   * @return this <code>SexpList</code> object
   */
  public SexpList add(Sexp sexp) {
    if (this == emptyList)
      return this;
    list.add(sexp);
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
    if (this == emptyList)
      return this;
    list.add(index, sexp);
    return this;
  }

  /**
   * Appends all the elements in <code>elementsToAdd</code> to this list.
   *
   * @return whether this list was modified
   */
  public boolean addAll(SexpList elementsToAdd) {
    return list.addAll(elementsToAdd.list);
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
    return list.addAll(index, elementsToAdd.list);
  }

  /**
   * Gets the <code>Sexp</code> at the specified index.
   */
  public Sexp get(int index) {
    return (Sexp)list.get(index);
  }

  /**
   * Replaces the element at <code>index</code> with <code>element</code>.
   *
   * @return the value of the element that used to be at <code>index</code>
   */
  public Sexp set(int index, Sexp element) {
    return (Sexp)list.set(index, element);
  }

  /**
   * Removes the element at <code>index</code>.
   *
   * @return the element that was removed from the list
   * @exception IndexOutOfBoundsException if the index is out of range
   * (<code>(index < 0 || index >= size())</code>)
   */
  public Sexp remove(int index) {
    return (Sexp)list.remove(index);
  }

  /**
   * Removes all elements from this list.
   */
  public void clear() {
    list.clear();
  }

  /**
   * Increases the number of elements that this list can hold, if necessary, to
   * be at least <code>minCapacity</code>.
   */
  public void ensureCapacity(int minCapacity) {
    ((ArrayList)list).ensureCapacity(minCapacity);
  }

  /**
   * Causes the capacity of this list to be its size.  An application can
   * use this method to reduce the memory footprint of <code>SexpList</code>
   * objects.
   */
  public void trimToSize() {
    ((ArrayList)list).trimToSize();
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
    return list.size();
  }
  /**
   * Returns the number of elements in this list.
   */
  public int size() {
    return list.size();
  }

  /**
   * Returns the first element of this list (identical to calling
   * <code>get(0)</code>).
   */
  public Sexp first() {
    return (Sexp)list.get(0);
  }

  /**
   * Returns the last element of this list (identical to calling
   * <code>get(size() - 1)</code>).
   */
  public Sexp last() {
    return (Sexp)list.get(list.size() - 1);
  }

  /**
   * Finds the index of the specified S-expresion.
   * @param toFind the S-expression to find in this list
   * @return the index of <code>toFind</code> in this list, or -1 if
   * it does not appear in this list
   */
  public int indexOf(Sexp toFind) {
    return list.indexOf(toFind);
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
    Collections.reverse(list);
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
   * Returns the symbol at the specified index.  Calling this method is
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
    if (this == emptyList)
      return this;
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
      if (get(i).isList())
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
    if (o == null || !(o instanceof SexpList))
      return false;
    SexpList sexpList = (SexpList)o;
    return (list.size() == sexpList.list.size() &&
	    list.equals(sexpList.list));
  }

  /**
   * Returns the hash code value for this list.
   * <p>
   * The implementation used is that provided by {@link AbstractList#hashCode}.
   * @return the hash code value for this list
   */
  public int hashCode() {
    return list.hashCode();
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
      Sexp curr = get(i);
      if (curr instanceof Symbol)
	result.append(curr);
      else
	((SexpList)curr).toString(result);
    }
    result.append(')');
  }

  public Iterator iterator() {
    return list.iterator();
  }

  public ListIterator listIterator() {
    return list.listIterator();
  }
}
