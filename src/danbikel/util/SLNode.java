package danbikel.util;

import java.util.*;

/**
 * Represents a node in a singly-linked list.
 */
public class SLNode {
  private Object data;
  private SLNode next;

  public SLNode(Object data, SLNode next) {
    this.data = data;
    this.next = next;
  }

  /**
   * Returns the data associated with this node of the list.
   */
  public Object data() { return data; }

  /**
   * Returns the next node of this list.
   */
  public SLNode next() { return next; }

  /**
   * Sets the next node of this list.
   *
   * @param newNext the new node to be inserted after this node
   */
  private void setNext(SLNode next) {
    this.next = next;
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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    SLNode curr = this;
    while (curr != null) {
      sb.append(curr.data);
      curr = curr.next;
    }
    sb.append(")");
    return sb.toString();
  }

  public int hashCode() {
    int code = 0;
    SLNode curr = this;
    while (curr != null) {
      code = (code * 31) + (curr.data == null ? 0 : curr.data.hashCode());
      curr = curr.next;
    }
    return code;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof SLNode))
      return false;
    SLNode other = (SLNode)obj;
    SLNode curr = this;
    while (curr != null && other != null) {
      if ((curr.data == null && other.data != null) ||
          (curr.data != null && other.data == null))
        return false;
      if (curr.data != null && curr.data.equals(other.data) == false)
        return false;
      curr = curr.next;
      other = other.next;
    }
    return curr == null && other == null;
  }
}