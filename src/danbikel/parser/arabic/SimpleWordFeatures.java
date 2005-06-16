package danbikel.parser.arabic;

import danbikel.lisp.*;

/**
 * This class simply uses the defaults provided by the class
 * {@link danbikel.parser.lang.AbstractWordFeatures}.
 */
public class SimpleWordFeatures
  extends danbikel.parser.lang.AbstractWordFeatures {

  /**
   * Constructs a new {@link danbikel.parser.WordFeatures} instance for
   * Arabic that simply returns the default word-feature vector.
   */
  public SimpleWordFeatures() {
    super();
  }

  public Symbol defaultFeatureVector() { return features(null, false); }
}