package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the right side of the head child.
 */
public class RightSubcatModelStructure2 extends SubcatModelStructure2 {
  public RightSubcatModelStructure2() {
    super();
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).rightSubcat();
  }

  public ProbabilityStructure copy() {
    return new RightSubcatModelStructure2();
  }
}
