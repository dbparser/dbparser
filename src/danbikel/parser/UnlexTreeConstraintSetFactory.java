package danbikel.parser;

import danbikel.lisp.*;

/**
 * Factory to produce {@link UnlexTreeConstraintSet} objects.
 */
public class UnlexTreeConstraintSetFactory implements ConstraintSetFactory {

  public UnlexTreeConstraintSetFactory() {}

  public ConstraintSet get() {
    return new UnlexTreeConstraintSet();
  }

  public ConstraintSet get(Object tree) {
    return new UnlexTreeConstraintSet((Sexp)tree);
  }
}
