package danbikel.parser.constraints;

import danbikel.lisp.*;

/**
 * Factory to produce {@link PartialLexTreeConstraintSet} objects.
 */
public class PartialLexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  public PartialLexTreeConstraintSetFactory() {}

  public ConstraintSet get() {
    return new PartialLexTreeConstraintSet();
  }

  public ConstraintSet get(Object tree) {
    return new PartialLexTreeConstraintSet((Sexp)tree);
  }
}
