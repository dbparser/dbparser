package danbikel.util;

import java.util.*;

/**
 * Represents a node in a singly-linked list.
 */
public class SLNode extends Pair {
  public SLNode(Object data, Object next) {
    super(data, next);
  }

  /**
   * Returns the data associated with this node of the list.
   */
  public Object data() { return first; }

  /**
   * Returns the next node of this list.
   */
  public SLNode next() { return (SLNode)second; }

  /**
   * Sets the next node of this list.
   *
   * @param newNext the new node to be inserted after this node
   */
  private void setNext(SLNode newNext) {
    second = newNext;
  }

  /**
   * Returns the length of this list.
   */
  public int length() {
    int len = 1;
    SLNode curr = this;
    for ( ; curr.next() != null; len++)
      curr = curr.next();
    return len;
  }

  /**
   * Returns the length of this list.
   */
  public int size() { return length(); }

  /**
   * Returns a new <code>LinkedList</code> object containing all the data of
   * this list.
   */
  public LinkedList toList() {
    LinkedList list = new LinkedList();
    SLNode curr = this;
    while (curr != null) {
      list.add(curr.data());
      curr = curr.next();
    }
    return list;
  }

  /**
   * Returns an iterator to iterate over the elements of this list.
   */
  public Iterator iterator() {
    return new Iterator() {
      private SLNode curr = SLNode.this;
      private boolean nextCalled = false;
      public boolean hasNext() {
        nextCalled = true;
        return curr.next() != null;
      }
      public Object next() {
        Object currData = curr.data();
        curr = curr.next();
        return currData;
      }
      public void remove() {
        if (!nextCalled || !hasNext())
          throw new IllegalStateException();
        curr.setNext(curr.next().next());
        curr = curr.next();
      }
    };
  }
}