package danbikel.parser.constraints;

import danbikel.lisp.*;
import danbikel.parser.*;

/**
 * Static factory for <code>ConstraintSet</code> objects.  This scheme allows
 * the type of <code>ConstraintSet</code> object to be determined at run-time.
 * The type of constraint set factory used is deteremined by the value of the
 * property {@link Settings#constraintSetFactoryClass}.
 *
 * @see ConstraintSetFactory
 * @see Settings#constraintSetFactoryClass
 * @see ConstraintSet
 */
public class ConstraintSets {
  private static final String className = ConstraintSets.class.getName();


  private ConstraintSets() {}

  private static ConstraintSetFactory factory;
  static {
    String constraintSetFactStr =
      Settings.get(Settings.constraintSetFactoryClass);
    if (constraintSetFactStr != null) {
      try {
	Class constraintSetFactClass = Class.forName(constraintSetFactStr);
	factory = (ConstraintSetFactory)constraintSetFactClass.newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + constraintSetFactStr + ":\n\t" + e +
			   "\n\tusing UnlexTreeConstraintSetFactory instead");
	factory = new UnlexTreeConstraintSetFactory();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.constraintSetFactoryClass +
			 " was not set;\n\t" +
			 "using UnlexTreeConstraintSetFactory");
      factory = new UnlexTreeConstraintSetFactory();
    }
  }

  /**
   * Return a <code>ConstraintSet</code> object created with its default
   * constructor.
   */
  public static ConstraintSet get() {
    return factory.get();
  }
  /**
   * Return a <code>ConstraintSet</code> object created with its one-argument
   * constructor, using the specified S-expression.
   *
   * @param input the input from which to derive parsing constraints
   */
  public static ConstraintSet get(Object input) {
    return factory.get(input);
  }
}
