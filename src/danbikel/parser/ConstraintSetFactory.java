package danbikel.parser;

/**
 * Specification for a <code>ConstraintSet</code> object factory, to be used by
 * the <code>ConstraintSets</code> static factory class.
 *
 * @see ConstraintSets
 * @see Settings#constraintSetFactoryClass
 */
public interface ConstraintSetFactory {
  /**
   * Return a <code>ConstraintSet</code> object created with its default
   * constructor.
   */
  ConstraintSet get();

  /**
   * Return a <code>ConstraintSet</code> object created with its one-argument
   * constructor.
   *
   * @param input the input from which to derive parsing constraints
   */
  ConstraintSet get(Object input);
}