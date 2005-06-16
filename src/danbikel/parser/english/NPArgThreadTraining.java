package danbikel.parser.english;

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

  /**
   * Identical to {@link danbikel.parser.english.Training#preProcess}, except
   * that {@link #threadNPArgAugmentations(danbikel.lisp.Sexp)} is invoked
   * after all other preprocessing methods.
   * @param tree the tree to be pre-processed
   * @return the specified tree, modified by pre-processing methods 
   */
  public Sexp preProcess(Sexp tree) {
    super.preProcess(tree);
    threadNPArgAugmentations(tree);
    return tree;
  }

  /** Test driver for this class. */
  public static void main(String[] args) {
    Training.main(args);
  }
}
