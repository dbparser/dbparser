package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the left side of the head child.
 */
public class BrokenLeftSubcatModelStructure extends SubcatModelStructure1 {
  public BrokenLeftSubcatModelStructure() {
    super();
  }

  public double lambdaFudge(int backOffLevel) { return 5.0; }
  public double lambdaFudgeTerm(int backOffLevel) { return 0.0; }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).leftSubcat();
  }

  public ProbabilityStructure copy() {
    return new BrokenLeftSubcatModelStructure();
  }
}
