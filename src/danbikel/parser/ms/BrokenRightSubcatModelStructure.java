package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the right side of the head child.
 */
public class BrokenRightSubcatModelStructure extends SubcatModelStructure1 {
  public BrokenRightSubcatModelStructure() {
    super();
  }

  public double lambdaFudge(int backOffLevel) { return 5.0; }
  public double lambdaFudgeTerm(int backOffLevel) { return 0.0; }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).rightSubcat();
  }

  public ProbabilityStructure copy() {
    return new BrokenRightSubcatModelStructure();
  }
}
