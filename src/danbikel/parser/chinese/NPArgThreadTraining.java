package danbikel.parser.chinese;

import java.util.*;
import java.io.*;
import danbikel.lisp.*;

/**
 * This class is identical to {@link danbikel.parser.english.Training}, except
 * that the {@link #preProcess(Sexp)} method invokes
 * {@link danbikel.parser.lang.AbstractTraining#threadNPArgAugmentations(Sexp)}.
 */
public class NPArgThreadTraining extends Training {
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
  public NPArgThreadTraining() throws FileNotFoundException, IOException {
    super();
  }

  public Sexp preProcess(Sexp tree) {
    //transformSubjectNTs(tree);
    super.preProcess(tree);
    threadNPArgAugmentations(tree);
    return tree;
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    Training.main(args);
  }
}
