package danbikel.parser;

import danbikel.lisp.*;

/**
 * Factory to produce {@link LexTreeConstraintSet} objects.
 */
public class LexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  public LexTreeConstraintSetFactory() {}

  public ConstraintSet get() {
    return new LexTreeConstraintSet();
  }

  public ConstraintSet get(Object tree) {
    return new LexTreeConstraintSet((Sexp)tree);
  }
}
