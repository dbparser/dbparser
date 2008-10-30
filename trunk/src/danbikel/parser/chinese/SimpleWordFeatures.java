package danbikel.parser.chinese;

import danbikel.lisp.*;

/**
 * This class simply uses the defaults provided by the class
 * {@link danbikel.parser.lang.AbstractWordFeatures}.
 */
public class SimpleWordFeatures
  extends danbikel.parser.lang.AbstractWordFeatures {

  /**
   * Constructs a new instance of this class.
   */
  public SimpleWordFeatures() {
    super();
  }

  /**
   * Returns {@link danbikel.parser.lang.AbstractWordFeatures#unknownWordSym}.
   *
   * @return {@link danbikel.parser.lang.AbstractWordFeatures#unknownWordSym}
   */
  public Symbol defaultFeatureVector() { return features(null, false); }
}
