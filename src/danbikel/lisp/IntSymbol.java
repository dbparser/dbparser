package danbikel.lisp;

import java.io.*;

/**
 * <code>IntSymbol</code> objects associate integers with unique references.
 * This association is maintained in <code>Symbol</code>, not in
 * <code>IntSymbol</code> itself.  As a consequence, <b>the <code>new</code>
 * operator for <code>IntSymbol</code> cannot be invoked directly</b>.  Rather,
 * new symbols are created by calling {@link Symbol#add(Integer)} or {@link
 * Symbol#add(int)}.
 * 
 * @see Symbol
 * @see StringSymbol
 */
public class IntSymbol extends Symbol implements Externalizable {
  private Integer symInt;

  /**
   * A public, no-arg constructor, required by the <code>Externalizable</code>
   * interface.
   * <p>
   * <b><code>IntSymbol</code> objects should not be created via this
   * constructor.</b>
   * <p>
   */
  public IntSymbol() {
  }

  /**
   * Creates an instance whose internal <code>Integer</code>
   * has the value <code>i</code>.
   */
  IntSymbol(int i) {
    symInt = new Integer(i);
  }
  /**
   * Creates an instance whose internal <code>Integer</code>
   * has the value <code>i</code>.
   */
  IntSymbol(Integer i) {
    symInt = i;
  }
  
  /**
   * Gets a string representation of this symbol.
   *
   * @return a string representation of this symbol, by calling
   * {@link Integer#toString()} on this object's internal <code>Integer</code>
   * object.
   */
  public String toString() {
    return symInt.toString();
  }
  
  /**
   * Gets the internal <code>Integer</code> object for this
   * symbol.
   *
   * @return the internal <code>Integer</code> object for this
   * symbol.
   */
  public Integer getInteger() { return symInt; }

  /**
   * Returns the key used by the internal symbol map of the class
   * <code>Symbol</code>, which, for this type of symbol, is the
   * <code>Integer</code> object returned by {@link #getInteger}.
   */
  protected Object getSymKey() { return symInt; }

  // methods to comply with Externalizable interface

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(symInt.intValue());
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    symInt = new Integer(in.readInt());
  }

  /** 
   * Deals with the issue of uniqueness when we are dealing with more
   * than one VM by adding the read symbol to the symbol map, if it
   * is not already there.
   */
  public Object readResolve() throws ObjectStreamException {
    return Symbol.get(symInt);
  }
}
