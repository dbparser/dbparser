package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the subcat-generation
 * model for the right side of the head child.
 */
public class RightSubcatModelStructure1 extends SubcatModelStructure1 {
  public RightSubcatModelStructure1() {
    super();
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).rightSubcat();
  }

  public ProbabilityStructure copy() {
    return new RightSubcatModelStructure1();
  }
}
