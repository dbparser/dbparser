package danbikel.parser;

import danbikel.lisp.*;
import danbikel.util.*;

public class SymbolPair extends Pair {
  public SymbolPair() { super(); }

  public SymbolPair(Sexp sexp) {
    super(sexp.list().symbolAt(0), sexp.list().symbolAt(1));
  }

  public SymbolPair(Symbol first, Symbol second) {
    super(first, second);
  }

  public final Symbol first() { return (Symbol)first; }
  public final Symbol second() { return (Symbol)second; }

  public String toString() {
    return "(" + first + " " + second + ")";
  }
}
