package danbikel.lisp;

import java.io.*;
import java.util.*;

/**
 * This class provides the abstract base type for S-epxressions, which are
 * either symbols or lists.
 *
 * @see Symbol
 * @see SexpList
 */
abstract public class Sexp implements Externalizable {
  private static final String className = Sexp.class.getName();

  // all Sexp objects are constructed via the read method
  Sexp() {}

  /**
   * Returns this object cast to a <code>Symbol</code>.
   * @exception ClassCastException if this object is not type-compatible with
   * <code>Symbol</code>
   */
  public final Symbol symbol() { return (Symbol)this; }

  /**
   * Returns this object cast to a <code>SexpList</code>.
   * @exception ClassCastException if this object is not type-compatible with
   * <code>SexpList</code>
   */
  public final SexpList list() { return (SexpList)this; }


  /**
   * Returns <code>true</code> if this is an instance of a
   * <code>SexpList</code>, <code>false</code> otherwise.
   */
  public boolean isList() { return this instanceof SexpList; }
  /**
   * Returns <code>true</code> if this is an instance of a <code>Symbol</code>,
   * <code>false</code> otherwise.
   */
  public boolean isSymbol() { return this instanceof Symbol; }

  /**
   * Returns a deep copy of this S-expression.
   */
  abstract public Sexp deepCopy();

  /**
   * Returns a canonical version of this S-expression.  If this
   * S-expression is a symbol, this object is returned;
   * otherwise, if it is a zero-element list,
   * <code>SexpList.emptyList</code> is returned; otherwise, if it is
   * a key in <code>map</code>, the map's value for this list is
   * returned; otherwise, this list is added as a reflexive key-value
   * pair in <code>map</code>, after its <code>trimToSize</code> method
   * has been invoked.  Note that this method has a superset of
   * the functionality of <code>SexpList.getCanonical(SexpList)</code>.
   *
   * @param map the reflexive map of <code>SexpList</code> objects with
   * which to canonicalize this <code>Sexp</code> object
   *
   * @see SexpList#getCanonical(SexpList)
   * @see SexpList#trimToSize
   */
  public final Sexp getCanonical(Map map) {
    if (this.isSymbol())
      return this;
    else {
      if (this.list().size() == 0)
	return SexpList.emptyList;
      else {
	Sexp mapElt = (Sexp)map.get(this);
	if (mapElt == null) {
	  this.list().trimToSize();
          // make sure if any elements are themselves lists, that they are
          // canonicalized
          int size = this.list().size();
          for (int i = 0; i < size; i++) {
            Sexp curr = this.list().get(i);
            if (curr.isList())
              this.list().set(i, curr.getCanonical(map));
          }
	  map.put(this, this);
	  return this;
	}
	else {
	  return mapElt;
	}
      }
    }
  }

  /**
   * Returns the S-expression contained in the stream held by <code>tok</code>.
   * If there are no tokens remaining in <code>tok</code>, this method returns
   * <code>null</code>.
   *
   * @exception IOException if there is an unexpected end of stream, mismatched
   * parentheses or an unexpected character
   */
  public static Sexp read(SexpTokenizer tok) throws IOException {
    while (tok.nextToken() != StreamTokenizer.TT_EOF) {
      switch (tok.ttype) {
      case StreamTokenizer.TT_WORD:
	return Symbol.add(tok.sval);
      case '(':
	SexpList list = new SexpList();
	while (tok.nextToken() != ')') {
	  if (tok.ttype == StreamTokenizer.TT_EOF)
	    throw new IOException(className + ": error: "+
				  "unexpected end of stream (line " +
				  tok.lineno() + ")");
	  tok.pushBack();
	  Sexp listElement = Sexp.read(tok);
	  list.add(listElement);
	}
	return list;
      case ')':
	throw new IOException(className + ": error: mismatched parentheses");
      default:
	throw new IOException(className + ": error: " +
			      "unexpected character: " + tok.ttype);
      }
    }
    // if the tokenizer has no more tokens, return null
    return null;
  }

  /**
   * Returns the S-expression contained in the specified string.  If the string
   * contains no tokens, this method returns <code>null</code>.
   */
  public static Sexp read(String in) throws IOException {
    return read(new SexpTokenizer(new StringReader(in)));
  }


  public abstract void readExternal(ObjectInput oi)
    throws IOException, ClassNotFoundException;

  public abstract void writeExternal(ObjectOutput oo) throws IOException;

  public static void main(String[] args) {
    try {
      //System.out.println(Sexp.read("(a"));
      Sexp s1 = Sexp.read("(foo bar)");
      Sexp s2 = Sexp.read("(bar soap)");
      //s1.add(Symbol.add("baz"));
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
