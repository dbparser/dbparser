package danbikel.lisp;

import java.io.*;

/**
 * <code>StringSymbol</code> objects associate strings with unique references.
 * This association is maintained in <code>Symbol</code>, not in
 * <code>StringSymbol</code> itself. As a consequence, <b>the new operator for
 * <code>StringSymbol</code> cannot be invoked directly</b>.  Rather,
 * new symbols should be created by calling {@link Symbol#add(String)}.
 *
 * @see Symbol
 * @see IntSymbol
 */

public class StringSymbol extends Symbol implements Externalizable {
  private String printName;

  /**
   * A public, no-arg constructor, required by the <code>Externalizable</code>
   * interface.
   * <p>
   * <b><code>StringSymbol</code> objects should not be created via this
   * constructor.</b>
   * <p>
   */
  public StringSymbol() {
  }

  /**
   * Creates a new StringSymbol object.  <b>Warning: the new operator cannot be
   * invoked directly.</b> Rather, new symbols are created by calling
   * {@link Symbol#add(String)}.
   *
   * @param printName the string containing the symbol's print name.
   */
  StringSymbol(String printName) {
    this.printName = printName;
  }

  /**
   * Returns the print name of this Symbol.
   *
   * @return the print name of this Symbol.
   */
  public String toString() {
    return printName;
  }

  /**
   * Returns null, since this extension of Symbol only stores
   * strings.
   */
  public Integer getInteger() { return null; }

  /**
   * Returns the key used by the internal symbol map of the class
   * <code>Symbol</code>, which, for this type of symbol, is the
   * <code>String</code> object returned by {@link #toString}.
   */
  protected Object getSymKey() { return printName; }


  // methods to comply with Externalizable interface

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(printName);
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    printName = (String)in.readObject();
  }


  /**
   * Deals with the issue of uniqueness when we are dealing with more
   * than one VM by adding the read symbol to the symbol map, if it
   * is not already there.
   */
  public Object readResolve() throws ObjectStreamException {
    return Symbol.get(printName);
  }
}
