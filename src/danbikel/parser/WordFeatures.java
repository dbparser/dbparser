package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

public abstract class WordFeatures implements Serializable {
  /**
   * The unique symbol to represent unknown words.  The default value
   * is the return value of <code>Symbol.add(&quot;+unknown+&quot;)</code>;
   * if this maps to an actual word in a particular language or Treebank,
   * this data member should be reassigned in a subclass.
   */
  protected static Symbol unknownWordSym = Symbol.add("+unknown+");

  /**
   * Default constructor, to be called by subclasses (usually implicitly).
   */
  protected WordFeatures() {
  }

  /**
   * Returns a symbol representing the orthographic and/or morphological
   * features of the specified word. This default implementation simply returns
   * the unknown word symbol.
   *
   * @param word the word whose features are to be computed
   * @param firstWord whether <code>word</code> is the first word in the
   * sentence (useful when computing capitalization features for certain
   * languages, such as English)
   * @return a symbol representing the orthographic and/or morphological
   * features of <code>word</code>
   *
   * @see #unknownWordSym
   */
  public Symbol features(Symbol word, boolean firstWord) {
    return unknownWordSym;
  }

  /**
   * The symbol that represents the case where none of the features fires
   * for a particular word.
   */
  public abstract Symbol defaultFeatureVector();
}
