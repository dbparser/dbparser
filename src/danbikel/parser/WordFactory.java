package danbikel.parser;

import danbikel.lisp.*;

/**
 * Specifies methods for constructing {@link Word} objects.
 */
public interface WordFactory {

  /**
   * Constructs a {@link Word} object from the specified S-expression, which
   * must be a list of length 2 or greater, where the first two elements are
   * symbols.  The first symbol is the actual word, the second its
   * part-of-speech tag and a third, if present, is the word's features-vector.
   *
   * @param s the S-expression from which to construct a {@link Word} object
   * @return a new {@link Word} object
   *
   * @see WordFeatures
   */
  public Word get(Sexp s);

  /**
   * Constructs a {@link Word} object from the specified symbols.
   *
   * @param word the word itself
   * @param tag  the word's part-of-speech tag
   * @return a new {@link Word} object
   */
  public Word get(Symbol word, Symbol tag);

  /**
   * Constructs a {@link Word} object from the specified symbols.
   *
   * @param word     the word itself
   * @param tag      the word's part-of-speech tag
   * @param features the word's feature vector
   * @return a new {@link Word} object
   *
   * @see WordFeatures
   */
  public Word get(Symbol word, Symbol tag, Symbol features);
}