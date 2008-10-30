package danbikel.util;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * A simple stack implementation created from an <code>ArrayList</code>.
 */
public class Stack extends ArrayList {
  /** Constructs an empty stack. */
  public Stack() {
    super();
  }
  /**
   * Constructs a stack with the specified collection, whose bottom
   * element is the first returned by <code>c.iterator()</code> and
   * whose top element is the last.
   */
  public Stack(Collection c) {
    super(c);
  }
  /**
   * Constructs a stack with the specified initial capacity.
   */
  public Stack(int initialCapacity) {
    super(initialCapacity);
  }

  /** Returns <code>true</code> if this stack contains no elements. */
  public boolean empty() { return size() == 0; }
  /** Pushes the specified object onto the stack. */
  public void push(Object obj) {
    add(obj);
  }
  /**
   * Pops the top element off the stack.
   *
   * @return the top element that was popped off the stack
   * @throws IndexOutOfBoundsException if this stack was empty prior
   * to calling this method
   */
  public Object pop() {
    return remove(size() - 1);
  }
}
