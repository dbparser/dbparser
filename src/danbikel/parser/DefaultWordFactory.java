package danbikel.parser;

import danbikel.lisp.Symbol;
import danbikel.lisp.Sexp;

public class DefaultWordFactory implements WordFactory {

  /**
   * Creates a word factory for constructing {@link Word} objects.
   */
  public DefaultWordFactory() {}

  public Word get(Sexp s) {
    return new Word(s);
  }

  public Word get(Symbol word, Symbol tag) {
    return new Word(word, tag);
  }

  public Word get(Symbol word, Symbol tag, Symbol features) {
    return new Word(word, tag, features);
  }
}