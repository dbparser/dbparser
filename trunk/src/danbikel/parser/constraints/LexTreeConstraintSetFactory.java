package danbikel.parser.constraints;

import danbikel.lisp.*;

/**
 * Factory to produce {@link LexTreeConstraintSet} objects.
 */
public class LexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  /**
   * Constructs a factory for {@link LexTreeConstraintSet} instances.
   */
  public LexTreeConstraintSetFactory() {}

  /**
   * Gets a new, empty {@link LexTreeConstraintSet} instance.
   * @return a new, empty {@link LexTreeConstraintSet} instance.
   */
  public ConstraintSet get() {
    return new LexTreeConstraintSet();
  }

  /**
   * Gets a new set of constraints for parsing the specified lexicalized tree.
   *
   * @param tree the lexicalized tree for which to get constraints;
   *             lexicalization performed by the current {@linkplain
   *             danbikel.parser.Language#headFinder() head finder}.
   * @return a new set of constraints for a lexicalized version of the specified
   *         parse tree.
   */
  public ConstraintSet get(Object tree) {
    return new LexTreeConstraintSet((Sexp)tree);
  }
}
