package danbikel.parser.constraints;

import danbikel.lisp.*;

/**
 * Factory to produce {@link UnlexTreeConstraintSet} objects.
 */
public class UnlexTreeConstraintSetFactory implements ConstraintSetFactory {

  /**
   * Returns a new factory for {@link UnlexTreeConstraintSet} objects.
   */
  public UnlexTreeConstraintSetFactory() {}

  /**
   * Returns an empty {@link UnlexTreeConstraintSet} object.
   * @return an empty {@link UnlexTreeConstraintSet} object
   */
  public ConstraintSet get() {
    return new UnlexTreeConstraintSet();
  }

  /**
   * Returns an {@link UnlexTreeConstraintSet} constructed with the specified
   * syntactic tree.
   * @param tree the syntactic tree from which to construct a set
   * of constraints
   * @return an {@link UnlexTreeConstraintSet} constructed with the specified
   * syntactic tree
   */
  public ConstraintSet get(Object tree) {
    return new UnlexTreeConstraintSet((Sexp)tree);
  }
}
