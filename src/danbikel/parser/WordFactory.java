package danbikel.parser;

import danbikel.lisp.*;

public interface WordFactory {

  public Word get(Sexp s);

  public Word get(Symbol word, Symbol tag);

  public Word get(Symbol word, Symbol tag, Symbol features);
}