package danbikel.parser;

import danbikel.lisp.*;

/**
 * Factory to produce {@link PartialTreeConstraintSet} objects.
 */
public class PartialTreeConstraintSetFactory implements ConstraintSetFactory {

  public PartialTreeConstraintSetFactory() {}

  public ConstraintSet get() {
    return new PartialTreeConstraintSet();
  }

  public ConstraintSet get(Object tree) {
    return new PartialTreeConstraintSet((Sexp)tree);
  }
}
