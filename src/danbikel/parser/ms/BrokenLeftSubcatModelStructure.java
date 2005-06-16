package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Provides the complete back-off structure of the subcat-generation
 * model for the left side of the head child.  This model structure is
 * just like {@link LeftSubcatModelStructure1} but is &ldquo;broken&rdquo;
 * in that its {@link #lambdaFudge(int)} method returns <tt>5.0</tt> for
 * all back-off levels and its {@link #lambdaFudgeTerm(int)} method returns
 * <tt>0.0</tt> for all back-off levels, just as Collins had implemented
 * for his thesis parser.
 * <p/>
 * For the actual details of this model's back-off structure, please see
 * {@link SubcatModelStructure1}.
 */
public class BrokenLeftSubcatModelStructure extends SubcatModelStructure1 {
  /**
   * Constructs a new {@link BrokenLeftSubcatModelStructure} instance.
   */
  public BrokenLeftSubcatModelStructure() {
    super();
  }

  /** Returns <tt>5.0</tt> regardless of the value of the argument. */
  public double lambdaFudge(int backOffLevel) { return 5.0; }
  /** Returns <tt>0.0</tt> regardless of the value of the argument. */
  public double lambdaFudgeTerm(int backOffLevel) { return 0.0; }

  /**
   * Gets the future being predicted conditioning on this subcat event.
   * @param trainerEvent the maximal-context event from which to get
   * the future being predicted
   * @param backOffLevel the back-off level whose estimate is being
   * sought
   * @return the future being predicted conditioning on the left subcat
   * event contained in the speciified {@link TrainerEvent} instance
   */
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).leftSubcat();
  }

  /** Returns a copy of this object. */
  public ProbabilityStructure copy() {
    return new BrokenLeftSubcatModelStructure();
  }
}
