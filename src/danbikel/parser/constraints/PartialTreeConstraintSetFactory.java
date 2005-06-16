package danbikel.parser.constraints;

import danbikel.lisp.*;

/**
 * Factory to produce {@link PartialTreeConstraintSet} objects.
 */
public class PartialTreeConstraintSetFactory implements ConstraintSetFactory {

  /**
   * Returns a new factory for {@link PartialTreeConstraintSet} objects.
   */
  public PartialTreeConstraintSetFactory() {}

  /**
   * Returns an empty partial tree constraint set.
   * @return an empty partial tree constraint set.
   */
  public ConstraintSet get() {
    return new PartialTreeConstraintSet();
  }

  /**
   * Returns a partial tree constraint set for the specified tree, which must
   * be a {@link Sexp} instance.
   * @param tree a {@link Sexp} representing a tree from which to construct
   * a set of partial tree constraints
   * @return a partial tree constraint set constructed from the specified
   * syntactic tree
   */
  public ConstraintSet get(Object tree) {
    return new PartialTreeConstraintSet((Sexp)tree);
  }
}
