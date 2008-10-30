package danbikel.parser.constraints;

import danbikel.lisp.*;

/**
 * Factory to produce {@link PartialLexTreeConstraintSet} objects.
 */
public class PartialLexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  /**
   * Constructs a new factory for {@link PartialLexTreeConstraintSet} objects.
   */
  public PartialLexTreeConstraintSetFactory() {}

  /**
   * Returns a new, empty {@link PartialLexTreeConstraintSet} object.
   * @return a new, empty {@link PartialLexTreeConstraintSet} object.
   *
   * @see PartialLexTreeConstraintSet#PartialLexTreeConstraintSet()
   */
  public ConstraintSet get() {
    return new PartialLexTreeConstraintSet();
  }

  /**
   * Returns a new {@link PartialLexTreeConstraintSet} using the specified
   * syntactic tree.
   * @param tree the syntactic tree from which to construct a tree structure
   * of {@link PartialLexTreeConstraint} objects contained in a
   * {@link PartialLexTreeConstraintSet}
   * @return a new {@link PartialLexTreeConstraintSet} using the specified
   * syntactic tree.
   *
   * @see PartialLexTreeConstraintSet#PartialLexTreeConstraintSet(Sexp) 
   */
  public ConstraintSet get(Object tree) {
    return new PartialLexTreeConstraintSet((Sexp)tree);
  }
}
