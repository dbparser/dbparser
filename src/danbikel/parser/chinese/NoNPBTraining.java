package danbikel.parser.chinese;

import java.util.*;
import java.io.*;
import danbikel.lisp.*;

/**
 * This class is identical to {@link danbikel.parser.chinese.Training}, except
 * that it overrides {@link
 * danbikel.parser.lang.AbstractTraining#addBaseNPs(Sexp)} to do nothing.
 */
public class NoNPBTraining extends Training {
  /**
   * The default constructor, to be invoked by {@link danbikel.parser.Language}.
   * This constructor looks for a resource named by the property
   * <code>metadataPropertyPrefix + language</code>
   * where <code>metadataPropertyPrefix</code> is the value of
   * the constant {@link #metadataPropertyPrefix} and <code>language</code>
   * is the value of <code>Settings.get(Settings.language)</code>.
   * For example, the property for English is
   * <code>&quot;parser.training.metadata.english&quot;</code>.
   */
  public NoNPBTraining() throws FileNotFoundException, IOException {
    super();
  }

  /**
   * We override this method from the default implementation so that
   * it does nothing.  This is the primary purpose of this class.
   */
  public Sexp addBaseNPs(Sexp tree) {
    return tree;
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    Training.main(args);
  }
}
