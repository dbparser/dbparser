package danbikel.parser.constraints;

import danbikel.lisp.*;
import danbikel.parser.*;

import java.util.Map;

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

  private static ConstraintSetFactory getFactory() {
    ConstraintSetFactory factory;
    String constraintSetFactStr =
      Settings.get(Settings.constraintSetFactoryClass);
    if (constraintSetFactStr != null) {
      try {
	Class constraintSetFactClass = Class.forName(constraintSetFactStr);
	if (ConstraintSetFactory.class.isAssignableFrom(constraintSetFactClass))
	  factory = (ConstraintSetFactory)constraintSetFactClass.newInstance();
	else {
	  System.err.println(className + ": error: user-specified constraint " +
			     "set factory class " +
			     constraintSetFactStr +
			     " does not specify a class assignable to " +
			     ConstraintSetFactory.class.getName());
	  throw new Exception();
	}
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + constraintSetFactStr + ":");
	e.printStackTrace();
	System.err.println("\n\tusing UnlexTreeConstraintSetFactory instead");
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
    return factory;
  }

  private static ConstraintSetFactory factory = getFactory();

  static {
    Settings.Change change = new Settings.Change() {
      public void update(Map<String, String> changedSettings) {
	if (changedSettings.containsKey(Settings.constraintSetFactoryClass)) {
	  factory = getFactory();
	}
      }
    };
    Settings.register(ConstraintSets.class, change, null);
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
