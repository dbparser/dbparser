package danbikel.parser.arabic;

import danbikel.lisp.*;

/**
 * This class simply uses the defaults provided by the class
 * <code>danbikel.parser.WordFeatures</code>.
 */
public class SimpleWordFeatures extends danbikel.parser.WordFeatures {
  public Symbol defaultFeatureVector() { return features(null, false); }
}