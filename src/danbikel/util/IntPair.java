package danbikel.util;

import java.io.*;

/**
 * A simple class to contain two integers.
 */
public class IntPair implements Externalizable {
  private int first;
  private int second;

  /**
   * Constructs a new <code>IntPair</code> object with both integers
   * equal to 0.
   */
  public IntPair() {
    first = second = 0;
  }

  /**
   * Constructs a new <code>IntPair</code> containing the specified integers.
   */
  public IntPair(int first, int second) {
    this.first = first;
    this.second = second;
  }

  /** Returns the first integer in this integer pair. */
  public int first() { return first; }
  /** Returns the second integer in this integer pair. */
  public int second() { return second; }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * an <code>IntPair</code> whose corresponding two integers are equal
   * to this <code>IntPair</code>'s two integers.
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof IntPair))
      return false;
    IntPair other = (IntPair)obj;
    return this.first == other.first && this.second == other.second;
  }

  /**
   * Returns the hash code for this integer pair, which is defined to be
   * <pre>31 * first + second</pre>
   * where <code>first</code> and <code>second</code> are the two
   * integers of this integer pair.
   */
  public int hashCode() {
    return 31 * first + second;
  }

  public String toString() { return first + "," + second; }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(first);
    out.writeInt(second);
  }
  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    first = in.readInt();
    second = in.readInt();
  }
}
