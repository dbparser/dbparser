package danbikel.parser;

import danbikel.lisp.*;
import danbikel.util.*;

/**
 * A simple class for holding a pair of {@link Symbol} objects.
 */
public class SymbolPair extends Pair {
  /**
   * Constructs an empty pair of symbols (both objects are <code>null</code>).
   */
  public SymbolPair() { super(); }

  /**
   * Constructs a {@link SymbolPair} from the first two symbols in the specified
   * list.  If the specified S-expression is not of type {@link SexpList},
   * or if the specified list does not have {@link Symbol} objects as its
   * first two elements, this method will throw an {@link ClassCastException}.
   * @param sexp the list whose first two symbol elements will be made into
   * the two elements of this symbol pair
   */
  public SymbolPair(Sexp sexp) {
    super(sexp.list().symbolAt(0), sexp.list().symbolAt(1));
  }

  /**
   * Constructs a new symbol pair from the specified symbols.
   * @param first the first symbol
   * @param second the second symbol
   */
  public SymbolPair(Symbol first, Symbol second) {
    super(first, second);
  }

  /** Returns the first symbol in this pair. */
  public final Symbol first() { return (Symbol)first; }
  /** Returns the second symbol in this pair. */
  public final Symbol second() { return (Symbol)second; }

  /**
   * Returns an S-expression list string representation of this symbol pair.
   * @return an S-expression list string representation of this symbol pair.
   */
  public String toString() {
    return "(" + first + " " + second + ")";
  }
}
