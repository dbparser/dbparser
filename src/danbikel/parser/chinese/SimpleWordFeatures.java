package danbikel.parser.chinese;

import danbikel.lisp.*;

/**
 * This class simply uses the defaults provided by the class
 * <code>danbikel.parser.WordFeatures</code>.
 */
public class SimpleWordFeatures extends danbikel.parser.lang.AbstractWordFeatures {
  public Symbol defaultFeatureVector() { return features(null, false); }
}
